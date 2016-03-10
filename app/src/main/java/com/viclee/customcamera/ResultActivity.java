package com.viclee.customcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

public class ResultActivity extends AppCompatActivity {
    ImageView imageView;
    String fileName = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_result);

        imageView = (ImageView) findViewById(R.id.result_image);

        fileName = getIntent().getStringExtra("image");
        Bitmap bitmap = BitmapFactory.decodeFile(fileName);

        imageView.setImageBitmap(bitmap);
    }
}
