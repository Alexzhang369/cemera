package com.gaohong.BIM;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.R;
import com.picture_util.MosaicView;
import com.picture_util.ProMosaic;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-1
 */
public class VideoCapture extends Activity {

    public static final String TAG="VideoCapture";
    public static final int VIDEO_CAMERA=0x100;
    public static final int VIDEO_CAMERA2=0x101;
    public static final int IMAGE_CAMERA=0x102;
    public static final int IMAGE_CAMERA2=0x103;
    private MosaicView mvImage;
    Button capture1Btn, capture2Btn;
    Button capture3Btn, capture4Btn,btn_yun;
    View.OnClickListener clickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id=view.getId();
            switch (id) {
                case R.id.capture1:
                    capture1();
                    break;

                case R.id.capture2:
                    capture2();
                    break;

                case R.id.capture3:
                    //capture3();
                    break;

                case R.id.capture4:
                    capture4();
                    break;
            }
        }
    };

    private void capture1() {
        /*Intent intent=new Intent();
        intent.setAction("android.media.action.VIDEO_CAMERA");
        startActivityForResult(intent, VIDEO_CAMERA);*/
    }

    private void capture2() {
        Intent intent=new Intent(MediaStore.ACTION_VIDEO_CAPTURE, null);
        //Uri fileUri=getOutputMediaFileUri(".mp4"); // 创建保存视频的文件
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // 设置视频文件名
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        startActivityForResult(intent, VIDEO_CAMERA2);
    }

    private Uri getOutputMediaFileUri(String ext) {
        File imagesFolder=new File(Environment.getExternalStorageDirectory()+"/DCIM/Camera/");
        imagesFolder.mkdirs();
        File image=new File(getMediaFileName(ext));
        Uri uri=Uri.fromFile(image);
        Log.d(TAG, "imagesFolder:"+imagesFolder.getAbsolutePath()+" uri:"+uri);
        return uri;

        /*//这里我们插入一条数据，ContentValues是我们希望这条记录被创建时包含的数据信息
        //这些数据的名称已经作为常量在MediaStore.Images.Media中,有的存储在MediaStore.MediaColumn中了
        //ContentValues values = new ContentValues();
        String filename=getMediaFileName(".jpg");
        ContentValues values=new ContentValues(2);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        //values.put(MediaStore.Images.Media.DESCRIPTION, "this is description");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri imageFilePath=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFilePath); //这样就将文件的存储方式和uri指定到了Camera应用中
        Log.d(TAG, "filename:"+filename+" uri:"+imageFilePath);
        return imageFilePath;*/
    }

    private String getMediaFileName(String ext) {
        Date date=new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HHmmss");
        return dateFormat.format(date);
    }

    private void capture3() {
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        Uri fileUri=getOutputMediaFileUri(".jpg"); // 创建保存视频的文件
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // 设置视频文件名
        startActivityForResult(intent, IMAGE_CAMERA);
    }

    private void capture4() {
        doTakePhoto();
    }

    private static final int ICON_SIZE=96;
    private static final int CAMERA_WITH_DATA=3023;
    private static final File PHOTO_DIR=new File(
        Environment.getExternalStorageDirectory()+"/DCIM/Camera");

    private File mCurrentPhotoFile;

    private String getPhotoFileName() {
        Date date=new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat=new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return dateFormat.format(date)+".jpg";
    }

    /**
     * Launches Camera to take a picture and store it in a file.
     */
    protected void doTakePhoto() {
        try {
            // Launch camera to take photo for selected contact
            PHOTO_DIR.mkdirs();
            mCurrentPhotoFile=new File(PHOTO_DIR, getPhotoFileName());
            final Intent intent=getTakePickIntent(mCurrentPhotoFile);
            startActivityForResult(intent, CAMERA_WITH_DATA);
            //VideoCapture.this.finish();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "R.string.photoPickerNotFoundText", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Constructs an intent for capturing a photo and storing it in a temporary file.
     */
    public static Intent getTakePickIntent(File f) {
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        return intent;
    }

    /**
     * Sends a newly acquired photo to Gallery for cropping
     */
    protected void doCropPhoto(File f) {
        try {
            String filepath=f.getAbsolutePath();
            // Add the image to the media store
            /*MediaScannerConnection.scanFile(
                this,
                new String[]{filepath},
                new String[]{null},
                null);*/

            // Launch gallery to crop the photo
            /*final Intent intent=getCropImageIntent(Uri.fromFile(f));
            startActivityForResult(intent, PHOTO_PICKED_WITH_DATA);*/
            uploadMedia(filepath, FtpFileBrowser.TYPE_IMAGE);
        } catch (Exception e) {
            Log.e(TAG, "Cannot crop image", e);
            Toast.makeText(this, "R.string.photoPickerNotFoundText", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Constructs an intent for image cropping.
     */
    public static Intent getCropImageIntent(Uri photoUri) {
        Intent intent=new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", ICON_SIZE);
        intent.putExtra("outputY", ICON_SIZE);
        intent.putExtra("return-data", true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.capture);

        capture1Btn=(Button) findViewById(R.id.capture1);
        capture2Btn=(Button) findViewById(R.id.capture2);
        capture3Btn=(Button) findViewById(R.id.capture3);
        capture4Btn=(Button) findViewById(R.id.capture4);
        btn_yun=(Button)findViewById(R.id.btn_yun);


        capture1Btn.setOnClickListener(clickListener);
        capture2Btn.setOnClickListener(clickListener);
        capture3Btn.setOnClickListener(clickListener);
        capture4Btn.setOnClickListener(clickListener);

       // capture4();
        btn_yun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(VideoCapture.this, FtpFileBrowser.class);
                //intent.putExtra("type", type);
                //intent.putExtra("path", filePath);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode:"+requestCode+" resultCode:"+resultCode+" data:"+data);
        if (resultCode==RESULT_OK) {
            if (requestCode==VIDEO_CAMERA2) {
                Uri uri=data.getData();
                Log.d(TAG, "uri:"+uri);
                uploadMedia(uri, FtpFileBrowser.TYPE_VIDEO);
            } else if (requestCode==VIDEO_CAMERA) {
                Uri uri=data.getData();
                Log.d(TAG, "uri:"+uri);
            } else if (requestCode==IMAGE_CAMERA) {
                if (data!=null) {
                    Bundle extras=data.getExtras();
                    Bitmap myBitmap=(Bitmap) extras.get("data");
                    Log.d(TAG, myBitmap.toString());

                    Uri uri=data.getData();//gai
                    Log.d(TAG, "image uri:"+uri+""+data.getDataString());
                    uploadMedia(uri, FtpFileBrowser.TYPE_IMAGE);
                }
            } else if (requestCode==CAMERA_WITH_DATA) {
                Log.d(TAG, "path:"+mCurrentPhotoFile.getAbsolutePath());

//                Uri selectedImage = data.getData(); //xuan ze zhaopian  gai kaishi
//                Log.d(TAG, selectedImage.toString());
//                String[] filePathColumn = { MediaStore.Images.Media.DATA };
//
//                Cursor cursor = getContentResolver().query(selectedImage,
//                        filePathColumn, null, null, null);
//                cursor.moveToFirst();
//
//                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//                String filePath = cursor.getString(columnIndex);
//                cursor.close();
                String filePath = mCurrentPhotoFile.getAbsolutePath();
                //mvImage.setSrcPath(filePath);   //gai jieshu

                Intent intent=new Intent(VideoCapture.this, ProMosaic.class);
                //intent.putExtra("type", type);
                intent.putExtra("path", filePath);
                startActivity(intent);

               // doCropPhoto(mCurrentPhotoFile); // gai
            }
        }
    }

    /**
     * 传输视频
     *
     * @param uri  视频文件的uri
     * @param type 视频类型
     */
    private void uploadMedia(Uri uri, int type) {
        Intent intent=new Intent(VideoCapture.this, FtpFileBrowser.class);
        intent.putExtra("type", type);
        intent.setData(uri);
        startActivity(intent);
    }

    /**
     * 太恶心了，直接传文件路径
     *
     * @param filepath 文件路径
     * @param type     文件类型，这里是图片
     */
    private void uploadMedia(String filepath, int type) {
        Intent intent=new Intent(VideoCapture.this, ProMosaic.class);
        intent.putExtra("type", type);
        intent.putExtra("path", filepath);
        startActivity(intent);
    }
}
