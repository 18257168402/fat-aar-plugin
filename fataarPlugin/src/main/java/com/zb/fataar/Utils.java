package com.zb.fataar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static boolean isEmpty(CharSequence s) {
        if (s == null) {
            return true;
        } else {
            return s.length() == 0;
        }
    }

    public static List<String> readLines(File file, Charset charset){
        List<String> lines = new ArrayList<>();
        BufferedReader br=null;
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis,charset);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine())!=null){
                lines.add(line);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(br!=null){
                    br.close();
                }
            }catch (Exception e){}
        }
        return lines;
    }
}
