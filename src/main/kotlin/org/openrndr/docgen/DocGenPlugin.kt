package org.openrndr.docgen

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import javax.inject.Inject

open class MarkdownConf {
    var media: String? = null
    var output: String? = null
}

open class ExamplesConf {
    var packageDirective: String? = null
    var output: String? = null
}

class DocGenException(message: String) : Exception("docgen exception: $message")


open class DocGenPluginExtension @Inject constructor(objectFactory: ObjectFactory) {
    var sources: String? = null
    var markdownConf: MarkdownConf? = objectFactory.newInstance(MarkdownConf::class.java)
    var examplesConf: ExamplesConf? = objectFactory.newInstance(ExamplesConf::class.java)

    fun markdown(action: Action<MarkdownConf>) {
        markdownConf?.let {
            action.execute(it)
        } ?: throw DocGenException("no markdown configuration was defined")
    }

    fun examples(action: Action<ExamplesConf>) {
        examplesConf?.let {
            action.execute(it)
        } ?: throw DocGenException("no examples configuration was defined")
    }
}

open class ProcessSourcesTask @Inject constructor() : DefaultTask() {

    init {
        group = "docgen"
        description = "processes into markdown and examples"
    }

    @InputDirectory
    var sourcesDir: File = File(project.projectDir, "src/main/kotlin/docs")

    @OutputDirectory
    var mdOutputDir: File = File(project.buildDir, "docgen/md")

    @OutputDirectory
    var examplesOutputDir: File = File(project.projectDir, "src/main/kotlin/examples")


    @TaskAction
    fun run(inputs: IncrementalTaskInputs) {
        val toProcess = mutableListOf<File>()
        inputs.outOfDate {
            toProcess.add(it.file)
        }
        DocGen.processSources(
            toProcess,
            sourcesDir,
            mdOutputDir,
            examplesOutputDir
        )
    }
}

open class RunExamplesTask @Inject constructor() : DefaultTask() {
    init {
        group = "docgen"
        description = "run the exported examples programs"
    }

    @InputDirectory
    var examplesDirectory: File = File(project.projectDir, "src/main/kotlin/examples")

    @TaskAction
    fun run(inputs: IncrementalTaskInputs) {
        val toRun = mutableListOf<File>()
        inputs.outOfDate {
            toRun.add(it.file)
        }


        val sourceSetContainer = project.property("sourceSets") as SourceSetContainer
        val ss = sourceSetContainer.getByName("main")

        val execClasses = DocGen.getExampleClasses(toRun, File(project.projectDir, "src/main/kotlin/examples"))

        execClasses.forEach { klass ->
            try {
                project.javaexec { spec ->
                    spec.classpath = ss.runtimeClasspath
                    spec.jvmArgs = listOf("-XstartOnFirstThread")
                    spec.main = klass
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
}

open class DocsifyTask @Inject constructor() : DefaultTask() {
    init {
        group = "docgen"
        description = "docsify"
    }

    val docsifySources = javaClass.classLoader.getResource("docsify")
    val docgenBuildDir = File(project.buildDir, "docgen")
    val docgenMdDir = File(docgenBuildDir, "md")
    val docsifyBuildDir = File(project.buildDir, "docgen/docsify")
    var docsifyDocsDir: File = File(docsifyBuildDir, "docs")
    var mediaInputDirectory: File = File(project.projectDir, "media")
    var mediaOutputDirectory: File = File(docsifyDocsDir, "media")


    @TaskAction
    fun run() {
        val jar = project.zipTree(docsifySources.path.split("!")[0])
        project.copy { spec ->
            spec.from(jar)
            spec.into(docgenBuildDir)
            spec.include("docsify/**/*")
        }

        project.copy { spec ->
            spec.from(mediaInputDirectory)
            spec.into(mediaOutputDirectory)
        }

        project.copy { spec ->
            spec.from(docgenMdDir)
            spec.into(docsifyDocsDir)
        }
    }

}


class DocGenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val conf = project.extensions.create("docgen",
            DocGenPluginExtension::class.java,
            project.objects
        )

        project.afterEvaluate { loaded ->
            val docGen = project.tasks.create("docgen")
            docGen.group = "docgen"
            docGen.description = "do the work"

            val processSources = project.tasks.create("processSources", ProcessSourcesTask::class.java)

            val runExamples = project.tasks.create("runExamples", RunExamplesTask::class.java)
            runExamples.dependsOn(processSources.path)
            runExamples.dependsOn(project.tasks.getByPath("compileKotlin"))

            docGen.dependsOn(processSources.path)
            docGen.dependsOn(runExamples.path)

            val docsifyTask = project.tasks.create("docsify", DocsifyTask::class.java)
            docGen.finalizedBy(docsifyTask)

            docsifyTask.dependsOn(docGen)
        }
    }
}
