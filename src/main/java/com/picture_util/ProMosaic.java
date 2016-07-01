package com.picture_util;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.R;
import com.gaohong.BIM.FtpFileBrowser;
import com.picture_util.MosaicView.Effect;
import com.picture_util.MosaicView.Mode;
import com.picture_util.PopMenuList.ListMenuListener;
import com.picture_util.PopMenuList.MenuItem;

import java.util.LinkedList;
import java.util.List;

public class ProMosaic extends Activity {
	public static final String TAG = "ProMosaic";
	private static final int REQ_PICK_IMAGE = 1984;


	public static final int TYPE_IMAGE=2;
	private static final int LOGIN_SUC=4;


	private MosaicView mvImage;

	private Button btClear;
	private Button btLoad;
	private Button btSave;
	private Button btEffect;
	private Button btMode;
	private Button btUpload;
	private Button btErase;

	private PopMenuList effectList;
	private PopMenuList modeList;

	private static  Bundle filePath_bundle;
	private static String fileOutPath;

//	private FTPService mService=null; //gai 20160330
//	FtpUser mFtpUser;
//	FTPClient mFtpClient;

//	private final Handler mHandler=new Handler();
//	private FTPUtils.ServiceToken mToken;

	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_pro_mosaic);

		mvImage = (MosaicView) findViewById(R.id.iv_content);

		btLoad = (Button) findViewById(R.id.bt_load);
		btClear = (Button) findViewById(R.id.bt_clear);
		btSave = (Button) findViewById(R.id.bt_save);
		//btUpload = (Button) findViewById(R.id.bt_upload);
		btUpload = (Button) findViewById(R.id.bt_upload);
		btMode = (Button) findViewById(R.id.bt_mode);
		btEffect = (Button) findViewById(R.id.bt_effect);
		btErase = (Button) findViewById(R.id.bt_erase);
		btLoad.setOnClickListener(cl);
		btClear.setOnClickListener(cl);
		btSave.setOnClickListener(cl);
		btUpload.setOnClickListener(cl);
		btEffect.setOnClickListener(cl);
		btMode.setOnClickListener(cl);
		btErase.setOnClickListener(cl);

		Intent intent=getIntent();
		filePath_bundle=intent.getExtras();
		String filePath = filePath_bundle.getString("path");

//		Uri selectedImage = data.getData(); //xuan ze zhaopian
//		Log.d(TAG, selectedImage.toString());
//		String[] filePathColumn = { MediaStore.Images.Media.DATA };
//
//		Cursor cursor = getContentResolver().query(selectedImage,
//				filePathColumn, null, null, null);
//		cursor.moveToFirst();
//
//		int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//		String filePath = cursor.getString(columnIndex);
//		cursor.close();

		mvImage.setSrcPath(filePath);
//		Intent FTPintent = new Intent(ProMosaic.this, FTPService.class);
//		bindService(FTPintent, conn, Context.BIND_AUTO_CREATE);
	}

