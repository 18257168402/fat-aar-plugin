package com.zb.fataar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedModuleVersion
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency


/**
 *
 * Created by Vigi on 2017/1/14.
 * Modified by lishusheng on 2019.03.21
 */
class FatLibraryPlugin implements Plugin<Project> {

    private Project project
    private Configuration embedConf

    private Set<ResolvedArtifact> artifacts
    private List<Dependency> mProjectDeps;
    private List<Dependency> mEmbedDeps;
    @Override
    void apply(Project project) {
        this.project = project


        if(checkAndroidPlugin()){
            createConfiguration(true)
            return
        }
        createConfiguration(false)
        project.afterEvaluate {
            mEmbedDeps = new ArrayList<>()
            resolveArtifacts()
            project.android.libraryVariants.all { variant ->
                processVariant(variant)
                //println(">>>>>>>>>>>>variant:"+variant.getCompileConfiguration())
            }
        }
        project.ext.publishFatAAR = {pom->
            pom.withXml{
                //println(">>>all deps:"+mEmbedDeps.size())
                def dependenciesNode = it.asNode() // 返回XmlParser

                def depNode = dependenciesNode.dependencies
                def nodes = depNode.dependency
                for(dep in nodes){
                   // println(">>>>dep:"+dep)
                    //println ">>>groupId:"+dep.groupId.text()+" artifactId:"+dep.artifactId.text()
                    for(Dependency dependency:mEmbedDeps){
                        if(     dep.groupId.text().equals(dependency.group) &&
                                dep.artifactId.text().equals(dependency.name) &&
                                dep.version.text().equals(dependency.version)){
                            //println(">>>>remove node:"+dep)
                            dep.parent().remove(dep)
                        }
                    }

                }
                println("fat-aar-->[embed publish] removed all embed dependecies")
            }
        }
    }

    private boolean checkAndroidPlugin() {
        return !project.plugins.hasPlugin('com.android.library');
    }

    private def createConfiguration(boolean isImpl) {

        embedConf = project.configurations.create('embed')
        project.dependencies {
            implementation embedConf
        }
//        embedConf.visible = false
//        embedConf.transitive = false
//

//        def listeners =  Reflect.on(embedConf).get("dependencyResolutionListeners");
//        if(listeners!=null){
//            listeners.add(new DependencyResolutionListener(){
//                public void beforeResolve(ResolvableDependencies dependencies){
//                    println(">>>>>>>>>>beforeResolve:"+dependencies.name)
//                }
//                public void afterResolve(ResolvableDependencies dependencies){
//                    println(">>>>>>>>>>afterResolve:"+dependencies.name)
//                    def artifacts =  dependencies.getArtifacts();
//                    artifacts.forEach{artifact->
//                        println(">>>>>>>>>>afterResolve artifact:"+artifact.getFile())
//                    }
//                }
//            })
//        }

        project.gradle.addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies resolvableDependencies) {
//                embedConf.dependencies.each { dependency ->
//                    /**
//                     * use provided instead of compile.
//                     * advantage:
//                     *   1. prune dependency node in generated pom file when upload aar library archives.
//                     *   2. make invisible to the android application module, thus to avoid some duplicated processes.
//                     * side effect:
//                     *   1. [Fixed]incorrect R.txt in bundle. I fixed it by another way.
//                     *   2. [Fixed]loss R.java that is supposed to be generated. I make it manually.
//                     *   3. [Fixed]proguard.txt of embedded dependency is excluded when proguard.
//                     *   4. any other...
//                     */
//                    project.dependencies.add('implementation', dependency)
//                }
//                project.gradle.removeListener(this)
            }

            @Override
            void afterResolve(ResolvableDependencies resolvableDependencies) {

            }
        })
        return embedConf;
    }

    private void resolveArtifacts() {
        def set = new HashSet<>()
        try {
            def dependecies = embedConf.dependencies;
            mEmbedDeps.addAll(dependecies)
            List<Dependency> projDepList = new ArrayList<>();
            dependecies.forEach{dep->
                String targetConfiguratoin = dep.getTargetConfiguration();
                //println(">>>dep:"+dep.name+"  "+dep.group+"  "+dep.version+" "+targetConfiguratoin)
                if(dep instanceof DefaultProjectDependency){
//                    if(dep.getTargetConfiguration()==null){
//                        dep.setTargetConfiguration("default")
//                        println(">>>dep reset!!!!");
//                    }
                    project.dependencies.add('implementation', dep)
                    projDepList.add(dep)
                }
            }
            dependecies.removeAll(projDepList)
            mProjectDeps = projDepList;
            //println(">>>>>>>>>>resolve state:"+embedConf.getState())
            def resultConfigurations = embedConf.resolvedConfiguration;
            def firstLevelDeps =new ArrayList<>(resultConfigurations.getFirstLevelModuleDependencies()) ;
            projDepList.forEach({dep->
                println 'fat-aar-->[embed detected][project] ' + dep.name
            })
            firstLevelDeps.reverseEach {
                //println("fat-aar-->[depend module] "+it.moduleName+" configuration:"+it.getConfiguration())
                it.moduleArtifacts.each { artifact ->
                    ResolvedModuleVersion mv = artifact.getModuleVersion();
                    //println(">>>>>>>>>>>>artifact:"+artifact.class)
                    if ('aar'.equals(artifact.type) || 'jar'.equals(artifact.type)) {
                        println 'fat-aar-->[embed detected][' + artifact.type + ']' + artifact.moduleVersion.id
                    } else {
                        throw new ProjectConfigurationException('Only support embed aar and jar dependencies!', null)
                    }
                    set.add(artifact)
                }
            }
            this.artifacts = Collections.unmodifiableSet(set)
        }catch (Exception e){
            e.printStackTrace()
        }
    }

    private void processVariant(variant) {
        def processor = new VariantProcessor(project, variant)
        for (artifact in artifacts) {
            if ('aar'.equals(artifact.type)) {
                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact,false,"")
                processor.addAndroidArchiveLibrary(archiveLibrary)
            }
            if ('jar'.equals(artifact.type)) {
                processor.addJarFile(artifact.file)
            }
        }
        for (Dependency projDep in mProjectDeps){
            MyProjectResolvedArtifact resolvedArtifact = new MyProjectResolvedArtifact(project,variant,projDep);
            AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project,
                    resolvedArtifact ,true,resolvedArtifact.variantDir())
            processor.addAndroidArchiveLibrary(archiveLibrary)
        }

        processor.processVariant()
    }
}
