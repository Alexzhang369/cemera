package com.gaohong.BIM;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

import java.util.HashMap;

public class FTPUtils {

    private static final String TAG="FTPUtils";

    public static FTPService sService=null;

    public static final String getFileSize(final long size) {
        if (size>1073741824) {
            return String.format("%.2f", size/1073741824.0)+" GB";
        } else if (size>1048576) {
            return String.format("%.2f", size/1048576.0)+" MB";
        } else if (size>1024) {
            return String.format("%.2f", size/1024.0)+" KB";
        } else {
            return size+" B";
        }
    }

    private static HashMap<Context, ServiceBinder> sConnectionMap=new HashMap<Context, ServiceBinder>();

    public static class ServiceToken {

        ContextWrapper mWrappedContext;

        ServiceToken(ContextWrapper context) {
            mWrappedContext=context;
        }
    }

    public static ServiceToken bindToService(Activity context) {
        return bindToService(context, null);
    }

    public static ServiceToken bindToService(Activity context, ServiceConnection callback) {
        Activity realActivity=context.getParent();
        if (realActivity==null) {
            realActivity=context;
        }
        ContextWrapper cw=new ContextWrapper(realActivity);
        cw.startService(new Intent(cw, FTPService.class));
        ServiceBinder sb=new ServiceBinder(callback);
        if (cw.bindService((new Intent()).setClass(cw, FTPService.class), sb, 0)) {
            sConnectionMap.put(cw, sb);
            return new ServiceToken(cw);
        }
        Log.e(TAG, "Failed to bind to service");
        return null;
    }

    public static void unbindFromService(ServiceToken token) {
        if (token==null) {
            Log.e(TAG, "Trying to unbind with null token");
            return;
        }
        ContextWrapper cw=token.mWrappedContext;
        ServiceBinder sb=sConnectionMap.remove(cw);
        if (sb==null) {
            Log.e(TAG, "Trying to unbind for unknown Context");
            return;
        }
        cw.unbindService(sb);
        if (sConnectionMap.isEmpty()) {
            // presumably there is nobody interested in the service at this point,
            // so don't hang on to the ServiceConnection
            sService=null;
        }
    }

    private static class ServiceBinder implements ServiceConnection {

        ServiceConnection mCallback;

        ServiceBinder(ServiceConnection callback) {
            mCallback=callback;
        }

        public void onServiceConnected(ComponentName className, android.os.IBinder service) {
            //sService=IFTPService.Stub.asInterface(service);
            sService=((FTPService.MyBinder) service).getService();
            Log.d(TAG, "onServiceConnected.sService:"+sService);
            //initAlbumArtCache();
            if (mCallback!=null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected.sService:"+sService+" className:"+className);
            if (mCallback!=null) {
                mCallback.onServiceDisconnected(className);
            }
            sService=null;
        }
    }

}
