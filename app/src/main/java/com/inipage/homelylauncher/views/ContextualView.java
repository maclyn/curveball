package com.inipage.homelylauncher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.inipage.homelylauncher.model.ContextualElement;

import java.util.List;

public class ContextualView extends View implements PointerInfoRelativeLayout.ScrollViewPullListener {
    public static final String TAG = "ContextualView";

    public interface ContextualViewListener {
        void openElement(ContextualElement element);
    }

    List<ContextualElement> elements;

    public ContextualView(Context context) {
        super(context);
    }

    public ContextualView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ContextualView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ContextualView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void attachContextualList(List<ContextualElement> elements){
        this.elements = elements;
        invalidate();
    }

    public void noteContextualListChanged(){
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }



    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(elements == null) { //Basically, take 0dp x 0dp
            setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                    getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else { //Depending on our mode (IS_PULLING), do various things

        }
    }

    @Override
    public void onPullStart(float currentY, float startY, float height) {
        Log.d(TAG, "onPullStart");
    }

    @Override
    public void onPullMove(float currentY, float startY, float height) {
        Log.d(TAG, "onPullMove " + currentY + " " + startY + " " + height);
    }

    @Override
    public void onPullAccept(float finalY, float startY, float height) {
        Log.d(TAG, "onPullAccept " + finalY + " " + startY + " " + height);
    }

    @Override
    public void onPullCancel() {
        Log.d(TAG, "onPullCancel");
    }

}
