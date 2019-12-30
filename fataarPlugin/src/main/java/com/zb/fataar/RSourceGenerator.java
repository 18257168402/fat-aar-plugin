package com.zb.fataar;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Vigi on 2017/2/28.
 * Modified by lishusheng on 2019.03.21
 */
public class RSourceGenerator {




    public static void generate(File mainManifest,File outputDir, AndroidArchiveLibrary androidLibrary) throws IOException {
        // check
        File symbolFile = androidLibrary.getSymbolFile();
        File manifestFile = androidLibrary.getManifest();
        if (!symbolFile.exists()) {
            System.out.println("fat-aar-->[gen R]symbolFile:"+symbolFile.getAbsolutePath()+" not exists!");
            return;
        }
        if (!manifestFile.exists()) {
            throw new RuntimeException("Can not find " + manifestFile);
        }
        // read R.txt
        List<String> lines = Utils.readLines(symbolFile, Charset.forName("UTF-8"));
        Map<String, List<TextSymbolItem>> symbolItemsMap =  new HashMap<String, List<TextSymbolItem>>();
        for (String line : lines) {
            String[] strings = line.split(" ", 4);
            TextSymbolItem symbolItem = new TextSymbolItem();
            symbolItem.type = strings[0];
            symbolItem.clazz = strings[1];
            symbolItem.name = strings[2];
            symbolItem.value = strings[3];
            List<TextSymbolItem> symbolItems = symbolItemsMap.get(symbolItem.clazz);
            if (symbolItems == null) {
                symbolItems = new ArrayList();
                symbolItemsMap.put(symbolItem.clazz, symbolItems);
            }
            symbolItems.add(symbolItem);
        }
        if (symbolItemsMap.isEmpty()) {
            // empty R.txt
            System.out.println("fat-aar-->[gen R] symbol isEmpty!");
            return;
        }
        // parse package name
        String packageName = androidLibrary.getPackageName();
        String mainPackageName = AndroidPluginHelper.parsePackageNameFromManifestFile(mainManifest);

        // write R.java
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("R")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("AUTO-GENERATED FILE.  DO NOT MODIFY.\n")
                .addJavadoc("\n")
                .addJavadoc("This class was automatically generated by the\n"
                        + "fat-aar-plugin (https://github.com/Vigi0303/fat-aar-plugin)\n"
                        + "from the R.txt of the dependency it found.\n"
                        + "It should not be modified by hand.");
        for (String clazz : symbolItemsMap.keySet()) {
            TypeSpec.Builder icb = TypeSpec.classBuilder(clazz)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
            List<TextSymbolItem> tsis = symbolItemsMap.get(clazz);
            for (TextSymbolItem item : tsis) {
                TypeName typeName = null;
                if ("int".equals(item.type)) {
                    typeName = TypeName.INT;
                }
                if ("int[]".equals(item.type)) {
                    typeName = TypeName.get(int[].class);
                }
                if (typeName == null) {
                    throw new RuntimeException("Unknown class type in " + symbolFile);
                }
                FieldSpec fieldSpec = FieldSpec.builder(typeName, item.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)  // Is the "final" necessary?
                        //.initializer(item.value)
                        .initializer("$N.R.$N.$N",mainPackageName,item.clazz,item.name)
                        .build();
                icb.addField(fieldSpec);
            }
            classBuilder.addType(icb.build());
        }
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();
        javaFile.writeTo(outputDir);
        System.out.println("fat-aar-->[gen R] file:"+outputDir+packageName+".R.java");
    }

    private static class TextSymbolItem {
        String type, clazz, name, value;
    }
}
