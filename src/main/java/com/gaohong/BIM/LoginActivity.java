package com.gaohong.BIM;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.R;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-1
 */
public class LoginActivity extends BaseActivity {

    public static final String TAG="LoginActivity";
    TextView title;
    Button loginBtn;
    EditText usernameET, passwordET, portET, hostET;
    private FTPUtils.ServiceToken mToken;
    private FTPService mService=null;
    private static final int LOGIN_FAILED=1;
    private static final int CLOSE_DIALOG=2;
    private static final int QUIT=3;
    private static final int LOGIN_SUC=4;
    public static final int TYPE_IMAGE=2;

    //---------------
    //int loginType=LOGIN_START;
    int loginType=Splash_LOGIN_START;
    public static final int LOGIN_START=0;
    public static final int LOGIN_CALLBACK=1;
    public static final int LOGIN_UPLOADCALLBACK=2;
    public static final int Splash_LOGIN_START=3;
    private static  Bundle fileOutPath_bundle;

    private final Handler mHandler=new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOGIN_FAILED:
                    Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                    break;

                case CLOSE_DIALOG:
                    closeProgressDialog();
                    break;

                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(LoginActivity.this)
                        .setTitle(R.string.service_start_error_title)
                        .setMessage(R.string.service_start_error_msg)
                        .setPositiveButton(R.string.service_start_error_button,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            })
                        .setCancelable(false)
                        .show();
                    break;

                case LOGIN_SUC:
                    if (loginType==LOGIN_START) {
//                        Intent intent_fileOutPath=getIntent();
//                         fileOutPath_bundle=intent_fileOutPath.getExtras();
//                        String fileOutPath =fileOutPath_bundle.getString("fileOutPath");
                        Intent intent=new Intent(LoginActivity.this, VideoCapture.class);
//                        intent.putExtra("type", TYPE_IMAGE);
//                        intent.putExtra("path", fileOutPath);
                        startActivity(intent);
                        LoginActivity.this.finish();

//                        Intent intent=new Intent(LoginActivity.this, FtpFileBrowser.class);
////                        intent.putExtra("type", TYPE_IMAGE);
////                        intent.putExtra("path", fileOutPath);
//                        startActivity(intent);
//                        LoginActivity.this.finish();

                    } else if (loginType==LOGIN_CALLBACK) {
                        setResult(RESULT_OK);
                        LoginActivity.this.finish();
                    }else if (loginType==LOGIN_UPLOADCALLBACK) {
                        setResult(RESULT_OK);
                        LoginActivity.this.finish();
                    }else if (loginType==Splash_LOGIN_START) {
//                        Intent intent_fileOutPath=getIntent();
//                         fileOutPath_bundle=intent_fileOutPath.getExtras();
//                        String fileOutPath =fileOutPath_bundle.getString("fileOutPath");
                    Intent intent=new Intent(LoginActivity.this, VideoCapture.class);
//                        intent.putExtra("type", TYPE_IMAGE);
//                        intent.putExtra("path", fileOutPath);
                    startActivity(intent);
                    LoginActivity.this.finish();

                }

                    break;

                default:
                    break;
            }
        }
    };

    View.OnClickListener clickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id=view.getId();
            switch (id) {
                case R.id.login_btn:
                    login();
                    break;
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        mToken=FTPUtils.bindToService(this, osc);
        if (mToken==null) {
            // something went wrong
            mHandler.sendEmptyMessage(QUIT);
        }
    }

    @Override
    public void onStop() {
        FTPUtils.unbindFromService(mToken);
        mService=null;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        FTPUtils.unbindFromService(mToken);
        super.onDestroy();
    }

    private ServiceConnection osc=new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService=((FTPService.MyBinder) obj).getService();
            Log.d(TAG, "onServiceConnected.mService:"+mService);
            ignoreLogin();//原有
            login();
        }

        public void onServiceDisconnected(ComponentName classname) {
            Log.d(TAG, "onServiceDisconnected.mService:" + mService);
            //ignoreLogin();//2
            login();
            mService=null;
        }
    };

    private void ignoreLogin() {
        FTPClient client=mService.mFtpClient;
        FtpUser user=mService.mFtpUser;
        if (null!=client&&null!=user) {
            if (!client.isConnected()) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            mService.login();
                        } catch (FTPException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            Message message=Message.obtain();
            message.what=LOGIN_SUC;
            mHandler.sendMessage(message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        Intent intent=getIntent();
        loginType=intent.getIntExtra("type", LOGIN_START);

        title=(TextView) findViewById(R.id.title);
        usernameET=(EditText) findViewById(R.id.username);
        passwordET=(EditText) findViewById(R.id.password);
        portET=(EditText) findViewById(R.id.port);
        hostET=(EditText) findViewById(R.id.host);
        loginBtn=(Button) findViewById(R.id.login_btn);

        loginBtn.setOnClickListener(clickListener);
        Toast.makeText(getApplicationContext(), "请保证网络连接畅通，如已打开网络，请忽略",
                Toast.LENGTH_SHORT).show();
        //ignoreLogin();//1

    }

    private void login() {
        try {
            String host=hostET.getText().toString();
            String user=usernameET.getText().toString();
            String pass=passwordET.getText().toString();
            if (TextUtils.isEmpty(host)||TextUtils.isEmpty(user)||TextUtils.isEmpty(pass)||
                TextUtils.isEmpty(portET.getText().toString())) {
                Toast.makeText(LoginActivity.this, "信息不完整,无法登录!", Toast.LENGTH_SHORT).show();
                return;
            }

            int port=Integer.valueOf(portET.getText().toString());
            mService.setFtpUser(host, port, user, pass);

            showProgressDialog(getString(R.string.dialog_title), getString(R.string.dialog_msg));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FTPClient ftpClient=mService.login();
                        if (null!=ftpClient) {
                            Log.d(TAG, "登录成功");
                            Message message=Message.obtain();
                            message.what=LOGIN_SUC;
                            mHandler.sendMessage(message);
                        }
                    } catch (FTPException e) {
                        e.printStackTrace();
                        int code=e.getCode();
                        if (code==530) {
                            Log.e(TAG, "用户名或密码错误");

                            Message message=Message.obtain();
                            message.what=LOGIN_FAILED;
                            mHandler.sendMessage(message);
                        }
                    } finally {
                        Message message=Message.obtain();
                        message.what=CLOSE_DIALOG;
                        mHandler.sendMessage(message);
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
