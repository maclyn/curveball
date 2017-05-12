package com.inipage.homelylauncher.views;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.inipage.homelylauncher.DebugActivity;

public class PointerInfoRelativeLayout extends RelativeLayout {
    public static final String TAG = "PointerInfoRL";

    public interface ScrollViewPullListener {
        void onPullStart(float currentY, float startY, float height);
        void onPullMove(float currentY, float startY, float height);
        void onPullAccept(float finalY, float startY, float height);
        void onPullCancel();
    }

    ScrollView sv;
    ScrollViewPullListener listener;
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

    boolean consideringEvent = true;
    float touchSlop = -1;
    float startY = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //We only get here once we've proven we're at the top; we then tell the listener what's happening
        //i.e. startY, currentY, and maxY
        //If this view is clickable, this is different... so don't make this view clickable.
        switch(event.getAction()){
            case MotionEvent.ACTION_MOVE:
                listener.onPullMove(event.getY(), startY, getHeight());
                break;
            case MotionEvent.ACTION_UP:
                listener.onPullAccept(event.getY(), startY, getHeight());
                break;
            case MotionEvent.ACTION_CANCEL:
                listener.onPullCancel();
                break;
        }

        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(touchSlop == -1){
            touchSlop = ViewConfiguration.get(this.getContext()).getScaledTouchSlop();
        }

        switch(ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                if(sv != null){
                    if(!ViewCompat.canScrollVertically(sv, -1)){ //Only can do it if we start at the top
                        consideringEvent = true;
                    } else {
                        consideringEvent = false;
                    }
                } else {
                    consideringEvent = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(consideringEvent){
                    float currentY = ev.getY();
                    if(currentY - startY > touchSlop) { //Down enough to start event
                        listener.onPullStart(currentY, startY, getHeight());
                        return true;
                    } else if (currentY < startY){ //Scroll down list; don't care
                        consideringEvent = false;
                        return false;
                    } else {
                        //We're just still thinking
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //If we get here, we never started; we actually never need to reset values
                break;
        }

        return super.onInterceptTouchEvent(ev); //This will be false
    }

    public void attachScrollView(ScrollView sv, ScrollViewPullListener listener){
        this.sv = sv;
        this.listener = listener;
    }

    public PointF getPointLocation(){
        if(x == -1 || y == -1) return new PointF(0, 0);
        return new PointF(x, y);
    }
}