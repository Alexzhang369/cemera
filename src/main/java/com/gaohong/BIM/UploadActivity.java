package com.gaohong.BIM;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.R;
import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-1
 */
public class UploadActivity extends Activity {

    public static final String TAG="UploadActivity";
    public static final int FTP_BROWSER_FILE=0x1001;
    FTPClient mFtpClient=null;
    TextView uploadTxt;
    String mFilePath=null;
    Button uploadBtn, chooseBtn, stopBtn, loginBtn;
    private ProgressBar pb;
    boolean isUploading=false;
    Button progressBtn;

    FTPDataTransferListener listener=new FTPDataTransferListener() {
        long mTotalSize=100;

        @Override
        public void started() {
            System.out.println("started");
        }

        @Override
        public void started(long totalSize) {
            Log.d(TAG, "transferred.mTotalSize:"+totalSize);
            this.mTotalSize=totalSize;
            Message msg;

            msg=Message.obtain();
            msg.what=2;
            msg.obj=(int) totalSize;
            UploadActivity.this.mHandler.sendMessage(msg);
        }

        @Override
        public void transferred(int length) {
        }

        @Override
        public void transferred(long length) {
            Log.d(TAG, "transferred.length:"+length);
            int progress=(int) (length*100.0/mTotalSize);

            Message msg;
            msg=Message.obtain();
            msg.what=1;
            msg.obj=progress;
            UploadActivity.this.mHandler.sendMessage(msg);
        }

        @Override
        public void completed() {
            Log.d(TAG, "completed");
            Message msg;
            msg=Message.obtain();
            msg.what=3;
            mHandler.sendMessage(msg);
        }

        @Override
        public void aborted() {
            Log.d(TAG, "aborted");
            Message msg;
            msg=Message.obtain();
            msg.what=4;
            mHandler.sendMessage(msg);
        }

        @Override
        public void failed() {
            Log.d(TAG, "failed");
            Message msg;
            msg=Message.obtain();
            msg.what=5;
            mHandler.sendMessage(msg);
        }
    };

    Handler mHandler=new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what=msg.what;
            Integer progress=(Integer) msg.obj;
            switch (what) {
                case 1:
                    //Log.d(TAG, "setProgress(progress)"+progress);
                    pb.setProgress(progress);
                    break;

                case 2:
                    Log.d(TAG, "setMax(progress)"+progress);
                    pb.setVisibility(View.VISIBLE);
                    pb.setMax(100);
                    break;

                case 3:
                    //pb.setVisibility(View.GONE);
                    break;

                case 4: //aborted
                    //pb.setVisibility(View.GONE);
                    break;

                case 5: //failed
                    //pb.setVisibility(View.GONE);
                    break;
            }
        }
    };

    View.OnClickListener clickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id=view.getId();
            switch (id) {
                case R.id.choose_btn:
                    chooseFile();
                    break;

                case R.id.upload_btn:
                    uploadFile();
                    break;

                case R.id.stop_btn:
                    stopUpload();
                    break;

                case R.id.login_btn:
                    login();
                    break;

                case R.id.progress_btn:
                    break;
            }
        }
    };

    private void login() {
        String host="";
        String user="";
        String pass="";
        try {
            mFtpClient=new FTPClient();
            mFtpClient.connect(host, 2001);
            mFtpClient.login(user, pass, null);

            FTPFile[] list=mFtpClient.list();
            for (FTPFile file : list) {
                Log.d(TAG, "file:"+file);
            }

            //transFile(mFtpClient);
            //client.createDirectory("newDir");
            //client.deleteDirectory("newDir");
            //mFtpClient.disconnect(true);
        } catch (IllegalStateException e) {
            e.printStackTrace();
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
        } catch (FTPListParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);

        uploadTxt=(TextView) findViewById(R.id.upload_txt);
        uploadBtn=(Button) findViewById(R.id.upload_btn);
        chooseBtn=(Button) findViewById(R.id.choose_btn);
        stopBtn=(Button) findViewById(R.id.stop_btn);
        loginBtn=(Button) findViewById(R.id.login_btn);
        pb=(ProgressBar) findViewById(R.id.progress_bar);
        progressBtn=(Button) findViewById(R.id.progress_btn);

        uploadBtn.setOnClickListener(clickListener);
        chooseBtn.setOnClickListener(clickListener);
        stopBtn.setOnClickListener(clickListener);
        loginBtn.setOnClickListener(clickListener);
        progressBtn.setOnClickListener(clickListener);
    }

    /**
     * 选择上传的文件
     */
    void chooseFile() {
        uploadBtn.setEnabled(false);
        mFilePath=null;
        Intent intent=new Intent(UploadActivity.this, FileActivity.class);
        startActivityForResult(intent, FTP_BROWSER_FILE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null!=mFtpClient) {
            try {
                mFtpClient.disconnect(true);
                mFtpClient=null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode:"+requestCode+" resultCode:"+resultCode+" data:"+data);
        if (resultCode==RESULT_OK) {
            if (requestCode==FTP_BROWSER_FILE) {
                String file_path=data.getStringExtra("file_path");
                if (!TextUtils.isEmpty(file_path)) {
                    //uploadFile(file_path);
                    mFilePath=file_path;
                    uploadBtn.setEnabled(true);
                    uploadTxt.setText(file_path);
                }
            }
        }
    }

    private void stopUpload() {
        if (null!=mFtpClient||mFtpClient.isConnected()) {
            Log.d(TAG, "stopUpload");
            try {
                mFtpClient.abortCurrentDataTransfer(true);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 上传文件到ftp
     *
     * @param file_path 文件的路径
     */
    private void uploadFile() {
        if (null==mFtpClient||!mFtpClient.isConnected()) {
            Toast.makeText(UploadActivity.this, "Ftp is not connected.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    isUploading=true;
                    Intent uploadService=new Intent(UploadActivity.this, FTPService.class);
                    startService(uploadService);
                    transFile();
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
                } finally {
                    isUploading=false;
                    Intent uploadService=new Intent(UploadActivity.this, FTPService.class);
                    stopService(uploadService);
                }
            }
        }).start();
    }

    private void transFile() throws IllegalStateException,
        FileNotFoundException, IOException,
        FTPIllegalReplyException, FTPException,
        FTPDataTransferException, FTPAbortedException {

        //mFtpClient.upload(new File("k:\\非我莫属.f4v"),listener);
        /*InputStream inputStream=new FileInputStream(mFilePath);
        mFtpClient.upload(mFilePath, inputStream, 0, 0, listener);*/
        File file=new File(mFilePath);
        if (file.exists()) {
            mFtpClient.upload(file, listener);
        } else {
            Log.d(TAG, "file is not exists.");
        }
    }
}
