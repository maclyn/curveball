package com.inipage.homelylauncher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.inipage.homelylauncher.R;

public class SearchPullView extends View implements PointerInfoRelativeLayout.ScrollViewPullListener {
    public interface SearchPullListener {
        void onPullAccepted();
    }

    public static final String TAG = "SearchPullView";

    public SearchPullView(Context context) {
        super(context);
    }

    public SearchPullView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchPullView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    SearchPullListener mListener;

    //Search data elements -- fixed to just search apps for now
    private int[] SEARCH_COLORS = new int[] { Color.BLACK };
    private int[] SEARCH_ICONS = new int[] { R.drawable.ic_search_white_48dp };

    //Dimensions
    Drawable toDraw = null;
    float topScrim = -1;
    float searchSize = -1;
    float openDist = -1;
    float maxDistPerItem = -1;
    Paint paint;

    public synchronized void setTopScrimSize(SearchPullListener listener, Context context, float topScrim){
        this.mListener = listener;
        this.topScrim = topScrim;
        this.searchSize = context.getResources().getDimension(R.dimen.search_circle_size);
        this.openDist = context.getResources().getDimension(R.dimen.open_dist);
        this.maxDistPerItem = context.getResources().getDimension(R.dimen.open_dist);
        paint = new Paint();
        toDraw = context.getResources().getDrawable(SEARCH_ICONS[0]);
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(paint == null) return;
        if(!isPulling) return;

        paint.setColor(SEARCH_COLORS[0]);
        paint.setAlpha((int) (currentElement * 255F));

        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        toDraw.setAlpha((int) (currentElement * 255F));
        toDraw.setBounds((int) ((getWidth() / 2) - (searchSize / 2)), (int) (getHeight() - searchSize), (int) ((getWidth() / 2) + (searchSize / 2)), getHeight());
        toDraw.draw(canvas);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);

        int heightOut = hSize;
        int widthOut = wSize;

        if(topScrim != -1) {
            if(wMode == MeasureSpec.AT_MOST){
                //Log.d(TAG, "wMode AT_MOST " + wSize);
                widthOut = wSize;
            } else if (wMode == MeasureSpec.EXACTLY){
                //Log.d(TAG, "wMode EXACTLY " + wSize);
                widthOut = wSize;
            } else { //Whatever you want
                //Log.d(TAG, "wMode DO_WHATEVER");
                widthOut = wSize;
            }

            if(hMode == MeasureSpec.AT_MOST || hMode == MeasureSpec.UNSPECIFIED){ //Whatever you want; normal case
                //Log.d(TAG, "hMode DO_WUATEVER");
                if(isPulling){
                    if(currentElement < 1) {
                        heightOut = (int) ((topScrim + searchSize) * currentElement);
                    } else {
                        heightOut = (int) (topScrim + searchSize);
                    }
                } else {
                    heightOut = 0;
                }
           } else {
                //Log.d(TAG, "hMode EXACTLY " + hSize);
                heightOut = hSize;
            }
        }


        //Log.d(TAG, "New size: " + widthOut + "x" + heightOut);
        setMeasuredDimension(widthOut, heightOut);
    }

    boolean isPulling = false;
    float currentElement = -1;
    float distPerElement = -1;

    @Override
    public void onPullStart(float currentY, float startY, float height) {
        Log.d(TAG, "onPullStart");
        this.isPulling = true;
        this.distPerElement = (height - startY > ((SEARCH_ICONS.length  * maxDistPerItem) + openDist)
                ? maxDistPerItem
                : (height - startY - openDist)  / SEARCH_ICONS.length);
        if(this.distPerElement < 0) { //With openDist, if this is <0, then we can't open... but that's okay
            distPerElement = 0.0F;
        }
        onPullMoveImpl(currentY, startY, height);
    }

    @Override
    public void onPullMove(float currentY, float startY, float height) {
        Log.d(TAG, "onPullMove " + currentY + " " + startY + " " + height);
        onPullMoveImpl(currentY, startY, height);
    }

    private void onPullMoveImpl(float currentY, float startY, float height){
        currentElement = 0;

        if(currentY < startY) {
            currentElement = 0;
        } else {
            float acc = currentY - startY;
            currentElement = (acc / openDist);
            if(currentElement > 1F) currentElement = 1F;
        }

        requestLayout();
        invalidate();
    }

    @Override
    public void onPullAccept(float finalY, float startY, float height) {
        Log.d(TAG, "onPullAccept " + finalY + " " + startY + " " + height);
        this.isPulling = false;
        if(currentElement > 0.8F){
            mListener.onPullAccepted();
        } else {
        }
        requestLayout();
        invalidate();
    }

    @Override
    public void onPullCancel() {
        Log.d(TAG, "onPullCancel");
        this.isPulling = false;
        requestLayout();
        invalidate();
    }
}
