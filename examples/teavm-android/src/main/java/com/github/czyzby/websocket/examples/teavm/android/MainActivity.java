package com.github.czyzby.websocket.examples.teavm.android;

import android.os.Bundle;

import com.github.xpenatan.gdx.teavm.android.TeaAndroidActivity;

public class MainActivity extends TeaAndroidActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        TeaVMAndroidTextInputBridge.setActivity(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        TeaVMAndroidTextInputBridge.setActivity(null);
        super.onDestroy();
    }
}
