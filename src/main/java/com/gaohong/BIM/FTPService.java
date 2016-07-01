package com.gaohong.BIM;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-1
 */
public class FTPService extends Service {

    public static final String TAG="FTPService";
    public FtpUser mFtpUser;
    public FTPClient mFtpClient;
    ArrayList<String> mQueue=new ArrayList<String>();
    TransferNotifyListener mNotifyListener;
    TransState mTransState=TransState.TRANNONE;
    long mTotalSize=0;
    long mCurrIndex=0;
    private NotificationManager mNM;
    private static final int NOTIFICATION_FLAG = 1;

    FTPDataTransferListener listener=new FTPDataTransferListener() {

        @Override
        public void started() {
            mTransState=TransState.TRANSTART;
        }

        @Override
        public void started(long totalSize) {
            Log.d(TAG, "transferred.mTotalSize:"+totalSize);
            mTransState=TransState.TRANSTART;
            mTotalSize=totalSize;
            if (null!=mNotifyListener) {
                mNotifyListener.started(totalSize);
            }
        }

        @Override
        public void transferred(int length) {
        }

        @Override
        public void transferred(long length) {
            Log.d(TAG, "transferred.length:"+mTransState);
            if (mTransState!=TransState.TRANSING) {
                mTransState=TransState.TRANSING;
            }

            mCurrIndex=length;
            if (null!=mNotifyListener) {
                mNotifyListener.transferred(length);
            }
        }

        @Override
        public void completed() {
            Log.d(TAG, "completed");
            mTransState=TransState.TRANSEND;
            if (null!=mNotifyListener) {
                mNotifyListener.completed();
            }
        }

        @Override
        public void aborted() {
            Log.d(TAG, "aborted");
            mTransState=TransState.TRANSEND;
            if (null!=mNotifyListener) {
                mNotifyListener.aborted();
            }
        }

        @Override
        public void failed() {
            Log.d(TAG, "failed");
            mTransState=TransState.TRANSEND;
            if (null!=mNotifyListener) {
                mNotifyListener.failed("Trans failed");
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind."+intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind.");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //------------------- aidl ----------------------
    private MyBinder binder=null;

    public class MyBinder extends Binder {

        public FTPService getService() {
            return FTPService.this;
        }
    }

    /**
     * 设置新用户，用户切换，如果存在就注销，然后换到新的用户
     *
     * @param host     ftp主机
     * @param port     端口
     * @param username 用户名
     * @param password 密码
     */
    public void setFtpUser(String host, int port, String username, String password) {
        if (null!=mFtpClient) {
            if (mFtpClient.isConnected()&&!mFtpUser.username.equals(username)) {
                logout();
            }
        }

        mFtpUser=new FtpUser();
        mFtpUser.host=host;
        mFtpUser.port=port;
        mFtpUser.username=username;
        mFtpUser.password=password;
    }

    /**
     * 这里在外面有非ui线程来处理
     */
    public FTPClient login() throws FTPException {
        if (mFtpUser==null) {
            throw new IllegalStateException("User is not exists.");
        }

        try {
            if (null!=mFtpClient) {
                if (mFtpClient.isConnected()) {
                    mFtpClient.logout();
                }
            } else if (null==mFtpClient) {
                mFtpClient=new FTPClient();
            }

            mFtpClient.connect(mFtpUser.host, mFtpUser.port);
            mFtpClient.login(mFtpUser.username, mFtpUser.password, null);

            /*FTPFile[] list=mFtpClient.list();
            for (FTPFile file : list) {
                Log.d(TAG, "file:"+file);
            }*/

            return mFtpClient;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FTPIllegalReplyException e) {
            e.printStackTrace();
        } /* catch (FTPException e) {
            e.printStackTrace();
        }catch (FTPDataTransferException e) {
            e.printStackTrace();
        } catch (FTPAbortedException e) {
            e.printStackTrace();
        } catch (FTPListParseException e) {
            e.printStackTrace();
        }*/

        return null;
    }

    /**
     * 注销，同时也将一些属性重置
     */
    public void logout() {
        reset();

        if (null!=mFtpClient) {
            try {
                mFtpClient.disconnect(true);
                mFtpClient.logout();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
            }
        }
    }

    private void reset() {
        mTransState=TransState.TRANNONE;
        mFtpUser=null;
        mNotifyListener=null;
        mQueue.clear();
        mTotalSize=0;
        mCurrIndex=0;
    }

    public FTPClient getFtpClient() {
        return mFtpClient;
    }

    Runnable uploadRunnable=new Runnable() {
        @Override
        public void run() {
            try {
                transFile();
                mNM.cancel(R.string.local_service_started);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
            } catch (FTPDataTransferException e) {
                e.printStackTrace();
            } catch (FTPAbortedException e) {
                e.printStackTrace();
            }
        }
    };

    public synchronized void setNotifyListener(TransferNotifyListener notifyListener) {
        this.mNotifyListener=notifyListener;
    }

    public void setFile(String filepath) {
        mQueue.clear();
        mQueue.add(filepath);
    }

    /**
     * 开始传输
     *
     * @return -1表示需要传输的文件列表为空，-2表示没有初始化FtpClient，0表示准备就绪，可以传输了
     */
    public int preTransFile() {
        if (mQueue.size()<1) {
            Log.d(TAG, "no file to trans.");
            return -1;
        }

        if (null==mFtpClient||!mFtpClient.isConnected()) {
            Log.d(TAG, "mFtpClient is null or it's disconnected.");
            return -2;
        }

        showNotification();
        Thread thread=new Thread(uploadRunnable);
        thread.start();
        return 0;
    }

    /**
     * 传输文件，
     *
     * @throws IllegalStateException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FTPIllegalReplyException
     * @throws FTPException
     * @throws FTPDataTransferException
     * @throws FTPAbortedException
     */
    private void transFile() throws IllegalStateException,
        FileNotFoundException, IOException,
        FTPIllegalReplyException, FTPException,
        FTPDataTransferException, FTPAbortedException {

        //mFtpClient.upload(new File("k:\\f.f4v"),listener);
        /*InputStream inputStream=new FileInputStream(mFilePath);
        mFtpClient.upload(mFilePath, inputStream, 0, 0, listener);*/

        String filepath=mQueue.get(0);
        File file=new File(filepath);
        if (file.exists()) {
            mFtpClient.upload(file, listener);
        } else {
            Log.d(TAG, "file is not exists.");
            if (null!=mNotifyListener) {
                mTransState=TransState.TRANSEND;
                mNotifyListener.failed("["+filepath+"] is not exists.");
            }
        }
    }

    public void abort() {
        if (null!=mFtpClient&&mTransState==TransState.TRANSING) {
            try {
                Log.d(TAG, "abort Trans.");
                mFtpClient.abortCurrentDataTransfer(true);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            }
        }

        reset();
    }

    //------------------  ----------------------
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mNM=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        abort();
        logout();
        mFtpClient=null;
        mNM.cancel(R.string.local_service_started);
    }

    public void showNotification() {
        PendingIntent pendingIntent2 = PendingIntent.getActivity(this, 0,
                new Intent(this, FileActivity.class), 0);
        // 通过Notification.Builder来创建通知，注意API Level
        // API11之后才支持
        Notification notify2 = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.xiaologo) // 设置状态栏中的小图片，尺寸一般建议在24×24，这个图片同样也是在下拉状态栏中所显示，如果在那里需要更换更大的图片，可以使用setLargeIcon(Bitmap
                        // icon)
                .setTicker("小云拍" + "文件正在上传")// 设置在status
                        // bar上显示的提示文字
                .setContentTitle("方程聊")// 设置在下拉status
                        // bar后Activity，本例子中的NotififyMessage的TextView中显示的标题
                .setContentText("正在运行")// TextView中显示的详细内容
                .setContentIntent(pendingIntent2) // 关联PendingIntent
                .setNumber(1) // 在TextView的右方显示的数字，可放大图片看，在最右侧。这个number同时也起到一个序列号的左右，如果多个触发多个通知（同一ID），可以指定显示哪一个。
                .getNotification(); // 需要注意build()是在API level
        // 16及之后增加的，在API11中可以使用getNotificatin()来代替
        notify2.flags |= Notification.FLAG_AUTO_CANCEL;

        mNM.notify(NOTIFICATION_FLAG, notify2);
    }
    public void showNotification_download(String filename) {
        PendingIntent pendingIntent2 = PendingIntent.getActivity(this, 0,
                new Intent(this, FileHomeActivity.class), 0);
        // 通过Notification.Builder来创建通知，注意API Level
        // API11之后才支持
        Notification notify2 = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.xiaologo) // 设置状态栏中的小图片，尺寸一般建议在24×24，这个图片同样也是在下拉状态栏中所显示，如果在那里需要更换更大的图片，可以使用setLargeIcon(Bitmap
                        // icon)
                .setTicker("小云拍" + "文件正在下载")// 设置在status
                        // bar上显示的提示文字
                .setContentTitle("方程聊")// 设置在下拉status
                        // bar后Activity，本例子中的NotififyMessage的TextView中显示的标题
                .setContentText("文件被下载到xiaoyunpai文件夹")// TextView中显示的详细内容
                .setContentIntent(pendingIntent2) // 关联PendingIntent
                .setNumber(1) // 在TextView的右方显示的数字，可放大图片看，在最右侧。这个number同时也起到一个序列号的左右，如果多个触发多个通知（同一ID），可以指定显示哪一个。
                .getNotification(); // 需要注意build()是在API level
        // 16及之后增加的，在API11中可以使用getNotificatin()来代替
        notify2.flags |= Notification.FLAG_NO_CLEAR;
        notify2.defaults |=Notification.DEFAULT_VIBRATE;

        mNM.notify(NOTIFICATION_FLAG, notify2);
    }
}
