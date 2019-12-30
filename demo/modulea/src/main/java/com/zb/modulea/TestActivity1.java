package com.zb.modulea;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

public class TestActivity1 extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText(">>>>>TestActivity1<<<<<:"+getString(R.string.modulea_app_name));
        setContentView(tv);
    }
}
