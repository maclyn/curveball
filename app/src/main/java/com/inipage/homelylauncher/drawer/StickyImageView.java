package com.inipage.homelylauncher.drawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * An 'ImageView' _without_ many of the optimizations of Android's ImageView, allowing for more
 * direct memory management (I mean, we're manually caching bitmaps anyways...)
 */
public class StickyImageView extends View {
    Bitmap bitmap;

    Rect src = new Rect();
    Rect dst = new Rect();
    Paint paint = new Paint();

    public StickyImageView(Context context) {
        super(context);
    }

    public StickyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StickyImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected synchronized void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(changed){
            this.dst.set(0, 0, getWidth(), getHeight());
        }
    }

    public synchronized void setImageBitmap(Bitmap bitmap){
        this.bitmap = bitmap;
        if(bitmap != null){
            this.src.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            this.dst.set(0, 0, getWidth(), getHeight());
            this.paint.setAntiAlias(true);
        }
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if(bitmap != null) canvas.drawBitmap(bitmap, src, dst, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }
}
