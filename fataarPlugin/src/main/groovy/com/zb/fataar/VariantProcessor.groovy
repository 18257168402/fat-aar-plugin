package com.zb.fataar

import org.gradle.api.Project
import org.gradle.api.Task

import org.gradle.jvm.tasks.Jar

/**
 * Created by Vigi on 2017/2/24.
 * Modified by lishusheng on 2019.03.21
 */
class VariantProcessor {

    private final Project mProject

    private final def  mVariant

    private Collection<AndroidArchiveLibrary> mAndroidArchiveLibraries = new ArrayList<>()

    private Collection<File> mJarFiles = new ArrayList<>()

    public VariantProcessor(Project project,def variant) {
        mProject = project
        mVariant = variant
    }

    public void addAndroidArchiveLibrary(AndroidArchiveLibrary library) {
        mAndroidArchiveLibraries.add(library)
    }

    public void addJarFile(File jar) {
        mJarFiles.add(jar)
    }

    public void processVariant() {


        processClassesAndJars()

        if (mAndroidArchiveLibraries.isEmpty()) {
            return
        }
        processManifest()
        processResourcesAndR()
        processRSources()
        processAssets()
        processJniLibs()

        //        String taskPath = 'prepare' + mVariant.name.capitalize() + 'Dependencies'
//        Task prepareTask = mProject.tasks.findByPath(taskPath)
//        if (prepareTask == null) {
//            throw new RuntimeException("Can not find task ${taskPath}!")
//        }
//        processProguardTxt(prepareTask)
    }


