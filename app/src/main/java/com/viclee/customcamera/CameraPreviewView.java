package com.viclee.customcamera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.util.List;

public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback {

    private OnCameraStatusListener listener;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    public boolean isCameraFront = false;// 默认为后置摄像头
    public boolean shutterFlag = false;

    private ShutterCallback shutterCallback = new ShutterCallback() {
        @Override
        public void onShutter() {
        }
    };
    private PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                cameraErrorToast();
            }

            if (null != listener) {
                listener.onCameraStopped(data);
            }
        }
    };

    public CameraPreviewView(Context context) {
        this(context, null);
    }

    public CameraPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    @SuppressWarnings("deprecation")
    private void init(Context context, AttributeSet attrs, int defStyle) {
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (null == mCamera) {
            setCameraFront(isCameraFront);
        } else {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception e) {
                cameraErrorToast();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                cameraErrorToast();
            }
        }
    }

    public void autoFocus() {
        if (mCamera != null) {
            try {
                mCamera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (null != listener) {
                            listener.onAutoFocus(success);
                        }
                    }
                });
            } catch (Exception e) {
                cameraErrorToast();
            }
        }
    }

    public void takePicture() {
        if (mCamera != null) {
            try {
                mCamera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (null != listener) {
                            listener.onAutoFocus(success);
                        }
                        try {
                            if (shutterFlag) {
                                camera.takePicture(shutterCallback, null, pictureCallback);
                            } else {
                                camera.takePicture(null, null, pictureCallback);
                            }
                        } catch (Exception e) {
                            cameraErrorToast();
                        }
                    }
                });
                return;
            } catch (Exception e) {
                cameraErrorToast();
            }
        }
    }

    public void setOnCameraStatusListener(OnCameraStatusListener listener) {
        this.listener = listener;
    }

    public interface OnCameraStatusListener {
        void onCameraStopped(byte[] data);

        void onAutoFocus(boolean success);
    }

    public boolean isSupportFlash() {
        if (mCamera == null) {
            try {
                mCamera = Camera.open();
            } catch (Exception e) {
                cameraErrorToast();
                return false;
            }
        }
        if (mCamera == null) {
            return false;
        }
        try {
            Parameters parameters = mCamera.getParameters();
            List<String> flashModes = parameters.getSupportedFlashModes();
            if (flashModes == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            cameraErrorToast();
            mCamera = null;
            return false;
        }
    }

    public void changeFlashlight() {
        Camera.Parameters mParams = mCamera.getParameters();
        if (mParams.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON)) {
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        } else {
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        }
        mCamera.setParameters(mParams);
    }

    public void setCameraFront(boolean isFront) {
        CameraInfo cameraInfo = new CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();// 摄像头的个数
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);// 每个摄像头的信息
            if ((!isFront && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    || (isFront && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)) {
                try {
                    if (mCamera != null) {
                        mCamera.stopPreview();
                        mCamera.release();
                    }

                    mCamera = Camera.open(i);// 打开指定的摄像头

                    if (mCamera == null) {// 打开失败
                        return;
                    }
                    mCamera.setDisplayOrientation(90);// 旋转90以竖屏显示

                    // 开始设置相机的参数
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setPictureFormat(ImageFormat.JPEG);
                    parameters.setJpegQuality(90);
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);// 默认进来关闭闪光灯
                    parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

                    String focusMode = getProperFocusMode(parameters.getSupportedFocusModes());
                    parameters.setFocusMode(focusMode);// 设置支持的聚焦模式，默认为连续对焦

                    DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();// 设置预览大小和图片大小
                    float ratio = (float) metrics.widthPixels / (float) metrics.heightPixels;
                    int screenPixel = (int) (metrics.widthPixels * metrics.heightPixels);
                    Camera.Size preSize = getProperPreviewSize(
                            parameters.getSupportedPreviewSizes(), ratio, screenPixel);
                    Camera.Size saveSize = getProperSaveSize(parameters.getSupportedPictureSizes(),
                            ratio);

                    if ("Nexus 4".equalsIgnoreCase(Build.MODEL)) {
                        parameters.setPictureSize(saveSize.height, saveSize.width);
                    } else {
                        parameters.setPictureSize(saveSize.width, saveSize.height);
                    }

                    parameters.setPreviewSize(preSize.width, preSize.height);

                    try {
                        mCamera.setParameters(parameters);
                    } catch (Exception e) {
                        cameraErrorToast();
                    }
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();
                    mCamera.autoFocus(null);
                    
                    isCameraFront = isFront;
                } catch (Exception e) {
                    cameraErrorToast();
                    e.printStackTrace();
                    return;
                }
                break;
            }
        }
    }

    private void cameraErrorToast() {
        Toast.makeText(getContext(), "相机无法打开", Toast.LENGTH_SHORT).show();
    }

    private static String getProperFocusMode(List<String> supported) {
        if (supported == null)
            return Parameters.FOCUS_MODE_AUTO;
        for (String mode : supported) {
            if (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mode))
                return mode;
        }
        for (String mode : supported) {
            if (Parameters.FOCUS_MODE_AUTO.equals(mode))
                return mode;
        }
        if (supported != null && supported.size() > 0) {
            return supported.get(0);
        }
        return Parameters.FOCUS_MODE_AUTO;
    }

    private static Camera.Size getMaxSize(List<Camera.Size> supported) {
        Camera.Size maxSize = null;
        int pixN, maxPixelN = 0;
        for (Camera.Size size : supported) {
            pixN = size.width * size.height;
            if (pixN > maxPixelN) {
                maxPixelN = pixN;
                maxSize = size;
            }
        }
        return maxSize;
    }

    private static Camera.Size getProperPreviewSize(List<Camera.Size> supported, float aspectRatio,
            int screenPixel) {
        Camera.Size maxSize = null;
        int pix, maxPixel = 0;
        for (Camera.Size size : supported) {
            float ratio = (float) size.width / (float) size.height;
            if (aspectRatio < 1 && ratio > 1) {
                ratio = (float) size.height / (float) size.width;
            }
            if (Math.abs(ratio - aspectRatio) < 0.001f) {
                pix = size.width * size.height;
                if (pix <= screenPixel) {
                    if (pix > maxPixel) {
                        maxPixel = pix;
                        maxSize = size;
                    }
                }
            }
        }
        if (maxSize == null)
            return getMaxSize(supported);
        return maxSize;
    }

    private static Camera.Size getProperSaveSize(List<Camera.Size> supported, float aspectRatio) {
        Camera.Size maxSize = null;
        int pixN, maxPixelN = 0;
        for (Camera.Size size : supported) {
            float ratio = (float) size.width / (float) size.height;
            if (aspectRatio < 1 && ratio > 1) {
                ratio = (float) size.height / (float) size.width;
            }
            if (Math.abs(ratio - aspectRatio) < 0.001f) {
                pixN = size.width * size.height;
                if (pixN > maxPixelN) {
                    maxPixelN = pixN;
                    maxSize = size;
                }
            }
        }
        if (maxSize == null)
            return getMaxSize(supported);
        return maxSize;
    }
}
