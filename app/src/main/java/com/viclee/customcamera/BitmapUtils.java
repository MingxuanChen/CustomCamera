package com.viclee.customcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;

public class BitmapUtils {
    public static final String PHOTO_FOLDER = "/VICLEEPHOTO";
    public static final String FILE_PERMISSION = "rw";

    public static String saveBitmap(Context ctx, Bitmap bmp, Bitmap.CompressFormat picFormat,
            int quality) {
        try {
            if (bmp == null)
                return null;
            String picSuffix = null;
            String dcimDir = getPhotoSavePath();
            File dirFile = new File(dcimDir);
            if (!dirFile.exists()) {
                if (!dirFile.mkdirs()) {
                    Toast.makeText(ctx, "保存图片失败", Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            if (picFormat.equals(Bitmap.CompressFormat.PNG)) {
                picSuffix = ".png";
            } else if (picFormat.equals(Bitmap.CompressFormat.JPEG)) {
                picSuffix = ".jpg";
            } else {
                return null;
            }

            String filePath = dcimDir + "/" + String.valueOf(new java.util.Date().getTime())
                    + picSuffix;

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(picFormat, quality, stream);

            RandomAccessFile accessFile = null;
            try {
                accessFile = new RandomAccessFile(new File(filePath), FILE_PERMISSION);
                accessFile.write(stream.toByteArray());
            } catch (Exception e) {
                return null;
            } finally {
                stream.close();
                if (accessFile != null) {
                    accessFile.close();
                }
            }
            return filePath;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getPhotoSavePath() {
        String dcimDir = null;

        File extDcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (extDcimDir == null || !extDcimDir.exists()) {
            File extDir = Environment.getExternalStorageDirectory();
            if (extDir == null) {
                return null;
            }
            dcimDir = extDir.getAbsolutePath() + "/DCIM";
            try {
                new File(dcimDir).mkdirs();
            } catch (Exception e) {
            }
        } else {
            dcimDir = extDcimDir.getAbsolutePath();
        }
        return dcimDir + PHOTO_FOLDER;
    }

    public static String getPhotoSavePath(String dir) {
        String dcimDir = null;

        File extDcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (extDcimDir == null || !extDcimDir.exists()) {
            File extDir = Environment.getExternalStorageDirectory();
            if (extDir == null) {
                return null;
            }
            dcimDir = extDir.getAbsolutePath() + "/DCIM";
            try {
                new File(dcimDir).mkdirs();
            } catch (Exception e) {
            }
        } else {
            dcimDir = extDcimDir.getAbsolutePath();
        }
        if (dir == null) {
            return dcimDir + PHOTO_FOLDER;
        } else {
            return dcimDir + "/" + dir;
        }
    }
}
