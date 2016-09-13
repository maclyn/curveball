package com.inipage.homelylauncher.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DockView extends View {
    public static final String TAG = "DockView";

    public static final long DOCK_MOVEMENT_ANIMATION_DURATION_MS = 500;
    public static final long DOCK_FLASH_ANIMATION_DURATION_MS = 500;

    private static final long FRAME_TIME_MS = 1000 / 60;

    private enum DockAction {
        ACTION_NONE, ACTION_REPLACE, ACTION_PUSH_LEFT, ACTION_PUSH_RIGHT
    }

    private class DragChange {
        private DockAction action;
        private int index;

        public DragChange(DockAction action, int index) {
            this.action = action;
            this.index = index;
        }

        public DockAction getAction() {
            return action;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof DragChange && ((DragChange) o).getAction().equals(this.getAction())
                    && ((DragChange) o).getIndex() == this.getIndex();
        }
    }

    /** Bookkeeping. */

    private Handler mHandler;

    /**
     * A calculated value that represents the most recently calculated decision for the action
     * to performed when a drag is completed.
     */
    private DragChange mDragChange;

    /**
     * Has the view been inited with data?
     */
    private boolean inited = false;

    /**
     * Is their currently an ongoing drag event?
     */
    boolean dragging = false;

    /**
     * Has the view been laid out yet (i.e. are size calculations valid)?
     */
    private boolean laidOut = false;

    /** Drawing data. */
    private Paint iconPaint;
    private Paint tempPaint;
    public Paint getIconPaint(){
        return iconPaint;
    }
    private int flashColor;
    private int whiteColor;

    private enum AnimationReasons {
        /** An animation to move the elements of the dock a given amount. **/
        MOVE_ELEMENTS,
        /** An animation to flash the dock a given color. **/
        FLASH_COLOR
    }

    private List<Pair<AnimationReasons, Long>> reasons = new ArrayList<>();
    Runnable animationHandler;

    /** The represented data. **/
    private List<DockElement> elements;

    /** Calculated values. **/
    private int elementCount;
    public int getElementCount(){
        return elementCount;
    }
    private DisplayMetrics cachedMetrics;
    public DisplayMetrics getCachedMetrics(){
        return cachedMetrics;
    }
    private int perElementWidth;
    public int getPerElementWidth(){
        return perElementWidth;
    }
    private int iconSize;
    public int getIconSize(){
        return iconSize;
    }

    public DockView(Context context) {
        super(context);
    }

    public DockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public synchronized void init(int elementCount, List<DockElement> elements){
        if(inited) return;

        this.elements = elements;
        this.elementCount = elementCount;
        inited = true;
        this.mHandler = new Handler();

        if(laidOut){
            invalidate();
        } else {
            requestLayout();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!inited || !laidOut) return;

        //Draw 'animations'
        List<Pair<AnimationReasons, Long>> toRemove = new ArrayList<>();
        for(Pair<AnimationReasons, Long> reason : reasons){
            //Log.d(TAG, "For reason " + reason.first.name() + " w/ " + reason.second + " at " + System.currentTimeMillis() + "...");

            switch(reason.first){
                case MOVE_ELEMENTS:
                    if(reason.second > System.currentTimeMillis() - DOCK_MOVEMENT_ANIMATION_DURATION_MS){
                        //Our standard element draw procedure will capture this... weirdness.
                        //Log.d(TAG, "Drawing dock movement animation...");
                    } else {
                        Log.d(TAG, "Removing an expired dock movement animation; committing all current positions");

                        for(DockElement de : elements){
                            if(de.getIndex() != touchIndex) de.commitMovement();
                        }
                        elements.get(touchIndex).commitMovement();

                        if(mDragChange.getAction() == DockAction.ACTION_REPLACE){ //We actually have to hollow out the replaced element, and move it to the position of what we moved
                            elements.get(mDragChange.getIndex()).hollowOut(touchIndex);
                        }

                        //Reorder the list to match the new positions of the elements
                        Collections.sort(elements, DockElement.comparator);

                        toRemove.add(reason);
                    }
                    break;
                case FLASH_COLOR:
                    if(reason.second > System.currentTimeMillis() - DOCK_FLASH_ANIMATION_DURATION_MS){
                        Log.d(TAG, "Drawing ongoing flash animation; second is " + reason.second);

                        //0% at 0ms, 100% at 250ms, 0% at 500ms
                        long diff = (reason.second + DOCK_FLASH_ANIMATION_DURATION_MS) - System.currentTimeMillis();
                        int newFlashColor = flashColor;
                        if(diff < 250) {
                            newFlashColor = Utilities.colorWithMutedAlpha(flashColor, diff /
                                    ((float) (DOCK_FLASH_ANIMATION_DURATION_MS / 2)));
                        } else {
                            newFlashColor = Utilities.colorWithMutedAlpha(flashColor, (DOCK_FLASH_ANIMATION_DURATION_MS - diff) /
                                    ((float) (DOCK_FLASH_ANIMATION_DURATION_MS / 2)));
                        }

                        Log.d(TAG, "Diff is " + diff + "; color is " + newFlashColor);

                        tempPaint.setColor(newFlashColor);
                        canvas.drawRect(0, 0, getWidth(), getHeight(), tempPaint);
                    } else {
                        Log.d(TAG, "Removing an expired flash animation");
                        toRemove.add(reason);
                    }
                    break;
                default:
                    Log.w(TAG,"Unmatched reason!");
                    break;
            }
        }
        reasons.removeAll(toRemove);

        //Draw icons
        for(DockElement de : elements){
            if(!dragging || (dragging && de.getIndex() != touchIndex)) de.draw(canvas);
        }

        if(dragging){
            elements.get(touchIndex).draw(canvas);
        }
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
        if(!inited) return;

        //Calculated needed data
        this.iconPaint = new Paint();
        this.iconPaint.setAntiAlias(true);
        this.tempPaint = new Paint();
        this.tempPaint.setAntiAlias(true);
        this.whiteColor = getResources().getColor(R.color.white);

        this.cachedMetrics = getContext().getResources().getDisplayMetrics();
        int width = cachedMetrics.widthPixels;
        this.perElementWidth = width / elementCount;
        this.iconSize = perElementWidth > getHeight() ? getHeight() : perElementWidth;

        laidOut = true;

        for(DockElement de : elements){
            de.attachDockView(this);
        }
        invalidate();
    }

    long startTouchEventTime = -1L;
    int touchIndex = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                //Figure out which index we're touching
                for(int i = 0; i < elementCount; i++){
                    int startLocation = i * perElementWidth;
                    int endLocation = (i + 1) * perElementWidth;
                    if(event.getRawX() >= startLocation && event.getRawX() <= endLocation){
                        touchIndex = i;
                        break;
                    }
                }

                startTouchEventTime = System.currentTimeMillis();

                //Start dragging after 500ms
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(startTouchEventTime == -1L) return;

                        startTouchEventTime = -1L;

                        dragging = true;
                        invalidate();
                    }
                }, 500);
                return true;
            case MotionEvent.ACTION_UP:
                if(startTouchEventTime != -1L) {
                    elements.get(touchIndex).launch(this);
                    startTouchEventTime = -1L;
                    Palette.from(elements.get(touchIndex).getIcon()).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            if (palette.getVibrantSwatch() != null) {
                                Log.d(TAG, "Generated vibrant palette; " + palette.getVibrantSwatch().getRgb());
                                flashColor = palette.getVibrantSwatch().getRgb();
                            } else if (palette.getMutedSwatch() != null) {
                                Log.d(TAG, "Generated muted palette; " + palette.getMutedSwatch().getRgb());
                                flashColor = palette.getMutedSwatch().getRgb();
                            } else {
                                Log.d(TAG, "Unable to generate palette...");
                                flashColor = whiteColor;
                            }
                            playAnimation(AnimationReasons.FLASH_COLOR);
                        }
                    });
                    return true;
                } else {
                    if(dragging) {
                        //Tell the dragged element that it isn't being dragged anymore
                        elements.get(touchIndex).indicateDragEnd();

                        //Make sure nothing is colored anymore
                        for(DockElement de : elements) de.indicateNotHoveredOver();

                        //Note: Committing drag events occurs in the animation handlers

                        invalidate();
                        return true;
                    } else {
                        return false;
                    }
                }
            case MotionEvent.ACTION_CANCEL: //Our touch event has been stolen! Eep!
                return false;
            case MotionEvent.ACTION_MOVE:
                if(dragging){
                    handleInternalDrag((int) event.getRawX());
                }
                return true;
            default:
                return false;
        }
    }

    private void playAnimation(AnimationReasons reason){
        //Remove existing, if needed
        Pair<AnimationReasons, Long> toRemove = null;
        for(Pair<AnimationReasons, Long> r : reasons){
            if(r.first.equals(reason)) toRemove = r;
        }

        if(toRemove != null) reasons.remove(toRemove);

        //Add new reason
        reasons.add(new Pair<>(reason, System.currentTimeMillis()));

        //Start animation handler
        animationHandler = new Runnable() {
            @Override
            public void run() {
                if(!reasons.isEmpty()){
                    invalidate();
                    mHandler.postDelayed(animationHandler, FRAME_TIME_MS); //Yay pointers?
                }
            }
        };
        invalidate();
        mHandler.postDelayed(animationHandler, FRAME_TIME_MS);
    }

    public void handleInternalDrag(int xPosition){
        elements.get(touchIndex).indicateDragMotion(xPosition);

        //Try and figure this nonsense out
        boolean isReplacement = false;
        boolean isPushLeft = false;
        boolean isPushRight = false;

        int location = -1;
        int spaceLocation = -1;
        for(int i = 0; i < elementCount; i++){
            int startLocation = i * perElementWidth;
            int endLocation = (i + 1) * perElementWidth;
            if(xPosition >= startLocation && xPosition <= endLocation){
                //Okay, this is where we're calculating
                location = i;

                float percent = ((float) (xPosition - startLocation)) / perElementWidth;
                Log.d(TAG, "Percent is " + percent);
                if(percent < 0.2) {
                    if(location != touchIndex) {
                        //If the spot we're over is empty, it's always a replacement, never a push
                        if (!elements.get(location).isValid()) {
                            isReplacement = true;
                        } else {
                            for (int j = location + 1; j < elementCount; j++) {
                                if (!elements.get(j).isValid()) {
                                    spaceLocation = j;
                                    break;
                                }
                            }

                            if (spaceLocation != -1) {
                                isPushRight = true;
                            }
                        }
                    }
                } else if (percent < 0.8) {
                    if(location != touchIndex){
                        isReplacement = true;
                    }
                } else {
                    if(location != touchIndex) {
                        //If the spot we're over is empty, it's always a replacement, never a push
                        if (!elements.get(location).isValid()) {
                            isReplacement = true;
                        } else {
                            for (int j = location - 1; j >= 0; j--) {
                                if (!elements.get(j).isValid()) {
                                    spaceLocation = j;
                                    break;
                                }
                            }

                            if (spaceLocation != -1) {
                                isPushLeft = true;
                            }
                        }
                    }
                }
                break;
            }
        }

        DragChange previousDrag = mDragChange;
        DragChange newDrag;

        if(isReplacement) {
            newDrag = new DragChange(DockAction.ACTION_REPLACE, location);
        } else if (isPushLeft) {
            newDrag = new DragChange(DockAction.ACTION_PUSH_LEFT, location);
        } else if (isPushRight) {
            newDrag = new DragChange(DockAction.ACTION_PUSH_RIGHT, location);
        } else { //Is invalid
            newDrag = new DragChange(DockAction.ACTION_NONE, -1);
        }

        if(previousDrag == null || !newDrag.equals(previousDrag)) {
            Log.d(TAG, "New drag state for movement at " + xPosition);

            //Changes have happened; queue new movements
            mDragChange = newDrag;

            switch(mDragChange.getAction()){
                case ACTION_PUSH_LEFT: //Queue motion
                    Log.d(TAG, "Element at " + touchIndex + " is pushing left from " + location + " to " + spaceLocation);

                    for(int j = location; j > spaceLocation; j--){
                        elements.get(j).queueMovementTo(j - 1);
                    }
                    elements.get(touchIndex).queueMovementTo(location);
                    elements.get(spaceLocation).queueMovementTo(touchIndex);
                    playAnimation(AnimationReasons.MOVE_ELEMENTS);
                    break;
                case ACTION_PUSH_RIGHT: //Queue motion
                    Log.d(TAG, "Element at " + touchIndex + " is pushing right from " + location + " to " + spaceLocation);

                    for(int j = location; j < spaceLocation; j++){
                        elements.get(j).queueMovementTo(j + 1);
                    }
                    elements.get(touchIndex).queueMovementTo(location);
                    elements.get(spaceLocation).queueMovementTo(touchIndex);
                    playAnimation(AnimationReasons.MOVE_ELEMENTS);
                    break;
                case ACTION_REPLACE: //Greenify
                    Log.d(TAG, "Element at " + touchIndex + " is replacing the element at " + location);

                    for(int i = 0; i < elementCount; i++){
                        elements.get(i).queueMovementTo(elements.get(i).getIndex()); //Move to original place
                        elements.get(i).indicateNotHoveredOver();
                    }
                    elements.get(mDragChange.getIndex()).indicateHoveredOver();
                    playAnimation(AnimationReasons.MOVE_ELEMENTS);
                    break;
                case ACTION_NONE:
                    Log.d(TAG, "Element at " + touchIndex + " is back to doing nothing");

                    for(int i = 0; i < elementCount; i++){
                        elements.get(i).queueMovementTo(elements.get(i).getIndex()); //Move to original place
                        elements.get(i).indicateNotHoveredOver();
                    }
                    break;
            }
        } else {
            Log.d(TAG, "No special case for movement at " + xPosition);
        } //Existing animations should be fine to continue as they were

        invalidate();
    }

    public void handleExternalDrag(int dragPosition){

    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        //TODO: Wire up to handle external drag, which will have similar (but different) logic to handle internal drag
        //Or... maybe not? We could save a lot of time by just drawing "(+)" in the empty spots and letting tapping on those
        //suggest replacements -- maybe releasing where you started draws a " x " in the dock element's location, which you can
        //tap to remove (e.g. "nope no(X)pe nope nope", "no(x)pe nope nope nope" -- weird and fun?)

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

        return true;
    }
}
