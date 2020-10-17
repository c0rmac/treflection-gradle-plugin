package ie.trinitcore.trinreflection.gradleplugin

import ie.trinitcore.trinreflection.gradleplugin.task.BuildReflectionDataTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class TReflectionGradlePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.create("buildReflectionData", BuildReflectionDataTask::class.java)
    }

}