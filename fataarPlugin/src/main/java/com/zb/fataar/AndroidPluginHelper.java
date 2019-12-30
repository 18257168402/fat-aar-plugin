package com.zb.fataar;


import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.util.GradleVersion;
import org.gradle.util.TextUtil;
import org.gradle.util.VersionNumber;
import org.joor.Reflect;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Vigi on 2017/3/5.
 * Modified by lishusheng on 2019.03.21
 */
public class AndroidPluginHelper {

    /**
     * Resolve from com.android.builder.Version#ANDROID_GRADLE_PLUGIN_VERSION
     *
     * Throw exception if can not found
     */
    public static String getAndroidPluginVersion() {
        //return Reflect.on("com.android.builder.Version").get("ANDROID_GRADLE_PLUGIN_VERSION");
        String gradleVersionStr = GradleVersion.current().getVersion();
        return gradleVersionStr;
    }

    public static String parsePackageNameFromManifestFile(File manifestFile){
        String packageName = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(manifestFile);
            Element element = doc.getDocumentElement();
            packageName = element.getAttribute("package");
        } catch (Exception ignored) {}
        if (Utils.isEmpty(packageName)) {
            throw new RuntimeException("Parse package from " + manifestFile + " error!");
        }
        return packageName;
    }

    /**
     * return bundle dir of specific variant
     */
    public static File resolveBundleDir(Project project, Object variant) {
//        if (VersionNumber.parse(getAndroidPluginVersion()).compareTo(VersionNumber.parse("2.3.0")) < 0) {
//            String dirName = Reflect.on(variant).call("getDirName").get();
//            if (Strings.isNullOrEmpty(dirName)) {
//                return null;
//            }
//            return project.file(project.getBuildDir() + "/intermediates/bundles/" + dirName);
//        } else {
            // do the trick getting assets task output
            Task mergeAssetsTask = Reflect.on(variant).call("getMergeAssets").get();
            File assetsDir = Reflect.on(mergeAssetsTask).call("getOutputDir").get();

            Task mergeResources = Reflect.on(variant).call("getMergeResources").get();
            File resourcesDir = Reflect.on(mergeResources).call("getOutputDir").get();

            String dirName = Reflect.on(variant).call("getDirName").get();

            //System.out.println(">>>>>dirName: "+dirName);
            //System.out.println(">>>>>getMergeAssets: "+assetsDir.getAbsolutePath());
            //System.out.println(">>>>>getMergeResources: "+resourcesDir.getAbsolutePath());
            return new File(project.getBuildDir() +"/intermediates/packaged-classes/"+dirName);//
            //return assetsDir.getParentFile();
//        }
    }
}
