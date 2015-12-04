package com.inipage.homelylauncher.views;

import android.content.Context;
import android.gesture.Gesture;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.inipage.homelylauncher.utils.AttributeApplier;
import com.inipage.homelylauncher.utils.SizeAttribute;

public class DragToOpenView extends RelativeLayout {
    public interface OnDragToOpenListener {
        void onDragStarted();
        void onDragChanged(float distance);
        void onDragCompleted(boolean dragAccepted, float finalDistance, float flingVelocity);
    }

    private static final String TAG = "DragToOpenView";

    float startY = -1;
    float lastY = -1;
    float lastDist = -1;
    float lastTime = -1;
    OnDragToOpenListener listener;

    @SizeAttribute(24)
    float startSwipeDistance;

    public DragToOpenView(Context context) {
        super(context);
        init();
    }

    public DragToOpenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragToOpenView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        AttributeApplier.ApplyDensity(this, getContext());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dist = startY - event.getRawY();

        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
                lastDist = event.getRawY() - (lastY == -1 ? event.getRawY() : lastY);
                lastY = event.getRawY();
                lastTime = event.getEventTime();
                if(listener != null) listener.onDragChanged(dist);
                break;
            case MotionEvent.ACTION_UP:
                float time = event.getEventTime() - lastTime;
                if(time == 0) time = 1;

                if(listener != null) listener.onDragCompleted(dist > startSwipeDistance * 4, dist,
                        Math.abs(lastDist / time));
                break;
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                startY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dist = startY - ev.getRawY();
                Log.d(TAG, "Distance: " + dist);
                if(dist > startSwipeDistance){
                    if(listener != null) listener.onDragStarted();
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        //No.
    }

    public void setOnDragToOpenListener(OnDragToOpenListener listener){
        this.listener = listener;
    }
}
