package com.viclee.customcamera;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

public class CameraPreviewActivity extends Activity implements OnClickListener,
        CameraPreviewView.OnCameraStatusListener {
    private static final int MAX_PIC_SIZE = 720;
    private static final int REQUEST_FOR_PICTURE = 1;

    private boolean flashOpen = false;

    private CameraPreviewView mCameraPreview;
    private ImageView takePicBtn;
    private ImageView flashSwtich;
    private ImageView shutterSwtich;

    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏

        setContentView(R.layout.activity_camera_preview);
        initView();
    }

    private void initView() {

        mCameraPreview = (CameraPreviewView) findViewById(R.id.camera_preview);
        mCameraPreview.setOnCameraStatusListener(this);
        mCameraPreview.setOnClickListener(this);

        takePicBtn = (ImageView) findViewById(R.id.take_picture);
        takePicBtn.setOnClickListener(this);

        findViewById(R.id.camera_switch).setOnClickListener(this);
        findViewById(R.id.camera_back).setOnClickListener(this);
        findViewById(R.id.camera_gallery).setOnClickListener(this);

        shutterSwtich = (ImageView) findViewById(R.id.camera_shutter);
        shutterSwtich.setOnClickListener(this);
        flashSwtich = (ImageView) findViewById(R.id.camera_flash);
        flashSwtich.setOnClickListener(this);

        if (!mCameraPreview.isSupportFlash()) {
            flashSwtich.setVisibility(View.GONE);
        }

        mCameraPreview.setCameraFront(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.camera_back:
            this.finish();
            break;
        case R.id.camera_preview:
            mCameraPreview.autoFocus();
            break;
        case R.id.camera_shutter:
            if (mCameraPreview.shutterFlag) {
                mCameraPreview.shutterFlag = false;
                shutterSwtich.setBackgroundResource(R.drawable.camera_shutter_close);
            } else {
                mCameraPreview.shutterFlag = true;
                shutterSwtich.setBackgroundResource(R.drawable.camera_shutter_open);
            }
            break;
        case R.id.camera_flash:
            mCameraPreview.changeFlashlight();
            if (flashOpen) {
                flashOpen = false;
                flashSwtich.setBackgroundResource(R.drawable.camera_flash_close);
            } else {
                flashOpen = true;
                flashSwtich.setBackgroundResource(R.drawable.camera_flash_open);
            }
            break;
        case R.id.camera_switch:
            mCameraPreview.setCameraFront(!mCameraPreview.isCameraFront);
            break;
        case R.id.camera_gallery:
            startGallery();
            break;
        case R.id.take_picture:
            if (mCameraPreview != null) {
                mCameraPreview.takePicture();
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void onCameraStopped(byte[] data) {
        new SaveBitmapTask().execute(data);
    }

    @Override
    public void onAutoFocus(boolean success) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FOR_PICTURE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumns={MediaStore.Images.Media.DATA};
            Cursor c = this.getContentResolver().query(selectedImage, filePathColumns, null,null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            String picturePath= c.getString(columnIndex);
            c.close();

            Intent intent = new Intent();
            intent.putExtra("image", picturePath);
            intent.setClass(this, ImageCropActivity.class);
            startActivity(intent);
        }
    }

    private void startGallery() {
        String galleryPackage = "com.android.gallery3d";
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        List<PackageInfo> pList = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < pList.size(); i++) {
            PackageInfo pI = pList.get(i);
            if (pI.packageName.equalsIgnoreCase(galleryPackage)) {
                intent.setPackage(galleryPackage);
            }
        }
        startActivityForResult(intent, REQUEST_FOR_PICTURE);
    }

    private void showLoadingDialog(String msg) {
        if (loadingDialog == null) {
            loadingDialog = ProgressDialog.show(this, null, msg, true, false);
            loadingDialog.setCanceledOnTouchOutside(false);
        } else {
            loadingDialog.setMessage(msg);
            if (!loadingDialog.isShowing()) {
                loadingDialog.show();
            }
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
    }

    // 由于拍照完成保存后，数据库不会立即更新，故需要调用下面的函数来更新多媒体数据库
    private void refreshMediaStore(String fileName) {
        MediaScannerConnection.scanFile(this, new String[]{fileName}, null, null);
    }

    class SaveBitmapTask extends AsyncTask<byte[], String, String> {

        @Override
        protected String doInBackground(byte[]... params) {
            if (params == null || params.length == 0) {
                return null;
            }
            byte[] bytes = (byte[]) params[0];

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            int width = opts.outWidth;
            int height = opts.outHeight;

            int inSampleSize = 1;
            if (width <= height) {
                if (height > MAX_PIC_SIZE) {
                    inSampleSize = height / MAX_PIC_SIZE;
                }
            } else {
                if (width > MAX_PIC_SIZE) {
                    inSampleSize = width / MAX_PIC_SIZE;
                }
            }

            opts.inSampleSize = inSampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inJustDecodeBounds = false;

            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

            Matrix matrix = new Matrix();
            if (mCameraPreview.isCameraFront) {
                matrix.postRotate(90.0f);
            } else {
                matrix.postRotate(270.0f);
            }

            Bitmap rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, false);
            if (rotateBitmap != bitmap && !bitmap.isRecycled()) {
                bitmap.recycle();
            }

            String path = BitmapUtils.saveBitmap(CameraPreviewActivity.this, rotateBitmap,
                    Bitmap.CompressFormat.JPEG, 100);
            if (rotateBitmap != null && !rotateBitmap.isRecycled()) {
                rotateBitmap.recycle();
            }
            return path;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoadingDialog("正在保存图片...");
        }

        @Override
        protected void onPostExecute(String result) {
            hideLoadingDialog();
            refreshMediaStore(result);
            if (!TextUtils.isEmpty(result)) {
                Toast.makeText(CameraPreviewActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CameraPreviewActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
            }
            // if (mCameraPreview.isCameraFront) {
            // mCameraPreview.setCameraFront(true);
            // } else {
            // mCameraPreview.setCameraFront(false);
            // }
            Intent intent = new Intent();
            intent.putExtra("image", result);
            intent.setClass(CameraPreviewActivity.this, ImageCropActivity.class);
            startActivity(intent);
        }
    }
}
