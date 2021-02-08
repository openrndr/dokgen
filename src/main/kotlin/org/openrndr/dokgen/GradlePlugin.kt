package org.openrndr.dokgen


import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import java.io.Serializable
import javax.inject.Inject

const val PLUGIN_NAME = "dokgen"

class DokGenException(message: String) : Exception("$PLUGIN_NAME exception: $message")

fun generatedExamplesDirectory(project: Project): File {
    return File(project.buildDir, "dokgen/generated/examples")
}

fun generatedExamplesForExportDirectory(project: Project): File {
    return File(project.buildDir, "dokgen/generated/examples-export")
}

// this class provides the configuration dsl
// the inner classes represent configurable data
// the methods represent closures in the dsl through which configuration can be set
open class DokGenPluginExtension @Inject constructor(objectFactory: ObjectFactory):Serializable {
    open class ExamplesConf: Serializable {
        var webRootUrl: String? = null
    }

    open class RunnerConf: Serializable {
        var jvmArgs = mutableListOf<String>()
    }

    open class DocsifyConf: Serializable {
        var assets: List<File>? = null
        var media: List<File>? = null
    }

    var examplesConf: ExamplesConf? = objectFactory.newInstance(ExamplesConf::class.java)
    var runnerConf: RunnerConf? = objectFactory.newInstance(RunnerConf::class.java)
    var docsifyConf: DocsifyConf? = objectFactory.newInstance(DocsifyConf::class.java)


    fun runner(action: Action<RunnerConf>) {
        runnerConf?.let {
            action.execute(it)
        }
    }

    fun examples(action: Action<ExamplesConf>) {
        examplesConf?.let {
            action.execute(it)
        }
    }

    fun docsify(action: Action<DocsifyConf>) {
        docsifyConf?.let {
            action.execute(it)
        }
    }
}

abstract class ProcessSourcesTask @Inject constructor(
    @Input
    val examplesConf: DokGenPluginExtension.ExamplesConf?) : DefaultTask() {
    init {
        group = PLUGIN_NAME
        description = "processes into markdown and examples"
    }

    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    val inputDir: DirectoryProperty = project.objects.directoryProperty().also {
        it.set(File(project.projectDir, "src/main/kotlin/docs"))
    }

    @OutputDirectory
    var mdOutputDir: File = File(project.buildDir, "$PLUGIN_NAME/md")

    @OutputDirectory
    var examplesOutputDir: File = generatedExamplesDirectory(project)

    @OutputDirectory
    var examplesForExportOutputDir: File = generatedExamplesForExportDirectory(project)


    @TaskAction
    fun run(inputChanges: InputChanges) {
        println(
                if (inputChanges.isIncremental) "Executing incrementally"
                else "Executing non-incrementally"
        )
        val toProcess = mutableListOf<File>()
        for (change in inputChanges.getFileChanges(inputDir)) {
            println("${change.changeType}: ${change.normalizedPath}")
            toProcess.add(change.file)
        }

        DokGen.processSources(
            toProcess,
            inputDir.asFile.get(),
            mdOutputDir,
            examplesOutputDir,
            examplesForExportOutputDir,
            webRootUrl = examplesConf?.webRootUrl
        )
    }
}

