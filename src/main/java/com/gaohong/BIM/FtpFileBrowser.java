package com.gaohong.BIM;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;


/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-4
 */
public class FtpFileBrowser extends BaseActivity {

    public static final String TAG="FtpFileBrowser";
    public static  String currFile="";
    FTPClient mFtpClient;
    private FTPUtils.ServiceToken mToken;
    private FTPService mService=null;
    ListView mListView;
    FTPFileAdapter mAdapter;
    ArrayList<FTPFile> mFileList;
    TextView mCurrPath;
    TextView mUploadTxt;
    String mUploadFilepath;
    Button mUploadBtn, mChooseBtn, mStopBtn,mBackBtn;
    ProgressBar mProgressBar;
    int mode=MODE_BROWSER;
    public static final int MODE_CHOOSE=1;
    public static final int MODE_BROWSER=2;
    //--------------------
    private static final int LOCAL_BROWSER=0x100;
    //--------------------
    public static final int TYPE_NORMAL=0;
    public static final int TYPE_VIDEO=1;
    public static final int TYPE_IMAGE=2;
    int type=TYPE_IMAGE;

    public FtpFileBrowser() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent=getIntent();
                mode=intent.getIntExtra("mode", MODE_BROWSER);
                type=intent.getIntExtra("type", TYPE_IMAGE);
                queryMedia(intent);
            }
        }).start();



    }

    private void queryMedia(Intent intent) {
        Uri uri=intent.getData();
        Log.d(TAG, "uri:"+uri+" type:"+TAG);

        if (type==TYPE_IMAGE) {
            String filepath=intent.getStringExtra("path");
            Log.d(TAG, "filepath:" + filepath);
            setUploadFile(filepath);
        } else if (type==TYPE_VIDEO) {
            if (null!=uri) {
                Cursor cursor=null;
                try {
                    String projection=MediaStore.Images.Media.DATA;

                    cursor=getContentResolver().query(uri, new String[]{projection}, null, null, null);
                    if (null!=cursor) {
                        cursor.moveToFirst();
                        String path=cursor.getString(cursor.getColumnIndexOrThrow(projection));
                        Log.d(TAG, "path:"+path);
                        setUploadFile(path);
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } finally {
                    if (null!=cursor) {
                        cursor.close();
                    }
                }
            }
        }
    }

    private void init() {
        setContentView(R.layout.server_browser);
        mListView=(ListView) findViewById(R.id.files);
        mCurrPath=(TextView) findViewById(R.id.path);
        mUploadTxt=(TextView) findViewById(R.id.upload_txt);
        mUploadBtn=(Button) findViewById(R.id.upload_btn);
        mChooseBtn=(Button) findViewById(R.id.choose_btn);
        mStopBtn=(Button) findViewById(R.id.stop_btn);
        mBackBtn=(Button) findViewById(R.id.back_btn);
        mProgressBar=(ProgressBar) findViewById(R.id.progress_bar);

        mUploadBtn.setOnClickListener(clickListener);
        mChooseBtn.setOnClickListener(clickListener);
        mStopBtn.setOnClickListener(clickListener);
        mBackBtn.setOnClickListener(clickListener);
        
        mFileList=new ArrayList<FTPFile>();
        mAdapter=new FTPFileAdapter(FtpFileBrowser.this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(itemClickListener);
        //mListView.setOnItemLongClickListener(itemLongClickListener);
    }

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
            mFtpClient=mService.mFtpClient;
            FtpUser user=mService.mFtpUser;
            if (null==user) {
                Toast.makeText(FtpFileBrowser.this, "you should login first!", Toast.LENGTH_SHORT).show();
                Intent intent=new Intent(FtpFileBrowser.this, LoginActivity.class);
                startActivity(intent);
                FtpFileBrowser.this.finish();
                return;
            }


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                        refreshFiles(mFtpClient);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (FTPIllegalReplyException e) {
                            e.printStackTrace();
                        } catch (FTPException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


        }

        public void onServiceDisconnected(ComponentName classname) {
            Log.d(TAG, "onServiceDisconnected.mService:"+mService);
            mService=null;
        }
    };

    private static final int REFRESH_FILE=1;
    private static final int CLOSE_DIALOG=2;
    private static final int QUIT=3;
    private final Handler mHandler=new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_FILE:
                    String currDir=(String) msg.obj;
                    mCurrPath.setText(getString(R.string.curr_path)+currDir);
                    currFile = currDir;
                    mAdapter.notifyDataSetChanged();
                    break;

                case CLOSE_DIALOG:
                    closeProgressDialog();
                    break;

                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(FtpFileBrowser.this)
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

                default:
                    break;
            }
        }
    };

    //--------------------------
    AdapterView.OnItemClickListener itemClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            FTPFile ftpFile=mFileList.get(i);
            final String filename=ftpFile.getName();
            if ("..".equals(filename)) {
                Log.d(TAG, "change directory up.");
                changeDirectoryUp();
            } else if (".".equals(filename)) {
                Log.d(TAG, "change directory root");
                changeDirectory("/");
            } else {
                if (ftpFile.getType()==FTPFile.TYPE_DIRECTORY) {
                    Log.d(TAG, "change directory:"+filename);
                    changeDirectory(filename);
                }
                 else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(FtpFileBrowser.this);
                    dialog.setTitle("确认对话框");
                    dialog.setMessage("是否下载" + filename + "？");
                   // dialog.setPositiveButton("确认", null);
                   // dialog.setNegativeButton("取消", null);

                    dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                         //   Toast.makeText(FtpFileBrowser.this, "开始下载文件" + which, Toast.LENGTH_SHORT).show();
                            Toast.makeText(FtpFileBrowser.this,currFile+"/"+filename, Toast.LENGTH_SHORT).show();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Download(filename);
                                }
                            }).start();
                        }
                    });
                    //    设置一个NegativeButton
                    dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();


                }
            }
        }
    };

    AdapterView.OnItemLongClickListener itemLongClickListener=new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            FTPFile ftpFile=mFileList.get(i);
            String filename=ftpFile.getName();
            if ("..".equals(filename)||".".equals(filename)) {
                Log.d(TAG, ".|.. can't be a upload dir.");
            } else {
                if (ftpFile.getType()==FTPFile.TYPE_DIRECTORY) {
                    Log.d(TAG, "select directory:"+filename);
                    selectDirectory(filename);
                }
                //下载
            }

            return false;
        }
    };

    /**
     * 选择目录，提供一些目录的操作，暂时无用，对目录选择文件上传时使用其它按钮操作。
     *
     * @param filename 目录名称，相对路径
     */
    private void selectDirectory(String filename) {

    }

    /**
     * 刷新当前的ftp服务器文件列表
     *
     * @param mFtpClient
     * @throws IOException
     * @throws FTPIllegalReplyException
     * @throws FTPException
     */
    private void refreshFiles(FTPClient mFtpClient) throws IOException, FTPIllegalReplyException, FTPException {
        FTPFile[] files=new FTPFile[0];
        try {
            files=mFtpClient.list();
            List<FTPFile> ftpFiles=(List<FTPFile>) Arrays.asList(files);
            mFileList.clear();
            mFileList.addAll(ftpFiles);
            Message message=Message.obtain(mHandler, REFRESH_FILE);
            message.obj=mFtpClient.currentDirectory();
            mHandler.sendMessage(message);
        } catch (FTPDataTransferException e) {
            e.printStackTrace();
        } catch (FTPAbortedException e) {
            e.printStackTrace();
        } catch (FTPListParseException e) {
            e.printStackTrace();
        }
    }

    private void closeDialog() {
        Message message=Message.obtain();
        message.what=CLOSE_DIALOG;
        mHandler.sendMessage(message);
    }

    /**
     * 改变当前显示的目录内容
     *
     * @param path 服务器的相对路径目录名
     */
    private void changeDirectory(final String path) {
        if (mService==null) {
            Log.d(TAG, "mService==null");
            return;
        }

        showProgressDialog("", "");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mFtpClient=mService.getFtpClient();
                if (null!=mFtpClient) {
                    try {
                        mFtpClient.changeDirectory(path);
                        refreshFiles(mFtpClient);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (FTPIllegalReplyException e) {
                        e.printStackTrace();
                    } catch (FTPException e) {
                        e.printStackTrace();
                    } finally {
                        closeDialog();
                    }
                } else {
                    closeDialog();
                }
            }
        }).start();
    }

    /**
     * 切换到当前目录的上层
     */
    private void changeDirectoryUp() {
        if (mService==null) {
            Log.d(TAG, "mService==null");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                mFtpClient=mService.getFtpClient();
                if (null!=mFtpClient) {
                    try {
                        if ("/".equals(mFtpClient.currentDirectory())) {
                            Log.d(TAG, "current dir is root dir.");
                        } else {
                            mFtpClient.changeDirectoryUp();
                            refreshFiles(mFtpClient);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (FTPIllegalReplyException e) {
                        e.printStackTrace();
                    } catch (FTPException e) {
                        e.printStackTrace();
                    } finally {
                        closeDialog();
                    }
                } else {
                    closeDialog();
                }
            }
        }).start();
    }

    class FTPFileAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        Context ctx;

        public FTPFileAdapter(Context context) {
            ctx=context;
            mInflater=LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mFileList.size();
        }

        @Override
        public Object getItem(int i) {
            return mFileList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemHolder holder=null;
            if (convertView==null) {
                convertView=mInflater.inflate(R.layout.serverbrowseritem, null);
                holder=new ItemHolder();

                holder.filename=(TextView) convertView.findViewById(R.id.text);
                holder.fileicon=(ImageView) convertView.findViewById(R.id.folder);
                holder.filesize=(TextView) convertView.findViewById(R.id.file_size);
                holder.fileicon.setVisibility(View.VISIBLE);
                convertView.setTag(holder);
            } else {
                holder=(ItemHolder) convertView.getTag();
            }

            FTPFile file=mFileList.get(position);
            holder.filename.setText(file.getName());
            holder.filesize.setText(FTPUtils.getFileSize(file.getSize()));
            if (file.getType()==FTPFile.TYPE_DIRECTORY) {  //判断文件类型
                holder.fileicon.setImageResource(R.mipmap.dir);
            } else {
            	holder.fileicon.setImageResource(R.mipmap.file);
            }

            return convertView;
        }

        class ItemHolder {

            TextView filename;
            ImageView fileicon;
            TextView filesize;
        }
    }

    //--------------------------
    View.OnClickListener clickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id=view.getId();
            switch (id) {
                case R.id.upload_btn:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            upload();
                        }
                    }).start();

                    break;

                case R.id.choose_btn:
                    chooseLocalFile();
                    break;

                case R.id.stop_btn:
                    stopUpload();
                    break;
                case R.id.back_btn:
                    back();
                    break;
            }
        }
    };
    Handler mNotifyHandler=new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what=msg.what;
            Integer progress=(Integer) msg.obj;
            switch (what) {
                case 1:
                    //Log.d(TAG, "setProgress(progress)"+progress);
                    mProgressBar.setProgress(progress);
                    break;

                case 2:
                    Log.d(TAG, "setMax(progress)"+progress);
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setMax(progress);
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
    TransferNotifyListener mNotifyListener=new TransferNotifyListener() {
        @Override
        public void started() {
        }

        @Override
        public void started(long totalSize) {
            Message message=Message.obtain();
            message.what=2;
            message.obj=(int) totalSize;
            mNotifyHandler.sendMessage(message);
        }

        @Override
        public void transferred(int length) {
        }

        @Override
        public void transferred(long length) {
            Message message=Message.obtain();
            message.what=1;
            message.obj=(int) length;
            mNotifyHandler.sendMessage(message);
        }

        @Override
        public void completed() {
            clearNotify();
        }

        @Override
        public void aborted() {
            clearNotify();
        }

        @Override
        public void failed(String msg) {
            Log.e(TAG, "failed:"+msg);
            clearNotify();
        }
    };

    private void clearNotify() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mUploadTxt.setText(null);
                mUploadFilepath=null;
                mUploadTxt.setText(null);
            }
        });

        mService.setNotifyListener(null);
        //TODO 如果不一样的目录，不需要刷新。
        try {
            refreshFiles(mFtpClient);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FTPIllegalReplyException e) {
            e.printStackTrace();
        } catch (FTPException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传文件，需要先选择文件，而且当前没有其它文件在上传
     */
    private void upload() {
        if (TextUtils.isEmpty(mUploadFilepath)) {
            Log.d(TAG, "upload.mUploadFilepath is null");
            // Toast.makeText(FtpFileBrowser.this, "you should choose a file first!", Toast.LENGTH_SHORT).show();
            return;
        }

        FTPClient ftpClient=mService.mFtpClient;
        if (ftpClient==null) {
            Log.d(TAG, "upload.ftpClient==null");
            Toast.makeText(FtpFileBrowser.this, "you should login first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ftpClient.isConnected()) {
            try {
                Log.d(TAG, "re login.");
                mService.login();
            } catch (FTPException e) {
                e.printStackTrace();
            }
        }

        if (mService.mTransState==TransState.TRANSING) {
            Toast.makeText(FtpFileBrowser.this, "another file is being transforming.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasSameNameFile()) {
            Toast.makeText(FtpFileBrowser.this, "server has a same name file,now overwrite it.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "server has a same name file,now overwrite it.");
        }

        ftpClient=mService.mFtpClient;
        mService.setFile(mUploadFilepath);
        mService.setNotifyListener(mNotifyListener);
        mService.preTransFile();
    }

    /**
     * 判断服务器是否有相同的文件名的文件
     * 如果有相同的文件，需要提示覆盖，默认是直接覆盖不提示
     *
     * @return true表示有，false表示无
     */
    private boolean hasSameNameFile() {
        File file=new File(mUploadFilepath);
        for (FTPFile ftpFile : mFileList) {
            if (ftpFile.getName().equals(file.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 选择本地文件，选择后将覆盖之前选择的文件
     */
    private void chooseLocalFile() {
        Intent intent=new Intent(FtpFileBrowser.this, FileActivity.class);
        startActivityForResult(intent, LOCAL_BROWSER);
    }
    private void back() {
    	changeDirectoryUp();
    	  
    }
    
    /**
     * 停止上传
     */
    private void stopUpload() {
        FTPClient ftpClient=mService.mFtpClient;
        if (ftpClient==null) {
            Log.d(TAG, "stopUpload.ftpClient==null");
            return;
        }

        if (!ftpClient.isConnected()) {
            Log.d(TAG, "ftpClient.isConnected");
        }

        Log.d(TAG, "abort pre trans:" + mService.mTransState);
        if (mService.mTransState==TransState.TRANSING) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    mService.abort();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode:" + requestCode + " resultCode:" + resultCode + " data:" + data);
        if (resultCode==RESULT_OK) {
            if (requestCode==LOCAL_BROWSER) {
                String file_path=data.getStringExtra("file_path");
                if (!TextUtils.isEmpty(file_path)) {
                    setUploadFile(file_path);
                }
            }
        }
    }

    private void setUploadFile(String file_path) {
        mUploadFilepath=file_path;
        mUploadTxt.setText(file_path);
        mProgressBar.setProgress(0);
    }

    private void Download(String filename){
        FTPClient ftpClient=mService.mFtpClient;
        if (ftpClient==null) {
            Log.d(TAG, "download.ftpClient==null");
            Toast.makeText(FtpFileBrowser.this, "you should login first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ftpClient.isConnected()) {
            try {
                Log.d(TAG, "re login.");
                mService.login();
            } catch (FTPException e) {
                e.printStackTrace();
            }
        }

        if (mService.mTransState==TransState.TRANSING) {
            Toast.makeText(FtpFileBrowser.this, "another file is being transforming.", Toast.LENGTH_SHORT).show();
            return;
        }

//        if (hasSameNameFile()) {
//            //Toast.makeText(FtpFileBrowser.this, "server has a same name file,now overwrite it.", Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "server has a same name file,now overwrite it.");
//        }
        ftpClient=mService.mFtpClient;
        //File localDir =new java.io.File("/mnt/sdcard/BIMdownload/");

        //Toast.makeText(FtpFileBrowser.this, currFile+"/"+filename, Toast.LENGTH_SHORT).show();
        File localDir = null;

        try {
            createDir("xiaoyunpai");
            localDir = createFile("xiaoyunpai" + "/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ////Toast.makeText(FtpFileBrowser.this, currFile+"/"+filename, Toast.LENGTH_SHORT).show();
        try {

            //ftpClient.download(currFile+"/"+filename,localDir);
            ftpClient.download(filename, localDir);

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
        }


        mService.setNotifyListener(mNotifyListener);
        mService.showNotification_download(filename);


    }



    public boolean checkFileExists(String filepath) {
        String SDPATH=Environment.getExternalStorageDirectory()+"/";
        File file=new File(SDPATH+filepath);
        return file.exists();
    }

    /*
    * 在SD卡上创建目录；
    */
    public File createFile(String dirpath) throws IOException{
        String SDPATH= Environment.getExternalStorageDirectory()+"/";
        checkFileExists(SDPATH+dirpath);
        File dir=new File(SDPATH+dirpath);
        //dir.mkdir();
        dir.createNewFile();
        return dir;
    }

    public File createDir(String dirpath) throws IOException{
        String SDPATH= Environment.getExternalStorageDirectory()+"/";
        checkFileExists(SDPATH+dirpath);
        File dir=new File(SDPATH+dirpath);
        dir.mkdir();
        return dir;
    }

}
