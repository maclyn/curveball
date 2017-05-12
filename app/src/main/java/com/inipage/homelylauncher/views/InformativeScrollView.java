package com.inipage.homelylauncher.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class InformativeScrollView extends ScrollView {
    public static final String TAG = "InformativeScrollView";

    public InformativeScrollView(Context context) {
        super(context);
    }

    public InformativeScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InformativeScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        View view = getChildAt(0);

        if(getScrollY() == 0 && oldt != 0) {
         //   Log.d(TAG, "now at top!");
        }

        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

       // Log.d(TAG, "ScrollY + clampedY " + scrollY + ", " + clampedY);
    }
}
