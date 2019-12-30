package com.zb.fataar;


import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Vigi on 2017/2/16.
 * Modified by lishusheng on 2019.03.21
 *
 */
public class AndroidArchiveLibrary {

    private final Project mProject;

    private final ResolvedArtifact mArtifact;

    private boolean isProjectArtifact;

    private String packageName;
    private String artifactVariantName;
    public AndroidArchiveLibrary(Project project, ResolvedArtifact artifact,boolean isProjectArtifact,String artifactVariantName) {
        if (!"aar".equals(artifact.getType())) {
            throw new IllegalArgumentException("artifact must be aar type!");
        }
        mProject = project;
        mArtifact = artifact;
        this.isProjectArtifact = isProjectArtifact;
        this.isCopyed = isProjectArtifact;
        this.artifactVariantName  = artifactVariantName;
        //System.out.println(">>>>>>>>>>>>>artifact file:"+artifactVariantName+"  artifact:"+artifact.getName());
    }
    private boolean isCopyed;

    public boolean isCopyed() {
        return isCopyed;
    }

    public ResolvedArtifact getArtifact() {
        return mArtifact;
    }

    public void setCopyed(boolean copyed) {
        isCopyed = copyed;
    }

    public String getPackageName() {
        if(this.packageName!=null){
            return packageName;
        }
        File manifestFile = this.getManifest();
        packageName = AndroidPluginHelper.parsePackageNameFromManifestFile(manifestFile);
        return packageName;
    }


    public String getGroup() {
        return mArtifact.getModuleVersion().getId().getGroup();
    }

    public String getName() {
        return mArtifact.getModuleVersion().getId().getName();
    }

    public String getVersion() {
        return mArtifact.getModuleVersion().getId().getVersion();
    }

    public File getProjectArtifactBuildFolder(){
        return  mProject.getRootProject().file(getName()+File.separator+"build"+File.separator+"intermediates");
    }

    public File getRootFolder() {
        File explodedRootDir = new File(mProject.getBuildDir() , "/intermediates" + "/exploded-aar/");
        ModuleVersionIdentifier id = mArtifact.getModuleVersion().getId();
        return mProject.file(explodedRootDir + "/" + id.getGroup() + "/" + id.getName() + "/" + id.getVersion());
    }
    public File getRClassesFolder(){
        File explodedRootDir = mProject.file(mProject.getBuildDir() + "/intermediates" + "/exploded-aar/");
        return new File(explodedRootDir,"rclasses");
    }
    public File getRClassPathFolder(){
        return new File(getRClassesFolder(),getPackageName().replace(".","/"));
    }

    private File getJarsRootFolder() {

        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"intermediate-jars"+File.separator+getProjectArtifactVariantName());
        }
        File folder = new File(getRootFolder(), "");
        if(!folder.exists()){
            folder.mkdirs();
        }
        return folder;
    }

    public String getProjectArtifactVariantName(){
        //MyProjectResolvedArtifact artifact = (MyProjectResolvedArtifact)mArtifact;
        //return artifact.variantName();
        return artifactVariantName;
    }
    public File getAidlFolder() {
        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"packaged-aidl"+File.separator+getProjectArtifactVariantName());
        }
        File folder = new File(getRootFolder(), "aidl");
        if(!folder.exists()){
            folder.mkdirs();
        }
        return  folder;
    }

    public File getAssetsFolder() {
        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"packagedAssets"+File.separator+getProjectArtifactVariantName());
        }
        File folder =  new File(getRootFolder(), "assets");
        if(!folder.exists()){
            folder.mkdirs();
        }
        return folder;
    }

    public File getClassesJarFile() {
        return new File(getJarsRootFolder(), "classes.jar");
    }

    public Collection<File> getLocalJars() {
        List<File> localJars = new ArrayList<>();
        File[] jarList = new File(getJarsRootFolder(), "libs").listFiles();
        if (jarList != null) {
            for (File jars : jarList) {
                if (jars.isFile() && jars.getName().endsWith(".jar")) {
                    localJars.add(jars);
                }
            }
        }

        return localJars;
    }

    public File getJniLocalFolder(){
        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"library_and_local_jars_jni"+File.separator+getProjectArtifactVariantName());
        }
        return null;
    }

    public File getJniFolder() {
        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"jniLibs"+File.separator+getProjectArtifactVariantName());
        }
        File folder = new File(getRootFolder(), "jni");
        if(!folder.exists()){
            folder.mkdirs();
        }
        return folder;
    }

    public File getResFolder() {
        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"packaged_res"+File.separator+getProjectArtifactVariantName());
        }
        File folder = new File(getRootFolder(), "res");
        if(!folder.exists()){
            folder.mkdirs();
        }
        return folder;
    }

    public File getManifest() {
        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"manifests"
                    +File.separator+"full"+File.separator+getProjectArtifactVariantName()+File.separator+"AndroidManifest.xml");
        }
        return new File(getRootFolder(), "AndroidManifest.xml");
    }

    public File getLintJar() {
        return new File(getJarsRootFolder(), "lint.jar");
    }

    public File getProguardRules() {
        return new File(getRootFolder(), "proguard.txt");
    }

    public File getSymbolFile() {
        if(isProjectArtifact){
            return new File(getProjectArtifactBuildFolder(),File.separator+"symbols"+File.separator+getProjectArtifactVariantName()+File.separator+"R.txt");
        }
        return new File(getRootFolder(), "R.txt");
    }
}
