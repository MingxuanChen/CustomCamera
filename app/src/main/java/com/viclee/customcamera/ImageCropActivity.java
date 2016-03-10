package com.viclee.customcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class ImageCropActivity extends Activity {
    private ImageCropView cropView = null;
    private String originalUri = null;
    private TextView cancel;
    private TextView ensure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
        setContentView(R.layout.activity_image_crop);
        init();
    }

    private void init() {
        originalUri = getIntent().getStringExtra("image");

        cropView = (ImageCropView) findViewById(R.id.image_crop_view);

//        DisplayMetrics metrics = getResources().getDisplayMetrics();
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.outWidth = metrics.widthPixels;
//        options.outHeight = metrics.heightPixels;
        cropView.setBitmap(this, originalUri);

        cancel = (TextView) findViewById(R.id.cutCancel);
        cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ImageCropActivity.this.finish();
            }
        });

        ensure = (TextView) findViewById(R.id.cutEnsure);
        ensure.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = BitmapUtils.saveBitmap(ImageCropActivity.this,
                        cropView.getSubBitmap(), Bitmap.CompressFormat.JPEG, 100);
                refreshMediaStore(fileName);

                Intent intent = new Intent(ImageCropActivity.this, ResultActivity.class);
                intent.putExtra("image", fileName);
                startActivity(intent);
                ImageCropActivity.this.finish();
            }
        });
    }

    // 由于拍照完成保存后，数据库不会立即更新，故需要调用下面的函数来更新多媒体数据库
    private void refreshMediaStore(String fileName) {
        MediaScannerConnection.scanFile(this, new String[]{fileName}, null, null);
    }
}
