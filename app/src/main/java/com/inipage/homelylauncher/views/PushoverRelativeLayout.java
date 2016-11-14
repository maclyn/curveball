package com.inipage.homelylauncher.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class PushoverRelativeLayout extends RelativeLayout {
    public interface PushoverObserver {
        boolean onInterceptEvent(MotionEvent ev);
    }

    private boolean shouldIntercept = false;
    private PushoverObserver observer;

    public PushoverRelativeLayout(Context context) {
        super(context);
    }

    public PushoverRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PushoverRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PushoverRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(super.onInterceptTouchEvent(ev)){ //Okay, we don't care
            return true;
        }

        return observer != null && observer.onInterceptEvent(ev);
    }

    public void registerObserver(PushoverObserver observer){
        this.observer = observer;
    }

    public void unregisterObserver(){
        this.observer = null;
    }
}
