package com.gaohong.BIM;


import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;

import com.R;

public class DemoActivity extends Activity {
    WebView webview;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        webview=(WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setPluginState(PluginState.ON);
        webview.loadUrl("http://m.cecall.cc/cemeet/224/");
    }
}