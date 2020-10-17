package ie.trinitcore.trinreflection.gradleplugin.task

import groovy.cli.Option
import ie.trinitcore.trinreflection.gradleplugin.TReflectionUtilities
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

class BuildReflectionDataTask : DefaultTask() {

    @Input
    var srcKotlinDirPaths: List<String> = emptyList()

    @TaskAction
    fun build() {
        val path = this.path

        srcKotlinDirPaths.forEach { srcKotlinDirPath ->
            val srcKotlinDir = "$path/$srcKotlinDirPath"
            val util = TReflectionUtilities(srcKotlinDir)
            util.useForPreCompilation()
        }
    }

}