open class RunExamplesTask @Inject constructor(
    private val runnerConf: DokGenPluginExtension.RunnerConf?
) : DefaultTask() {
    init {
        group = PLUGIN_NAME
        description = "run the exported examples programs"
    }


    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    val examplesDirectory: DirectoryProperty = project.objects.directoryProperty().also {
        it.set(generatedExamplesDirectory(project))
    }

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val toRun = mutableListOf<File>()

        for (change in inputChanges.getFileChanges(examplesDirectory)) {
            println("find changes: $change")
            toRun.add(change.file)
        }


        val sourceSetContainer = project.property("sourceSets") as SourceSetContainer
        val ss = sourceSetContainer.getByName("GeneratedExamples")
        val execClasses = DokGen.getExamplesClassNames(toRun, examplesDirectory.get().asFile)

        execClasses.forEach { klass ->
            try {
                project.javaexec { spec ->
                    println("rcp: ${ss.runtimeClasspath.toList().joinToString(", ")}")
                    println("ccp: ${ss.compileClasspath.toList().joinToString(", ")}")
                    spec.classpath = ss.runtimeClasspath
                    runnerConf?.let {
                        spec.jvmArgs = it.jvmArgs
                    }
                    spec.main = klass
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

open class DocsifyTask @Inject constructor(
    private val docsifyConf: DokGenPluginExtension.DocsifyConf?
) : DefaultTask() {
    init {
        group = "dokgen"
        description = "docsify"
    }

    private val docsifySources = javaClass.classLoader.getResource("docsify")
    val dokgenBuildDir = File(project.buildDir, PLUGIN_NAME)

    @OutputDirectory
    val dokgenMdDir = File(dokgenBuildDir, "md")

    @OutputDirectory
    val docsifyBuildDir = File(project.buildDir, "$PLUGIN_NAME/docsify")

    @OutputDirectory
    var docsifyDocsDir: File = File(docsifyBuildDir, "docs")

    @OutputDirectory
    var mediaOutputDirectory: File = File(docsifyDocsDir, "media")
    @OutputDirectory
    var assetsOutputDirectory: File = File(docsifyDocsDir, "assets")

    @TaskAction
    fun run() {
        val jar = project.zipTree(docsifySources.path.split("!")[0])
        project.copy { spec ->
            spec.from(jar)
            spec.into(dokgenBuildDir)
            spec.include("docsify/**/*")
            spec.exclude("docsify/node_modules/")
        }

        docsifyConf?.let {
            it.media?.let { media ->
                project.copy { spec ->
                    spec.from(media)
                    spec.into(mediaOutputDirectory)
                }
            }
            it.assets?.let { assets ->
                project.copy { spec ->
                    spec.exclude("CNAME", "index.html")
                    spec.from(assets)
                    spec.into(assetsOutputDirectory)
                }

                val files = assets.flatMap {
                    if (it.isDirectory) {
                        it.listFiles().toList()
                    } else {
                        listOf(it)
                    }
                }

                listOf("CNAME", "index.html").map { name ->
                    files.find { it.name.contains(name) }
                }.filterNotNull().forEach { file ->
                    project.copy { spec ->
                        spec.from(file)
                        spec.into(docsifyDocsDir)
                    }
                }

            }

        }

        project.copy { spec ->
            spec.from(dokgenMdDir)
            spec.into(docsifyDocsDir)
        }
    }

}

open class ServeDocsTask @Inject constructor() : DefaultTask() {

    @InputDirectory
    val docsifyBuildDir = File(project.buildDir, "$PLUGIN_NAME/docsify")

    init {
        group = "dokgen"
        description = "runs the serves docs"
    }

    @TaskAction
    fun run() {
        project.exec { exec ->
            exec.workingDir = docsifyBuildDir
            exec.commandLine("docker-compose")
            exec.args = listOf("up")
        }
    }
}

class GradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val conf = project.extensions.create(PLUGIN_NAME,
            DokGenPluginExtension::class.java,
            project.objects
        )

        project.afterEvaluate { _ ->
            val sourceSets = project.property("sourceSets") as SourceSetContainer
            val mss = sourceSets.getByName("main")

            val gess = sourceSets.create("GeneratedExamples")
            gess.compileClasspath += mss.compileClasspath
            gess.runtimeClasspath += mss.runtimeClasspath

            gess.java.srcDir(generatedExamplesDirectory(project))


            val dokGenTask = project.tasks.create(PLUGIN_NAME)
            dokGenTask.group = PLUGIN_NAME
            dokGenTask.description = "do the work"

            val compileKotlinTask = project.tasks.getByPath("compileGeneratedExamplesKotlin")
            val processSources = project.tasks.create("processSources", ProcessSourcesTask::class.java, conf.examplesConf)
            val runExamples = project.tasks.create("runExamples", RunExamplesTask::class.java, conf.runnerConf)

            println("compileKotlinTask:")
            println(compileKotlinTask)

            runExamples.dependsOn(processSources)
            runExamples.dependsOn(compileKotlinTask)
            runExamples.outputs.upToDateWhen {
                true
            }

            dokGenTask.dependsOn(runExamples)

            val docsifyTask = project.tasks.create("docsify", DocsifyTask::class.java, conf.docsifyConf)

            dokGenTask.finalizedBy(docsifyTask)
            docsifyTask.dependsOn(dokGenTask)

            val serveDocsTask = project.tasks.create("serveDocs", ServeDocsTask::class.java)
            serveDocsTask.dependsOn(docsifyTask)
        }
    }
}
