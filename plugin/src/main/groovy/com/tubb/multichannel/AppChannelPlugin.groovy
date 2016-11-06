package com.tubb.multichannel

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

public class AppChannelPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        project.extensions.create('appchannel', AppChannelExtension)
        project.extensions.create('taskmanage', TaskManageExtension)
        project.ext {
            buildChannel = this.&buildChannel
        }
        project.afterEvaluate { // tasks is already fine
            taskManage(project)
            customOutput(project)
        }
    }

    private void taskManage(Project project) {
        def taskmanage = project['taskmanage']
        if (taskmanage == null) return
        boolean disableDebugTask = taskmanage.disableDebugTask
        boolean disableLintTask = taskmanage.disableLintTask
        boolean disableTestTask = taskmanage.disableTestTask
        def targetTasks = project.tasks.findAll{ task ->
            def taskName = task.name.toLowerCase()
            if (disableDebugTask && taskName.contains('debug'))
                return true
            if (disableLintTask && taskName.contains('lint'))
                return true
            if (disableTestTask && taskName.contains("test"))
                return true
            return false
        }
        targetTasks.each{
            println "disable task ${it.name} by AppChannelPlugin"
            it.setEnabled false
        }
    }

    private void customOutput(Project project) {
        def appchannel = project['appchannel']
        if (appchannel == null) return
        String outputDir = appchannel.outputDir
        def buildOutputFileName = appchannel.buildOutputFileName
        def variants
        if (project.plugins.hasPlugin('com.android.application')) {
            variants = project.android.applicationVariants
        } else if (project.plugins.hasPlugin('com.android.library')) {
            variants = project.android.libraryVariants
        } else {
            throw new GradleException('Android Application or Library plugin required')
        }
        variants.all { variant ->
            if (project.plugins.hasPlugin('com.android.library')
                    && (variant.flavorName == null || ''.equals(variant.flavorName))) {
                // filter library sync
                return
            }
            variant.outputs.each { output ->
                def sourceFile = output.outputFile
                String targetDir = outputDir != null ? outputDir : sourceFile.parent
                String targetFileName = sourceFile.name
                if (buildOutputFileName != null) {
                    targetFileName = buildOutputFileName(project, variant)
                }
                output.outputFile = new File(targetDir, targetFileName)
            }
        }
        def zipalignTasks = project.tasks.findAll { task ->
            task.name.contains('zipalign')
        }
        zipalignTasks.each { task ->
            String targetDir = outputDir != null ? outputDir : ''
            task.outputFile = new File("${project.rootDir}${File.separator}${targetDir}", task.outputFile.name)
        }
    }

    public void buildChannel(Project project) {
        def appchannel = project['appchannel']
        if (appchannel == null) {
            throw new GradleException('buildChannel(Project project) method must call in appchannel dsl')
        }
        boolean hasChannelProperty = project.hasProperty('channel')
        if (hasChannelProperty) {
            def channelFilePath = appchannel.channelFilePath
            def buildProductFlavor = appchannel.buildProductFlavor
            File channelFile = new File(channelFilePath)
            if (channelFile.isDirectory() || !channelFile.exists()) {
                throw new IllegalArgumentException("channelFilePath [${channelFilePath}] is not a valid file path")
            }
            if (buildProductFlavor == null) {
                throw new IllegalArgumentException('customProductFlavor cant not be null')
            }
            channelFile.eachLine { name ->
                if (!name.startsWith("#")) {
                    project.android.productFlavors.create(name, buildProductFlavor(name))
                }
            }
        }
    }
}