package com.zb.moduleall;

import android.util.Log;

import com.zb.modulea.ModuleA;

public class FatCall {
    public static void call(){
        Log.e("FatAAR",">>>>>>>>>>FatCall call");
        ModuleA.call();

    }
}
