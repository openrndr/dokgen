package org.openrndr.dokgen

import org.codehaus.groovy.runtime.GStringImpl
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import javax.inject.Inject
import org.gradle.api.tasks.Copy
import org.gradle.internal.impldep.bsh.commands.dir

const val PLUGIN_NAME = "dokgen"

class DokGenException(message: String) : Exception("$PLUGIN_NAME exception: $message")


fun generatedExamplesDirectory(project: Project): File {
    return File(project.projectDir, "src/generated/examples")
}


// this class provides the configuration dsl
// the inner classes represent configurable data
// the methods represent closures in the dsl through which configuration can be set
open class DokGenPluginExtension @Inject constructor(objectFactory: ObjectFactory) {
    open class ExamplesConf {
        var webRootUrl: String? = null
    }

    open class RunnerConf {
        var jvmArgs = mutableListOf<String>()
    }

    open class DocsifyConf {
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

open class ProcessSourcesTask @Inject constructor(val examplesConf: DokGenPluginExtension.ExamplesConf?) : DefaultTask() {

    init {
        group = PLUGIN_NAME
        description = "processes into markdown and examples"
    }

    @InputDirectory
    var sourcesDir: File = File(project.projectDir, "src/main/kotlin/docs")

    @OutputDirectory
    var mdOutputDir: File = File(project.buildDir, "$PLUGIN_NAME/md")

    @OutputDirectory
    var examplesOutputDir: File = generatedExamplesDirectory(project)


    @TaskAction
    fun run(inputs: IncrementalTaskInputs) {
        val toProcess = mutableListOf<File>()
        inputs.outOfDate {
            toProcess.add(it.file)
        }
        DokGen.processSources(
            toProcess,
            sourcesDir,
            mdOutputDir,
            examplesOutputDir,
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

    @InputDirectory
    var examplesDirectory: File = generatedExamplesDirectory(project)

    @TaskAction
    fun run(inputs: IncrementalTaskInputs) {
        val toRun = mutableListOf<File>()
        inputs.outOfDate {
            toRun.add(it.file)
        }


        val sourceSetContainer = project.property("sourceSets") as SourceSetContainer
        val ss = sourceSetContainer.getByName("main")

        val execClasses = DokGen.getExamplesClassNames(toRun, examplesDirectory)

        execClasses.forEach { klass ->
            try {
                project.javaexec { spec ->
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

    val docsifySources = javaClass.classLoader.getResource("docsify")
    val dokgenBuildDir = File(project.buildDir, PLUGIN_NAME)
    val dokgenMdDir = File(dokgenBuildDir, "md")
    val docsifyBuildDir = File(project.buildDir, "$PLUGIN_NAME/docsify")
    var docsifyDocsDir: File = File(docsifyBuildDir, "docs")
    var mediaOutputDirectory: File = File(docsifyDocsDir, "media")
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
                    spec.from(assets)
                    spec.into(assetsOutputDirectory)
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
            val ms = sourceSets.getByName("main")
            generatedExamplesDirectory(project).let {
                ms.java.srcDir(it)
            }

            val dokGenTask = project.tasks.create(PLUGIN_NAME)
            dokGenTask.group = PLUGIN_NAME
            dokGenTask.description = "do the work"

            val compileKotlinTask = project.tasks.getByPath("compileKotlin")
            val processSources = project.tasks.create("processSources", ProcessSourcesTask::class.java, conf.examplesConf)
            val runExamples = project.tasks.create("runExamples", RunExamplesTask::class.java, conf.runnerConf)

            runExamples.dependsOn(processSources)
            runExamples.dependsOn(compileKotlinTask)

            dokGenTask.dependsOn(runExamples)

            val docsifyTask = project.tasks.create("docsify", DocsifyTask::class.java, conf.docsifyConf)

            dokGenTask.finalizedBy(docsifyTask)
            docsifyTask.dependsOn(dokGenTask)


            val serveDocsTask = project.tasks.create("serveDocs", ServeDocsTask::class.java)
            serveDocsTask.dependsOn(docsifyTask)
        }
    }
}
