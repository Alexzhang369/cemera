package com.gaohong.BIM;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;

public abstract class BaseActivity extends Activity {

    private ProgressDialog mProgressDialog;
    boolean init=false; //是否已经初始化了，如果结束了，但线程没有结束，无法创建对话框

    void showProgressDialog(String title, String msg) {
        if (null==mProgressDialog) {
            mProgressDialog=new ProgressDialog(BaseActivity.this);
        }
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    void showProgressDialog(String title, String msg, DialogInterface.OnCancelListener cancelListener) {
        if (null==mProgressDialog) {
            mProgressDialog=new ProgressDialog(BaseActivity.this);
        }
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        if (cancelListener!=null) {
            mProgressDialog.setOnCancelListener(cancelListener);
        }
        mProgressDialog.show();
    }

    void closeProgressDialog() {
        if (null!=mProgressDialog&&mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * 提示是否退出系统
     *
     * @param title   提示标题
     * @param message 提示内容
     * @param ok      确认
     * @param cancel  取消
     */
    void initDialog(int title, int message, int ok, int cancel) {
        if (!init) {
            Log.d("", "initDialog.init failed!");
            return;
        }
        new AlertDialog.Builder(BaseActivity.this).setTitle(title)
            .setMessage(message).setNegativeButton(cancel, null)
            .setPositiveButton(ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                }
            }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init=true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        init=false;
    }
}