    private Closure copyAARClosure(){
        return {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                ExplodedHelper.copyAARIfNeed(archiveLibrary,mProject)
            }
        }
    }

    /**
     * merge manifest
     *
     * TODO process each variant.getOutputs()
     * TODO "InvokeManifestMerger" deserve more android plugin version check
     * TODO add setMergeReportFile
     * TODO a better temp manifest file location
     */
    private void processManifest() {//合并manifest的task
        Class invokeManifestTaskClazz = null
        String className = 'com.android.build.gradle.tasks.InvokeManifestMerger'
        try {
            invokeManifestTaskClazz = Class.forName(className)
        } catch (ClassNotFoundException ignored) {}
        if (invokeManifestTaskClazz == null) {
            throw new RuntimeException("Can not find class ${className}!")
        }
        def variantOutputs = mVariant.getOutputs();
        //println(">>>>>>>>>>>variantOutputs:"+variantOutputs)
        def variantOutput =variantOutputs.toArray()[0];
        def processManifestTask = variantOutput.getProcessManifest();
        def manifestOutputDir = mProject.file(mProject.buildDir.path + '/intermediates/fat-aar/' + mVariant.dirName)
        def manifestOutputFile = mProject.file(mProject.buildDir.path + '/intermediates/fat-aar/' + mVariant.dirName + '/AndroidManifest.xml')

        if(!manifestOutputDir.exists()){
            manifestOutputDir.mkdirs();
        }
        //println(">>>>>>>>>>>processManifestTask:"+processManifestTask.getName())
        File manifestOutputBackupDir =processManifestTask.getManifestOutputDirectory();
        File manifestOutputBackup = new File(manifestOutputBackupDir.absolutePath+File.separator+"AndroidManifest.xml");

        Task manifestsMergeTask = mProject.tasks.create('merge' + mVariant.name.capitalize() + 'Manifest', invokeManifestTaskClazz)
        manifestsMergeTask.setVariantName(mVariant.name)
        manifestsMergeTask.setMainManifestFile(manifestOutputBackup)
        manifestsMergeTask.doFirst {
            //println(">>>>>>>>>>> manifestsMergeTask.doFirst<<<<")
        }
        manifestsMergeTask.inputs.property("time",System.currentTimeMillis());//防止出现增量编译导致不合并

        manifestsMergeTask.doFirst copyAARClosure();

        List<File> list = new ArrayList<>()
        for (archiveLibrary in mAndroidArchiveLibraries) {
            list.add(archiveLibrary.getManifest())
        }
        manifestsMergeTask.setSecondaryManifestFiles(list)
        manifestsMergeTask.setOutputFile(manifestOutputFile)
        manifestsMergeTask.dependsOn processManifestTask

        manifestsMergeTask.doLast {
            mProject.copy {
                from manifestOutputFile
                into manifestOutputBackupDir
            }
            for (archiveLibrary in mAndroidArchiveLibraries) {
                println 'fat-aar-->[merge manifest] '+archiveLibrary.getManifest().absolutePath
            }
            //println(">>>>>>>>>manifestsMergeTask over")
        }
        processManifestTask.finalizedBy manifestsMergeTask

    }

    private Task buildJarRClassTask(def variant,Project project,File dustDir){
        def explodFile = new File(mProject.getBuildDir() , "/intermediates" + "/exploded-aar/");
        def rClassFile = new File(explodFile,"rclasses");
        if(!rClassFile.exists()){
            rClassFile.mkdirs()

        }
        String jarTaskName = "flat-jarRClass"+variant.getName().capitalize();
        //println("flat-jarRClass dustDir:"+dustDir.absolutePath+" rClassFile:"+rClassFile.absolutePath+" jarTaskName:"+jarTaskName)
        Task task = project.tasks.findByName(jarTaskName)
        if(task!=null){
            return task;
        }
        Task jarTask = mProject.task(type:Jar,jarTaskName){
            destinationDir dustDir
            from rClassFile
        }

        return jarTask;
    }

    private void processClassesAndJars() {//合并classes
        if (mVariant.getBuildType().isMinifyEnabled()) {//如果运行混淆
            for (archiveLibrary in mAndroidArchiveLibraries) {
                File thirdProguard = archiveLibrary.proguardRules
                if (!thirdProguard.exists()) {
                    continue
                }
                mProject.android.getDefaultConfig().proguardFile(thirdProguard)
            }
            Task javacTask = mVariant.getJavaCompile()
            if (javacTask == null) {
                // warn: can not find javaCompile task, jack compile might be on.
                return
            }
            def dustDir = mProject.file(mProject.buildDir.path + '/intermediates/classes/' + mVariant.dirName)

            javacTask.doLast {
                ExplodedHelper.processIntoClasses(mVariant,mProject, mAndroidArchiveLibraries, mJarFiles, dustDir)
            }
            javacTask.doFirst copyAARClosure();
            javacTask.finalizedBy(buildJarRClassTask(mVariant,mProject,dustDir))
        } else {
            String taskPath = 'transformClassesAndResourcesWithSyncLibJarsFor' + mVariant.name.capitalize()
            Task syncLibTask = mProject.tasks.findByPath(taskPath)
            if (syncLibTask == null) {
                throw new RuntimeException("Can not find task ${taskPath}!")
            }
            def dustDir = mProject.file(AndroidPluginHelper.resolveBundleDir(mProject, mVariant).path + '/libs')
            syncLibTask.doLast {
                ExplodedHelper.processIntoJars(mVariant,mProject, mAndroidArchiveLibraries, mJarFiles, dustDir)
            }
            syncLibTask.doFirst copyAARClosure();
            syncLibTask.finalizedBy(buildJarRClassTask(mVariant,mProject,dustDir))
        }
    }

    /**
     * merge R.txt(actually is to fix issue caused by provided configuration) and res
     *
     * Here I have to inject res into "main" instead of "variant.name".
     * To avoid the res from embed dependencies being used, once they have the same res Id with main res.
     *
     * Now the same res Id will cause a build exception: Duplicate resources, to encourage you to change res Id.
     * Adding "android.disableResourceValidation=true" to "gradle.properties" can do a trick to skip the exception, but is not recommended.
     */
    private void processResourcesAndR() {//在gennerateDebugResources这样的task后插入合并逻辑
        String taskPath = 'generate' + mVariant.name.capitalize() + 'Resources'
        Task resourceGenTask = mProject.tasks.findByPath(taskPath)
        if (resourceGenTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }
        resourceGenTask.doLast {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                ExplodedHelper.copyAARIfNeed(archiveLibrary,mProject)
                if(archiveLibrary.resFolder.exists()){

                    def srcSet = mProject.android.sourceSets.findByName(mVariant.name)
                    println 'fat-aar-->[merge res] '+archiveLibrary.resFolder
                    srcSet.res.srcDir(archiveLibrary.resFolder)
                }
            }
        }
    }

    /**
     * generate R.java
     */
    private void processRSources() {

        def variantOutput =mVariant.getOutputs().toArray()[0];
        Task processResourcesTask = variantOutput.getProcessResources()

        def processManifestTask = variantOutput.getProcessManifest();
        File manifestOutputBackupDir =processManifestTask.getManifestOutputDirectory();
        File manifestOutputBackup = new File(manifestOutputBackupDir.absolutePath+File.separator+"AndroidManifest.xml");


        processResourcesTask.doLast {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                ExplodedHelper.copyAARIfNeed(archiveLibrary,mProject)
                RSourceGenerator.generate(manifestOutputBackup,processResourcesTask.getSourceOutputDir(), archiveLibrary)
            }
        }
    }

    /**
     * merge assets
     *
     * AaptOptions.setIgnoreAssets and AaptOptions.setIgnoreAssetsPattern will work as normal
     */
    private void processAssets() {
        Task assetsTask = mVariant.getMergeAssets()
        if (assetsTask == null) {
            throw new RuntimeException("Can not find task in variant.getMergeAssets()!")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            if(archiveLibrary.assetsFolder.exists()){
                assetsTask.getInputs().dir(archiveLibrary.assetsFolder)
            }
        }
        assetsTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                ExplodedHelper.copyAARIfNeed(archiveLibrary,mProject)
                // the source set here should be main or variant?
                if(archiveLibrary.assetsFolder.exists()){

                    println 'fat-aar-->[merge assert] '+archiveLibrary.assetsFolder
                    def srcSet = mProject.android.sourceSets.findByName(mVariant.name)
                    srcSet.assets.srcDir(archiveLibrary.assetsFolder)
                }
            }
        }
    }

    /**
     * merge jniLibs
     */
    private void processJniLibs() {
        String taskPath = 'merge' + mVariant.name.capitalize() + 'JniLibFolders'
        Task mergeJniLibsTask = mProject.tasks.findByPath(taskPath)
        if (mergeJniLibsTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            if(archiveLibrary.jniFolder.exists()){
                //println(">>>>jniFolder:"+archiveLibrary.jniFolder)
                mergeJniLibsTask.getInputs().dir(archiveLibrary.jniFolder)
            }
            if(archiveLibrary.jniLocalFolder!=null && archiveLibrary.jniLocalFolder.exists()){
                //println(">>>>jniLocalFolder:"+archiveLibrary.jniLocalFolder)
                mergeJniLibsTask.getInputs().dir(archiveLibrary.jniLocalFolder)
            }
        }
        mergeJniLibsTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                ExplodedHelper.copyAARIfNeed(archiveLibrary,mProject)
                // the source set here should be main or variant?
                if(archiveLibrary.jniFolder.exists()){
                    println 'fat-aar-->[merge jni] '+archiveLibrary.jniFolder
                    def srcSet = mProject.android.sourceSets.findByName(mVariant.name)
                    srcSet.jniLibs.srcDir(archiveLibrary.jniFolder)
                }
                if(archiveLibrary.jniLocalFolder!=null && archiveLibrary.jniLocalFolder.exists()){
                    println 'fat-aar-->[merge jni] '+archiveLibrary.jniLocalFolder
                    def srcSet = mProject.android.sourceSets.findByName(mVariant.name)
                    srcSet.jniLibs.srcDir(archiveLibrary.jniLocalFolder)
                }
            }
        }
    }

    /**
     * merge proguard.txt
     */
    private void processProguardTxt(Task prepareTask) {
        String taskPath = 'merge' + mVariant.name.capitalize() + 'ProguardFiles'
        Task mergeFileTask = mProject.tasks.findByPath(taskPath)
        if (mergeFileTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            File thirdProguard = archiveLibrary.proguardRules
            if (!thirdProguard.exists()) {
                continue
            }
            mergeFileTask.getInputs().file(thirdProguard)
        }
        mergeFileTask.doFirst {
            Collection proguardFiles = mergeFileTask.getInputFiles()
            for (archiveLibrary in mAndroidArchiveLibraries) {
                ExplodedHelper.copyAARIfNeed(archiveLibrary,mProject)
                File thirdProguard = archiveLibrary.proguardRules
                if (!thirdProguard.exists()) {
                    continue
                }
                proguardFiles.add(thirdProguard)
            }
        }
        mergeFileTask.dependsOn prepareTask
    }
}
