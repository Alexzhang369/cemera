package com.gaohong.BIM;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.R;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * @version 1.00.00
 * @description:
 * @author: archko 12-6-1
 */
public class VideoCapture2 extends Activity {

    public static final String TAG="VideoCapture";
    private static final int ICON_SIZE=96;
    private static final int PHOTO_PICKED_WITH_DATA=3021;
    private static final int CAMERA_WITH_DATA=3023;
    private static final File PHOTO_DIR=new File(
        Environment.getExternalStorageDirectory()+"/DCIM/Camera");

    private File mCurrentPhotoFile;
    ImageView imageView;

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

            // Add the image to the media store
            MediaScannerConnection.scanFile(
                this,
                new String[]{f.getAbsolutePath()},
                new String[]{null},
                null);

            imageView.setImageBitmap(BitmapFactory.decodeStream(new FileInputStream(f)));
            // Launch gallery to crop the photo
            final Intent intent=getCropImageIntent(Uri.fromFile(f));
            startActivityForResult(intent, PHOTO_PICKED_WITH_DATA);
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
        imageView=(ImageView) findViewById(R.id.iv);
        doTakePhoto();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode:"+requestCode+" resultCode:"+resultCode+" data:"+data);
        if (resultCode!=RESULT_OK) return;

        switch (requestCode) {
            case PHOTO_PICKED_WITH_DATA: {
                final Bitmap photo=data.getParcelableExtra("data");
                Log.d(TAG, "photo:"+photo);
                break;
            }

            case CAMERA_WITH_DATA: {
                Log.d(TAG, "path:"+mCurrentPhotoFile.getAbsolutePath());
                doCropPhoto(mCurrentPhotoFile);
                break;
            }
        }
    }
}