//	public ServiceConnection conn = new ServiceConnection() {
//		@Override
//		public void onServiceConnected(ComponentName name, IBinder service) {
//			mService=((FTPService.MyBinder) service).getService();
//			Log.d(TAG, "onServiceConnected.mService:"+mService);
//			//ignoreLogin();//原有
//			login();
//		}
//
//		@Override
//		public void onServiceDisconnected(ComponentName name) {
//
//		}
//	};
//
//
//	@Override
//	public void onStart() {
//		super.onStart();
//		//mToken=FTPUtils.bindToService(this, conn);
//	}
//
//	@Override
//	public void onStop() {
//		//FTPUtils.unbindFromService(mToken);
//		mService=null;
//		super.onStop();
//	}



	@Override
	protected void onDestroy() {
//		FTPUtils.unbindFromService(mToken);
		super.onDestroy();
		mvImage.clear();
//		RadioGroup myTabRg;
//		myTabRg = (RadioGroup) findViewById(R.id.tab_menu);
//		myTabRg.check(R.id.project);
	}

	private OnClickListener cl = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (view.equals(btLoad)) {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_PICK);
				String title = getResources().getString(R.string.choose_image);
				Intent chooser = Intent.createChooser(intent, title);
				startActivityForResult(chooser, REQ_PICK_IMAGE);
			} else if (view.equals(btClear)) {
				mvImage.clear();
				mvImage.setErase(false);
			} else if (view.equals(btSave)) {
				 fileOutPath = mvImage.save();
				String text = " 图片保存"
						+ (fileOutPath.equals("false") ? " 失败" : " 成功");
				Toast.makeText(view.getContext(), fileOutPath +text, Toast.LENGTH_SHORT)
						.show();
			} else if (view.equals(btEffect)) {
				initEffectList();
				effectList.show(btEffect);
			} else if (view.equals(btMode)) {
				initModeList();
				modeList.show(btMode);
			} else if (view.equals(btUpload)) {
//				Intent intent = new Intent(ProMosaic.this, VideoCapture.class);
//				startActivity(intent);
				//Intent intent=new Intent(ProMosaic.this, FtpFileBrowser.class);
				//intent.putExtra("type", type);
				//intent.putExtra("fileOutPath", fileOutPath);
				//startActivity(intent);

				fileOutPath = mvImage.save();
				String text = " 图片保存"
						+ (fileOutPath.equals("false") ? " 失败" : " 成功");
				Toast.makeText(view.getContext(), fileOutPath +text, Toast.LENGTH_SHORT)
						.show();
				uploadMedia(fileOutPath,TYPE_IMAGE);

			} else if (view.equals(btErase)) {
				mvImage.setErase(true);
			}
		}
	};

	private void initEffectList() {
		if (effectList != null) {
			return;
		}
		effectList = new PopMenuList(this);
		List<MenuItem> items = new LinkedList<MenuItem>();
		items.add(new MenuItem(null, getResources().getString(
				R.string.effect_grid)));
		items.add(new MenuItem(null, getResources().getString(
				R.string.effect_blur)));
		items.add(new MenuItem(null, getResources().getString(
				R.string.effect_color)));
		effectList.setItems(items);
		effectList.setListMenuListener(el);
	}

	private ListMenuListener el = new ListMenuListener() {

		@Override
		public void onItemClick(int index) {
			if (index == 0) {
				mvImage.setEffect(Effect.GRID);
			} else if (index == 1) {
				mvImage.setEffect(Effect.BLUR);
			} else if (index == 2) {
				mvImage.setMosaicColor(0xff000000);
				mvImage.setEffect(Effect.COLOR);
			}
		}
	};

	private void initModeList() {
		if (modeList != null) {
			return;
		}
		modeList = new PopMenuList(this);
		List<MenuItem> items = new LinkedList<MenuItem>();
		items.add(new MenuItem(null, getResources().getString(
				R.string.mode_path)));
		items.add(new MenuItem(null, getResources().getString(
				R.string.mode_grid)));
		modeList.setItems(items);
		modeList.setListMenuListener(ml);
	}

	private ListMenuListener ml = new ListMenuListener() {

		@Override
		public void onItemClick(int index) {
			if (index == 0) {
				mvImage.setMode(Mode.PATH);
			} else if (index == 1) {
				mvImage.setMode(Mode.GRID);
			}
		}
	};

	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		// user cancelled
		if (resultCode != Activity.RESULT_OK) {
			Log.d(TAG, "user cancelled");
			return;
		}

		if (reqCode == REQ_PICK_IMAGE) {
			Uri selectedImage = data.getData(); //xuan ze zhaopian
			Log.d(TAG, selectedImage.toString());
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String filePath = cursor.getString(columnIndex);
			cursor.close();
			mvImage.setSrcPath(filePath);   //gai
		}
	}


	private void uploadMedia(String filepath, int type) {

		//login();
		Intent intent=new Intent(ProMosaic.this, FtpFileBrowser.class); //20160329gai  paizhao  zhunbei shangchuan
		intent.putExtra("type", type);
		intent.putExtra("path", filepath);
		//intent.putExtra("LOGIN_START", 2);
		startActivity(intent);
	}



//	private void login() {
//		try {
//			String host="182.92.107.145";
//			String user="zttftp";
//			String pass="123456";
//			int port=21;
//			mService.setFtpUser(host, port, user, pass);
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						FTPClient ftpClient = new FTPClient();
//						ftpClient.connect(mFtpUser.host, mFtpUser.port);
//						ftpClient.login(mFtpUser.username, mFtpUser.password, null);
//
//					} catch (FTPException e) {
//						e.printStackTrace();
//					}catch (IOException e){
//						e.printStackTrace();
//					} catch (FTPIllegalReplyException e) {
//					e.printStackTrace();
//				}
//			}
//		}).start();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}



}
