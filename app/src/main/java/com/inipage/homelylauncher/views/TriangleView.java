package com.inipage.homelylauncher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.View;

import com.inipage.homelylauncher.R;

public class TriangleView extends View {
    Paint defaultPaint;

    public TriangleView(Context context) {
        super(context);
        init();
    }

    public TriangleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TriangleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        defaultPaint = new Paint();
        defaultPaint.setColor(getResources().getColor(R.color.triangle_color));
        defaultPaint.setStrokeWidth(0);
        defaultPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        defaultPaint.setShader(new LinearGradient(getWidth() / 2, 0, getWidth() / 2, getHeight(),
                new int[] {
                        getResources().getColor(R.color.triangle_color),
                        getResources().getColor(R.color.transparent)
                },
                null,
                Shader.TileMode.CLAMP));
        defaultPaint.setAntiAlias(true);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(changed){
            defaultPaint.setShader(new LinearGradient(getWidth() / 2, 0, getWidth() / 2, getHeight(),
                    new int[] {
                            getResources().getColor(R.color.triangle_color),
                            getResources().getColor(R.color.transparent)
                    },
                    null,
                    Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path path = new Path();
        path.moveTo(0, getHeight());
        path.lineTo(getWidth() / 2, 0);
        path.lineTo(getWidth(), getHeight());
        path.lineTo(0, getHeight());
        canvas.drawPath(path, defaultPaint);
    }
}
