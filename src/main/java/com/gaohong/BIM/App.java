package com.gaohong.BIM;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * @author archko
 */
public class App extends Application {

    public static final String TAG="App";
    public static boolean isLogined=false;

    @Override
    public void onCreate() {
        super.onCreate();

        Intent intent=new Intent(getApplicationContext(), FTPService.class);
        startService(intent);
        initCacheDir();
    }

    private void initCacheDir() {

    }

    public String getNetworkType() {
        ConnectivityManager connectivityManager=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo=connectivityManager.getActiveNetworkInfo();
        //NetworkInfo mobNetInfo = connectivityManager
        //		.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (activeNetInfo!=null) {
            return activeNetInfo.getExtraInfo(); // 接入点名称: 此名称可被用户任意更改 如: cmwap, cmnet,
            // internet ...
        } else {
            return null;
        }
    }

    public static boolean hasInternetConnection(Activity activity) {
        try {
            ConnectivityManager connectivity=(ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity==null) {
                return false;
            } else {
                NetworkInfo[] info=connectivity.getAllNetworkInfo();
                if (info!=null) {
                    for (int i=0; i<info.length; i++) {
                        if (info[i].getState()==NetworkInfo.State.CONNECTED) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager connectivity=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity==null) {
            return false;
        } else {
            NetworkInfo[] info=connectivity.getAllNetworkInfo();
            if (info!=null) {
                for (int i=0; i<info.length; i++) {
                    if (info[i].getState()==NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onTerminate() {
        isLogined=false;

        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
