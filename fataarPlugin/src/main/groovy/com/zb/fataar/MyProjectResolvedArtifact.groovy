package com.zb.fataar

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedModuleVersion
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier

/**
 * Created by lishusheng on 2019.03.21
 */
class MyProjectResolvedArtifact implements ResolvedArtifact{

    private Project mMainProject;
    private Project mSubProject;
    private def mMainVariant;
    private Dependency mMainDependency;
    private List<Object> mSubVariants;
    public MyProjectResolvedArtifact(Project mainProject,def mainVariant,Dependency dependency){
        mMainProject = mainProject;
        mMainVariant = mainVariant;
        mMainDependency = dependency;
        //println("MyProjectResolvedArtifact dep:"+mMainDependency.group+"  "+mMainDependency.name+" "+mMainDependency.version);
        //println("MyProjectResolvedArtifact mainVariant:"+mainVariant.name)

        mSubProject = mMainProject.rootProject.project(mMainDependency.name);
        mSubVariants = new ArrayList<>();
        //println(">>>>>subProj:"+mSubProject+"  dep["+mMainDependency.name+"]")
        mSubProject.android.libraryVariants.all {variant ->
//            println(">>>>>>>>>>>>sub variant:"+variant.name)
//            println(">> discription:"+variant.description)
//            println(">> baseName:"+variant.baseName)
//            println(">> dirName:"+variant.dirName)
//            println(">> buildType:"+variant.getBuildType().name)
//            println(">> flavorName:"+variant.getFlavorName())
//            println(">> mergedFlavor name:"+variant.getMergedFlavor().name)
            mSubVariants.add(variant);
        }
    }

    public def matchVariant(){
        def matchVariant = mSubVariants.find {variant->//完全匹配
            boolean isSame = variant.name.equals(mMainVariant.name);
            if(isSame){
               // println(">>>>>isSame variant:"+variant.name+" mainVar:"+mMainVariant.name)
            }
            return isSame
        }
        if(matchVariant==null){//buildType匹配
            matchVariant = mSubVariants.find {variant->
                boolean isSub = mMainVariant.getBuildType().name.toLowerCase().equals(variant.getBuildType().name.toLowerCase());
                if(isSub){
                  //  println(">>>>>isSub variant:"+variant.name+" mainVar:"+mMainVariant.name)
                }
                return isSub;
            }
        }

        return matchVariant
    }

    public String variantDir(){
        def matchVariant = matchVariant();
        if(matchVariant!=null){
            return matchVariant.dirName
        }else{
            return "debug"
        }
    }
    public  String variantName(){
        def matchVariant = matchVariant();
        if(matchVariant!=null){
            return matchVariant.name
        }else{
            return "debug"
        }
    }

    @Override
    File getFile() {
        String variantName = mMainVariant.name;
        return mMainProject.rootProject.file(mMainDependency.name+File.separator+"build/outputs/aar/"+mMainDependency.name+"-"+variantName+".aar");
    }
    @Override
    ResolvedModuleVersion getModuleVersion() {
        return new FadeResolvedModuleVersion();
    }
    @Override
    String getName() {
        return mMainDependency.name
    }
    @Override
    String getType() {
        return "aar"
    }
    @Override
    String getExtension() {
        return "aar"
    }
    @Override
    String getClassifier() {
        return null
    }
    @Override
    ComponentArtifactIdentifier getId() {
        return null
    }


    class  FadeModuleIdentifier implements ModuleIdentifier{
        @Override
        String getGroup() {
            return mMainDependency.group
        }
        @Override
        String getName() {
            return mMainDependency.name
        }
    }
    class FadeModuleVersionIdentifier implements ModuleVersionIdentifier{
        @Override
        String getVersion() {
            return mMainDependency.version
        }

        @Override
        String getGroup() {
            return mMainDependency.group
        }

        @Override
        String getName() {
            return mMainDependency.name
        }

        @Override
        ModuleIdentifier getModule() {
            return new FadeModuleIdentifier()
        }
    }
    class FadeResolvedModuleVersion implements ResolvedModuleVersion{
        @Override
        ModuleVersionIdentifier getId() {
            return new FadeModuleVersionIdentifier();
        }
    }
}