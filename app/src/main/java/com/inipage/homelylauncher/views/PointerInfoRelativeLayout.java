package com.inipage.homelylauncher.views;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.inipage.homelylauncher.DebugActivity;

public class PointerInfoRelativeLayout extends RelativeLayout {
    float x = -1;
    float y = -1;

    public PointerInfoRelativeLayout(Context context) {
        super(context);
    }

    public PointerInfoRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PointerInfoRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PointerInfoRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP){
            x = -1;
            y = -1;
        } else if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_MOVE){
            x = ev.getX();
            y = ev.getY();
        } else {
            Log.d(DebugActivity.TAG, "Other touch event: " + ev.getAction());
        }

        return super.dispatchTouchEvent(ev);
    }

    public PointF getPointLocation(){
        if(x == -1 || y == -1) return null;
        return new PointF(x, y);
    }
}