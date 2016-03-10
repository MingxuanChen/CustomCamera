package com.viclee.customcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ImageCropView extends ImageView {
    private static final int LEFT_AREA_ALPHA = 150;// 灰度区域的透明度
    private static final int CHOOSE_AREA_MIN = 200;// 选择框的最小宽高
    private static final int CORNER_RECT_SIZE = 10;// 四个角上小圆形的半径

    private final static int PRESS_LB = 0;// 表示左下角矩形框
    private final static int PRESS_LT = 1;// 表示左上角矩形框
    private final static int PRESS_RB = 2;// 表示右下角矩形框
    private final static int PRESS_RT = 3;// 表示右上角矩形框

    private Bitmap mOriginalBmp = null; // 原始图片
    private Bitmap mFinalBmp = null; // 最终切图
    private RectF mImageShowArea = null;// 图片的实际显示区域，由于图片和ImageView大小不一定一致，所以图片区域小于等于ImageView大小
    private RectF mChooseArea = null; // 选择区域
    private Paint mPaint = null; // 画笔

    private int mx = 0; // 点击位置的x坐标
    private int my = 0; // 点击位置的y坐标
    private int cornerRecFlag = -1; // 用来存储点击了哪个小矩形框
    private boolean initFlag = false;

    private RectF recLT = null; // 左上角的小矩形框
    private RectF recRT = null; // 右上角的小矩形框
    private RectF recLB = null; // 左下角的小矩形框
    private RectF recRB = null; // 右下角的小矩形框
    private RectF leftRectL = null;
    private RectF leftRectR = null;
    private RectF leftRectT = null;
    private RectF leftRectB = null;
    private Paint leftAreaPaint = null;

    public ImageCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageCropView(Context context) {
        super(context);
        init();
    }

    public void init() {
        recLT = new RectF();
        recLB = new RectF();
        recRT = new RectF();
        recRB = new RectF();
        mImageShowArea = new RectF();
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);

        mChooseArea = new RectF();
//        setCornerRecLocation();
        initFlag = true;

        // 选择框之外的灰色区域，分成四个矩形框
        leftAreaPaint = new Paint();
        leftAreaPaint.setStyle(Paint.Style.FILL);
        leftAreaPaint.setAlpha(ImageCropView.LEFT_AREA_ALPHA);
    }

    public void setBitmap(final Activity activity, final String bmpPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options opt = new BitmapFactory.Options();
//                opt.inPreferredConfig = Bitmap.Config.RGB_565;
                opt.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(bmpPath, opt);

                int ratio = opt.outWidth / getResources().getDisplayMetrics().widthPixels;
                opt.inJustDecodeBounds = false;
                opt.inSampleSize = ratio;
                mOriginalBmp = BitmapFactory.decodeFile(bmpPath, opt);

                leftRectB = new RectF();
                leftRectL = new RectF();
                leftRectR = new RectF();
                leftRectT = new RectF();

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setImageBitmap(mOriginalBmp);
                    }
                });
            }
        }).start();
    }

    public void initBitmapArea() {
        int bmpWidth = mOriginalBmp.getWidth();
        int bmpHeight = mOriginalBmp.getHeight();
        float bitmapRatio = (float) bmpHeight / (float) bmpWidth;
        float imageViewRatio = (float) getMeasuredHeight() / (float) getMeasuredWidth();
        int actualWidth = getMeasuredWidth();
        int actualHeight = getMeasuredHeight();
        int actualX = 0;
        int actualY = 0;
        if (bitmapRatio >= imageViewRatio) {
            actualWidth = getMeasuredHeight() * bmpWidth / bmpHeight;
            actualX = (getMeasuredWidth() - actualWidth) / 2;
        } else {
            actualHeight = getMeasuredWidth() * bmpHeight / bmpWidth;
            actualY = (getMeasuredHeight() - actualHeight) / 2;
        }

        mImageShowArea.set(actualX, actualY, actualWidth + actualX, actualHeight + actualY);
        // 初始化选择框为显示在图片中央的正方形
        if (actualWidth >= actualHeight) {
            mChooseArea.set((getMeasuredWidth() - actualHeight) / 2, actualY,
                    (getMeasuredWidth() + actualHeight) / 2, actualHeight + actualY);
        } else {
            mChooseArea.set(actualX, (getMeasuredHeight() - actualWidth) / 2,
                    actualWidth + actualX, (getMeasuredHeight() + actualWidth) / 2);
        }
        //
        setCornerRecLocation();
    }

    public Bitmap getSubBitmap() {
        float ratioWidth = mOriginalBmp.getWidth()
                / (float) (mImageShowArea.right - mImageShowArea.left);
        float ratioHeight = mOriginalBmp.getHeight()
                / (float) (mImageShowArea.bottom - mImageShowArea.top);
        int left = (int) ((mChooseArea.left - mImageShowArea.left) * ratioWidth);
        int right = (int) (left + (mChooseArea.right - mChooseArea.left) * ratioWidth);
        int top = (int) ((mChooseArea.top - mImageShowArea.top) * ratioHeight);
        int bottom = (int) (top + (mChooseArea.bottom - mChooseArea.top) * ratioHeight);
        initFlag = true;
        mFinalBmp = Bitmap.createBitmap(mOriginalBmp, left, top, right - left, bottom - top);

        if (mOriginalBmp != null && !mOriginalBmp.isRecycled()) {
            mOriginalBmp.recycle();
            mOriginalBmp = null;
        }
        return mFinalBmp;
    }

    public void moveChooseArea(int move_x, int move_y) {// 拖动整个选择框移动
        if (mChooseArea.left + move_x >= mImageShowArea.left
                && mChooseArea.right + move_x <= mImageShowArea.right
                && mChooseArea.top + move_y >= mImageShowArea.top
                && mChooseArea.bottom + move_y <= mImageShowArea.bottom) {// 选择框在中间的时候的处理
            mChooseArea.set(mChooseArea.left + move_x, mChooseArea.top + move_y, mChooseArea.right
                    + move_x, mChooseArea.bottom + move_y);
        } else {// 选择框在边界时候的处理
            if (mChooseArea.left + move_x < mImageShowArea.left) {
                if (mChooseArea.top + move_y > mImageShowArea.top
                        && mChooseArea.bottom + move_y < mImageShowArea.bottom) {
                    mChooseArea.set(mImageShowArea.left, mChooseArea.top + move_y,
                            mChooseArea.right, mChooseArea.bottom + move_y);
                }
            }
            if (mChooseArea.right + move_x > mImageShowArea.right) {
                if (mChooseArea.top + move_y > mImageShowArea.top
                        && mChooseArea.bottom + move_y < mImageShowArea.bottom) {
                    mChooseArea.set(mChooseArea.left, mChooseArea.top + move_y,
                            mImageShowArea.right, mChooseArea.bottom + move_y);
                }
            }

            if (mChooseArea.top + move_y < mImageShowArea.top) {
                if (mChooseArea.left + move_x > mImageShowArea.left
                        && mChooseArea.right + move_x < mImageShowArea.right) {
                    mChooseArea.set(mChooseArea.left + move_x, mImageShowArea.top,
                            mChooseArea.right + move_x, mChooseArea.bottom);
                }
            }

            if (mChooseArea.bottom + move_y > mImageShowArea.bottom) {
                if (mChooseArea.left + move_x > mImageShowArea.left
                        && mChooseArea.right + move_x < mImageShowArea.right) {
                    mChooseArea.set(mChooseArea.left + move_x, mChooseArea.top, mChooseArea.right
                            + move_x, mImageShowArea.bottom);
                }
            }
        }
        setCornerRecLocation();
        invalidate();
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mx = (int) event.getX();
            my = (int) event.getY();
            if (judgeInChooseArea(mx, my)) {// 在剪切选择区域内
                mPaint.setColor(Color.RED);
                invalidate();
                return true;
            } else {
                if (judgeInCornerArea((int) event.getX(), (int) event.getY())) {// 判断是否点击在四个角上
                    return true;
                }
            }
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // 控制选择框不能超过指定范围
            if (isOutOfArea((int) event.getX(), (int) event.getY())) {
                return true;
            }

            // 如果选择区域大小跟图像大小一样时，就不能移动
            if (mChooseArea.left == mImageShowArea.left && mChooseArea.top == mImageShowArea.top
                    && mChooseArea.right == mImageShowArea.right
                    && mChooseArea.bottom == mImageShowArea.bottom) {
            } else {
                moveChooseArea((int) event.getX() - mx, (int) event.getY() - my);
                mx = (int) event.getX();
                my = (int) event.getY();
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            cornerRecFlag = -1;
            mPaint.setColor(Color.WHITE);
            invalidate();
        }

        return super.onTouchEvent(event);
    }

    private boolean isOutOfArea(int x, int y) {
        switch (cornerRecFlag) {
        case ImageCropView.PRESS_LB:
            pressLB(x - mx, y - my);
            break;
        case ImageCropView.PRESS_LT:
            pressLT(x - mx, y - my);
            break;
        case ImageCropView.PRESS_RB:
            pressRB(x - mx, y - my);
            break;
        case ImageCropView.PRESS_RT:
            pressRT(x - mx, y - my);
            break;
        default:
            return false;
        }
        mx = x;
        my = y;
        invalidate();
        return true;
    }

    public boolean judgeInCornerArea(int x, int y) {
        if (inCornerRect(x, y, recLB)) {
            cornerRecFlag = ImageCropView.PRESS_LB;
            return true;
        } else if (inCornerRect(x, y, recLT)) {
            cornerRecFlag = ImageCropView.PRESS_LT;
            return true;
        } else if (inCornerRect(x, y, recRB)) {
            cornerRecFlag = ImageCropView.PRESS_RB;
            return true;
        } else if (inCornerRect(x, y, recRT)) {
            cornerRecFlag = ImageCropView.PRESS_RT;
            return true;
        }
        return false;
    }

    public boolean inCornerRect(int x, int y, RectF rect) {
        if (x >= rect.left - 20 && x <= rect.right + 20 && y > rect.top - 20
                && y < rect.bottom + 20) {
            return true;
        }
        return false;
    }

    private void pressLB(int x, int y) {
        float left = mChooseArea.left + x;
        float right = mChooseArea.right;
        float top = mChooseArea.top;
        float bottom = mChooseArea.bottom + y;
        if (left <= right - CHOOSE_AREA_MIN && left >= mImageShowArea.left
                && bottom <= mImageShowArea.bottom && bottom >= top + CHOOSE_AREA_MIN) {// 选择框在可拉伸的范围，设置拉伸后的位置
            mChooseArea.set(left, top, right, bottom);
        } else {// 选择框拉伸后不能超出图片范围或者小于CHOOSE_AREA_MIN*CHOOSE_AREA_MIN像素
            if (left < mImageShowArea.left) {
                left = mImageShowArea.left;
            } else if (left > right - CHOOSE_AREA_MIN) {
                left = right - CHOOSE_AREA_MIN;
            }

            if (bottom > mImageShowArea.bottom) {
                bottom = mImageShowArea.bottom;
            } else if (bottom < top + CHOOSE_AREA_MIN) {
                bottom = top + CHOOSE_AREA_MIN;
            }

            mChooseArea.set(left, top, right, bottom);
        }
        setCornerRecLocation();
    }

    private void pressLT(int x, int y) {
        float left = mChooseArea.left + x;
        float right = mChooseArea.right;
        float top = mChooseArea.top + y;
        float bottom = mChooseArea.bottom;
        if (left <= right - CHOOSE_AREA_MIN && left >= mImageShowArea.left
                && top <= bottom - CHOOSE_AREA_MIN && top >= mImageShowArea.top) {// 选择框在可拉伸的范围，设置拉伸后的位置
            mChooseArea.set(left, top, right, bottom);
        } else {// 选择框拉伸后不能超出图片范围或者小于CHOOSE_AREA_MIN*CHOOSE_AREA_MIN像素
            if (left < mImageShowArea.left) {
                left = mImageShowArea.left;
            } else if (left > right - CHOOSE_AREA_MIN) {
                left = right - CHOOSE_AREA_MIN;
            }

            if (top < mImageShowArea.top) {
                top = mImageShowArea.top;
            } else if (top > bottom - CHOOSE_AREA_MIN) {
                top = bottom - CHOOSE_AREA_MIN;
            }
            mChooseArea.set(left, top, right, bottom);
        }
        setCornerRecLocation();
    }

    private void pressRT(int x, int y) {
        float left = mChooseArea.left;
        float right = mChooseArea.right + x;
        float top = mChooseArea.top + y;
        float bottom = mChooseArea.bottom;

        if (right <= mImageShowArea.right && right >= left + CHOOSE_AREA_MIN
                && top <= bottom - CHOOSE_AREA_MIN && top >= mImageShowArea.top) {// 选择框在可拉伸的范围，设置拉伸后的位置
            mChooseArea.set(left, top, right, bottom);
        } else {// 选择框拉伸后不能超出图片范围或者小于CHOOSE_AREA_MIN*CHOOSE_AREA_MIN像素
            if (right > mImageShowArea.right) {
                right = mImageShowArea.right;
            } else if (right < left + CHOOSE_AREA_MIN) {
                right = left + CHOOSE_AREA_MIN;
            }

            if (top < mImageShowArea.top) {
                top = mImageShowArea.top;
            } else if (top > bottom - CHOOSE_AREA_MIN) {
                top = bottom - CHOOSE_AREA_MIN;
            }
            mChooseArea.set(left, top, right, bottom);
        }
        setCornerRecLocation();
    }

    private void pressRB(int x, int y) {
        float left = mChooseArea.left;
        float right = mChooseArea.right + x;
        float top = mChooseArea.top;
        float bottom = mChooseArea.bottom + y;

        if (right <= mImageShowArea.right && right >= left + CHOOSE_AREA_MIN
                && bottom <= mImageShowArea.bottom && bottom >= top + CHOOSE_AREA_MIN) {// 选择框在可拉伸的范围，设置拉伸后的位置
            mChooseArea.set(left, top, right, bottom);
        } else {// 选择框拉伸后不能超出图片范围或者小于CHOOSE_AREA_MIN*CHOOSE_AREA_MIN像素
            if (right > mImageShowArea.right) {
                right = mImageShowArea.right;
            } else if (right < left + CHOOSE_AREA_MIN) {
                right = left + CHOOSE_AREA_MIN;
            }

            if (bottom > mImageShowArea.bottom) {
                bottom = mImageShowArea.bottom;
            } else if (bottom < top + CHOOSE_AREA_MIN) {
                bottom = top + CHOOSE_AREA_MIN;
            }
            mChooseArea.set(left, top, right, bottom);
        }
        setCornerRecLocation();
    }

    // 跟随选择区域改变四个小矩形的位置
    private void setCornerRecLocation() {
        recLT.set(mChooseArea.left - CORNER_RECT_SIZE, mChooseArea.top - CORNER_RECT_SIZE,
                mChooseArea.left + CORNER_RECT_SIZE, mChooseArea.top + CORNER_RECT_SIZE);
        recLB.set(mChooseArea.left - CORNER_RECT_SIZE, mChooseArea.bottom - CORNER_RECT_SIZE,
                mChooseArea.left + CORNER_RECT_SIZE, mChooseArea.bottom + CORNER_RECT_SIZE);
        recRT.set(mChooseArea.right - CORNER_RECT_SIZE, mChooseArea.top - CORNER_RECT_SIZE,
                mChooseArea.right + CORNER_RECT_SIZE, mChooseArea.top + CORNER_RECT_SIZE);
        recRB.set(mChooseArea.right - CORNER_RECT_SIZE, mChooseArea.bottom - CORNER_RECT_SIZE,
                mChooseArea.right + CORNER_RECT_SIZE, mChooseArea.bottom + CORNER_RECT_SIZE);
    }

    public boolean judgeInChooseArea(float x, float y) {
        float start_x = mChooseArea.left;
        float start_y = mChooseArea.top;
        float last_x = mChooseArea.right;
        float last_y = mChooseArea.bottom;
        if (x > start_x + 10 && x < last_x - 10 && y > start_y + 10 && y < last_y - 10) {
            return true;
        }
        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOriginalBmp == null) {
            return;
        }
        if (initFlag) {
            initBitmapArea();
            initFlag = false;
        }

        // 绘制选择框之外的灰色区域
        setLeftFourGrayArea();
        canvas.drawRect(leftRectL, leftAreaPaint);
        canvas.drawRect(leftRectR, leftAreaPaint);
        canvas.drawRect(leftRectT, leftAreaPaint);
        canvas.drawRect(leftRectB, leftAreaPaint);

        // 绘制选择框
        mPaint.setStyle(Paint.Style.STROKE); // 将画笔的风格改为空心
        mPaint.setStrokeWidth(3);
        canvas.drawRect(mChooseArea, mPaint);
        // 绘制小矩形
        mPaint.setStyle(Style.FILL);// 将画笔的风格改为实心
        canvas.drawOval(recLT, mPaint);
        canvas.drawOval(recLB, mPaint);
        canvas.drawOval(recRT, mPaint);
        canvas.drawOval(recRB, mPaint);
    }

    // 设置选择框之外的灰色区域
    public void setLeftFourGrayArea() {
        leftRectL.set(mImageShowArea.left, mImageShowArea.top, mChooseArea.left,
                mImageShowArea.bottom);
        leftRectR.set(mChooseArea.right, mImageShowArea.top, mImageShowArea.right,
                mImageShowArea.bottom);
        leftRectT.set(mChooseArea.left, mImageShowArea.top, mChooseArea.right, mChooseArea.top);
        leftRectB.set(mChooseArea.left, mChooseArea.bottom, mChooseArea.right,
                mImageShowArea.bottom);
    }
}
