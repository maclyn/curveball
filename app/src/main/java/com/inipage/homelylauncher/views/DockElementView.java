package com.inipage.homelylauncher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.swiper.AppEditAdapter;

public class DockElementView extends View {
    private static final String TAG = "DockElementView";

    private class DockChangeState {
        private DockAction action;
        private int index;

        public DockChangeState(DockAction action, int index) {
            this.action = action;
            this.index = index;
        }

        public DockAction getAction() {
            return action;
        }

        public int getIndex() {
            return index;
        }
    }

    private enum DockAction {
        ACTION_NONE, ACTION_REPLACE, ACTION_PUSH_LEFT, ACTION_PUSH_RIGHT
    }

    private DockChangeState toChange;
    private OnDragListener dragListener = new OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {

            requestLayout();
            return true;
        }
    };
    private boolean inited = false;

    public DockElementView(Context context) {
        super(context);
        init();
    }

    public DockElementView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DockElementView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(){
        this.setOnDragListener(dragListener);
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    /**
     * Called whenever the layout is changed. Call requestLayout(...) to get this called.
     * @param changed If the layout has been changed.
     * @param l Left position of view.
     * @param t Top position of view.
     * @param r Right position of view.
     * @param b Bottom position of view.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!inited) {
            Log.d(TAG, "onLayout(...) called before init");
            return;
        }

    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch(event.getAction()){
            case DragEvent.ACTION_DRAG_STARTED:
                return true; //Nothing to do
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_LOCATION:
            case DragEvent.ACTION_DRAG_EXITED:
                return true;
            case DragEvent.ACTION_DROP:
                return true;
        }
        /**
         * Dock algorithm:
         *
         * On drag enter {
         *     Call on drag move with parameters
         * }
         *
         * On drag move {
         *      Calcuate which "slot" the dragged item is currently over (0th, 0.8, 1st, 1.2, 1.8, 2.0, 2.2, etc.)
         *          If slot is a full slot:
         *              Tint dragged icon green ("valid")
         *              Set "dockChange" object to { type = replace, index = index }
         *              Animate all present dock icons to translationX 0
         *          If slot is a "x.8" slot:
         *              If element to right exists:
         *                  If number of element to right < number of slots to right:
         *                      Animate all elements to right up to the slot to a translationX of [width of element]
         *                      Tint dragged icon green ("valid")
         *                      Set "dockChange" object to { type = pushRight, index = index }
         *                  Else
         *                      Perform animation set {
         *                          Animate all elements to right up to the slot to a translationX of [width of element * 0.3]
         *                          Animate all elements to right up to the slot to a translationX of [0]
         *                      }
         *                      Set "dockChange" object to { type = none, index = -1}
         *          If slot is a "x.2" slot:
         *              If element to left exists:
         *                  If number of element to left < number of slots to left:
         *                      Animate all elements to right up to the slot to a translationX of -[width of element]
         *                      Tint dragged icon green ("valid")
         *                      Set "dockChange" object to { type = pushLeft, index = index }
         *                  Else
         *                      Perform animation set {
         *                          Animate all elements to right up to the slot to a translationX of -[width of element * 0.3]
         *                          Animate all elements to right up to the slot to a translationX of [0]
         *                      }
         *                      Set "dockChange" object to { type = none, index = -1}
         *          Else
         *              Animate all present dock icons to translationX 0
         *              Set "dockChange" object to { type = none, index = -1}
         * }
         *
         * On drag exit {
         *      Remove any tints on icon
         *      Animate all present dock icons to translationX 0
         *      If dockChange.type == none:
         *          Animate all present dock icons to translationX 0
         *      Else if dockChange.type = replace:
         *      Else if dockChange.type = pushLeft:
         *      Else if dockChange.type = pushRight:
         *
         * }
         */
        return true;
    }
}
