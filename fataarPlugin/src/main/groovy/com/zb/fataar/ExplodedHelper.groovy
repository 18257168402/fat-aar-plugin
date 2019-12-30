package com.zb.fataar

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * Created by Vigi on 2017/1/20.
 * Modified by lishusheng on 2019.03.21
 */
class ExplodedHelper {

    private static void processRIntoJars(def variant,Project project, Collection<AndroidArchiveLibrary> androidLibraries, File folderOut){
        //println(">>>>>>classes "+variant.getJavaCompile().getDestinationDir().getAbsolutePath())
        String compileDest = variant.getJavaCompile().getDestinationDir().getAbsolutePath();
        for (androidLibrary in androidLibraries) {

            String outRDir = compileDest+File.separator+androidLibrary.getPackageName().replace(".","/")
            //println(">>>>outRDir:"+outRDir+"   pkgname:"+androidLibrary.getPackageName());
            File RClassPathFolder =androidLibrary.getRClassPathFolder()
            if(RClassPathFolder.exists()){
                RClassPathFolder.deleteDir();
            }else{
                RClassPathFolder.mkdirs();
            }
            project.copy {
                from outRDir
                into RClassPathFolder.getAbsolutePath()
            }
        }
    }
    public static void processIntoJars(def variant,Project project,
                                       Collection<AndroidArchiveLibrary> androidLibraries, Collection<File> jarFiles,
                                       File folderOut) {


        processRIntoJars(variant,project,androidLibraries,folderOut)
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.classesJarFile.exists()) {
                println 'fat-aar-->[warning]' + androidLibrary.classesJarFile + ' not found!'
                continue
            }
          println 'fat-aar-->[merge classes] from: ' + androidLibrary.classesJarFile.getAbsolutePath()
            def prefix = androidLibrary.name + '-' + androidLibrary.version
            project.copy {
                from(androidLibrary.classesJarFile)
                into folderOut
                rename { prefix + '.jar' }
            }
            project.copy {
                from(androidLibrary.localJars)
                into folderOut
                rename { prefix + '-' + it }
            }
        }
        for (jarFile in jarFiles) {
            if (!jarFile.exists()) {
                println 'fat-aar-->[warning]' + jarFile + ' not found!'
                continue
            }
          println 'fat-aar-->[merge classes] from: ' + jarFile
            project.copy {
                from(jarFile)
                into folderOut
            }
        }
    }
    public static void copyAARIfNeed(AndroidArchiveLibrary archiveLibrary,Project project){
        ResolvedArtifact artifact = archiveLibrary.getArtifact();
//        println(">>>>copyAARIfNeed<<< "+archiveLibrary.isCopyed()
//                +"  "+artifact.file.exists()+"  " +artifact.file.absolutePath)
        if(!archiveLibrary.isCopyed() && artifact.file.exists()){
            project.copy {
                from project.zipTree( artifact.file )
                into archiveLibrary.getRootFolder()
            }
            println("fat-aar-->unzip:"+artifact.file.absolutePath)
            archiveLibrary.setCopyed(true)
        }
    }
    public static void processIntoClasses(def variant, Project project,
                                          Collection<AndroidArchiveLibrary> androidLibraries, Collection<File> jarFiles,
                                          File folderOut) {
        processRIntoJars(variant,project,androidLibraries,folderOut)
        Collection<File> allJarFiles = new ArrayList<>()
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.classesJarFile.exists()) {
                println 'fat-aar-->[warning]' + androidLibrary.classesJarFile + ' not found!'
                continue
            }
            allJarFiles.add(androidLibrary.classesJarFile)
            allJarFiles.addAll(androidLibrary.localJars)
        }
        for (jarFile in jarFiles) {
            if (!jarFile.exists()) {
                println 'fat-aar-->[warning]' + jarFile + ' not found!'
                continue
            }
            allJarFiles.add(jarFile)
        }
        for (jarFile in allJarFiles) {
//          println 'fat-aar-->copy classes from: ' + jarFile
            project.copy {
                from project.zipTree(jarFile)
                into folderOut
                include '**/*.class'
                exclude 'META-INF/'
            }
        }
    }
}
