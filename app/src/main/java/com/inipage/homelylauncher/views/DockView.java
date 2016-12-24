package com.inipage.homelylauncher.views;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DockView extends View {
    public static final String TAG = "DockView";

    public static final long DOCK_FLASH_ANIMATION_DURATION_MS = 500;

    private static final long FRAME_TIME_MS = 1000 / 60;

    public float HORIZONTAL_MARGIN;
    public float ICON_INTERNAL_MARGIN;

    private Drawable iconDrawable;
    private PorterDuffColorFilter greenFilter;

    public static interface DockViewHost {
        void onElementRemoved(DockElement oldElement, int index);
        void onElementReplaced(DockElement oldElement, DockElement newElement, int index);
    }

    public static class DockElement {
        public static final String TAG = "DockElement";

        public static final Comparator<DockElement> comparator = new Comparator<DockElement>() {
            @Override
            public int compare(DockElement lhs, DockElement rhs) {
                return lhs.getIndex() - rhs.getIndex();
            }
        };

        /**
         * The activity represented by this dock element.
         */
        private ComponentName activity;

        /*
         * The title of the activity represented by this dock element.
         */
        private String title;

        /*
         * The icon of the activity represented by this dock element.
         */
        private Bitmap icon;

        /*
         * The index (i.e. ordering) of this particular dock element.
         */
        private int index;

        public DockElement(ComponentName activity, String title, int index) {
            this.activity = activity;
            this.title = title;
            this.icon = IconCache.getInstance().dummyBitmap;
            this.index = index;

            if(index < 0) Log.wtf(TAG, "INDEX VALUES ARE NEGATIVE. Failure will occur.");
        }


        public ComponentName getActivity() {
            return activity;
        }

        public String getTitle() {
            return title;
        }

        public Bitmap getIcon() {
            return icon;
        }

        public int getIndex() {
            return index;
        }

        /**
         * Launch the app shown here.
         */
        public void launch(DockView dockView){
            if(!isValid()) return;

            try {
                Intent i = new Intent();
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setComponent(activity);
                dockView.getContext().startActivity(i);
            } catch (ActivityNotFoundException anfe){
                Toast.makeText(dockView.getContext(), R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            }
        }

        /*
        public void draw(Canvas c){
            if(icon == null || !isValid()) return;

            int startX = calculateLocation();
            int startY = (int) ((dockView.getHeight() > dockView.getIconSize() ? (dockView.getHeight() - dockView.getIconSize()) / 2 : 0) - dockView.ICON_INTERNAL_MARGIN);

            Log.d(TAG, "Calculated at " + startX + ", " + startY);

            if(isBeingHoveredOver) dockView.getIconPaint().setColorFilter(greenFilter);

            c.drawBitmap(getIcon(),
                    new Rect(0,
                            0,
                            getIcon().getWidth(),
                            getIcon().getHeight()),
                    new Rect((int) (startX + dockView.ICON_INTERNAL_MARGIN),
                            (int) (startY + dockView.ICON_INTERNAL_MARGIN),
                            (int) (startX + dockView.ICON_INTERNAL_MARGIN + dockView.getIconSize()),
                            (int) (startY + dockView.ICON_INTERNAL_MARGIN + dockView.getIconSize())),
                    dockView.getIconPaint());

            dockView.getIconPaint().setColorFilter(null);
        }
        */

        public boolean isValid(){
            return activity != null;
        }

        public void setIcon(Bitmap icon) {
            this.icon = icon;
        }
    }

    /** Bookkeeping. */

    private DockViewHost host;

    private Handler mHandler;
    /**
     * Has the view been inited with data?
     */
    private boolean inited = false;

    /**
     * Has the view been laid out yet (i.e. are size calculations valid)?
     */
    private boolean laidOut = false;

    private boolean iconsFetched = false;

    /** Drawing data. */
    private Paint iconPaint;
    private Paint tempPaint;
    public Paint getIconPaint(){
        return iconPaint;
    }
    private int flashColor;
    private int whiteColor;

    private boolean isDragging = false;
    private int dragIndex = -1;

    private enum AnimationReasons {
        /** An animation to flash the dock a given color. **/
        FLASH_COLOR,
        /** A remove circle. **/
        TAP_REMOVE,
        /** A circle appearing behind a tapped icon. **/
        TAP_CIRCLE
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

    public synchronized void init(int elementCount, List<DockElement> elements, DockViewHost host){
        if(inited) return;

        this.elements = elements;
        this.elementCount = elementCount;
        inited = true;
        this.host = host;
        this.iconDrawable = getResources().getDrawable(R.drawable.ic_add_circle_outline_white_48dp);
        greenFilter = new PorterDuffColorFilter(Color.parseColor("#8000ff00"), PorterDuff.Mode.SRC_ATOP);
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

        //Draw 'animations' -- this seems like re-inventing the wheel; maybe there's a better (less heavy data structure reliant)
        //way to do this...
        List<Pair<AnimationReasons, Long>> toRemove = new ArrayList<>();
        for(Pair<AnimationReasons, Long> reason : reasons){
            //Log.d(TAG, "For reason " + reason.first.name() + " w/ " + reason.second + " at " + System.currentTimeMillis() + "...");

            switch(reason.first){
                case TAP_CIRCLE:
                    if(touchIndex == -1 || startTouchEventTime == -1) continue; //Invalid state

                    Rect circleCenter = getIconBoundsForIndex(touchIndex);
                    float circlePercent = ((System.currentTimeMillis() - startTouchEventTime) / 500F);
                    Log.d(TAG, "Circle percent: " + circlePercent);
                    tempPaint.setColor(Color.WHITE);
                    canvas.drawCircle(circleCenter.centerX(), circleCenter.centerY(), circlePercent * circleCenter.width() / 2, tempPaint);
                    break;
                case TAP_REMOVE:
                    if(touchIndex == -1 || !longPressing) continue; //Invalid state

                    Rect removeCenter = getIconBoundsForIndex(touchIndex);
                    tempPaint.setColor(Color.RED);
                    canvas.drawCircle(removeCenter.centerX(), removeCenter.centerY(), removeCenter.width() / 2, tempPaint);
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
        stopAnimations(toRemove);

        //Draw icons
        if(isDragging) {
            for(int i = 0; i < elementCount; i++){
                DockElement match = null;
                for(DockElement el : elements){
                    if(el.getIndex() == i){
                        match = el;
                        break;
                    }
                }



                if(match != null && match.isValid()){
                    if(i == dragIndex){
                        iconPaint.setColorFilter(greenFilter);
                    }
                    canvas.drawBitmap(match.getIcon(),
                            new Rect(0, 0, match.getIcon().getWidth(), match.getIcon().getHeight()),
                            getIconBoundsForIndex(i),
                            iconPaint);
                    iconPaint.setColorFilter(null);
                } else {
                    if(i == dragIndex){
                        iconDrawable.setColorFilter(greenFilter);
                    }
                    iconDrawable.setBounds(getIconBoundsForIndex(i));
                    iconDrawable.draw(canvas);
                    iconDrawable.setColorFilter(null);
                }
            }
        } else {
            if(touchIndex != -1) {
                if (startTouchEventTime != -1) { //<500ms after first touch; draw circle around touch

                } else if (longPressing) { //Draw red around touch touch

                }
            }

            for (DockElement de : elements) {
                if (!de.isValid() || de.getIcon() == null) continue;

                canvas.drawBitmap(de.getIcon(),
                        new Rect(0, 0, de.getIcon().getWidth(), de.getIcon().getHeight()),
                        getIconBoundsForIndex(de.getIndex()),
                        iconPaint);
            }
        }
    }



    private Rect getBoundsForIndex(int index){
        int xStart = (int) (HORIZONTAL_MARGIN + (index * perElementWidth));;
        int yStart = 0;
        int xEnd = (int) (HORIZONTAL_MARGIN + ((index + 1) * perElementWidth));
        int yEnd = getHeight();

        return new Rect(xStart, yStart, xEnd, yEnd);
    }

    private int calculateIndexForTouch(int xLocation, boolean verifyValid){
        for(int i = 0; i < elementCount; i++){
            Rect indexBound = getBoundsForIndex(i);
            Log.d(TAG, "xLocation= " + xLocation + "; bounds=" + indexBound.toString());
            if(indexBound.left <= xLocation && indexBound.right >= xLocation){
                if(verifyValid){
                    for(DockElement de : elements){
                        if(de.getIndex() == i) return i;
                    }
                    return -1;
                } else {
                    return i;
                }
            }
        }

        return -1;
    }

    private Rect getIconBoundsForIndex(int index){
        int startLocation = (int) (HORIZONTAL_MARGIN + (index * perElementWidth));
        int endLocation = (int) (HORIZONTAL_MARGIN + ((index + 1) * perElementWidth));

        int xStart = endLocation - (perElementWidth / 2) - (iconSize / 2);
        int xEnd = xStart + iconSize;
        int yStart = getHeight() - (getHeight() / 2) - (iconSize / 2);
        int yEnd = yStart + iconSize;

        return new Rect(xStart, yStart, xEnd, yEnd);
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
        this.HORIZONTAL_MARGIN = com.inipage.homelylauncher.utils.Utilities.convertDpToPixel(16F, getContext());
        this.ICON_INTERNAL_MARGIN = com.inipage.homelylauncher.utils.Utilities.convertDpToPixel(8F, getContext());
        this.whiteColor = getResources().getColor(R.color.white);

        this.cachedMetrics = getContext().getResources().getDisplayMetrics();
        int width = cachedMetrics.widthPixels;
        this.perElementWidth = (int) (width - (HORIZONTAL_MARGIN * 2)) / elementCount;
        this.iconSize = perElementWidth > getHeight() ? (int) (getHeight() - (2 * ICON_INTERNAL_MARGIN)) : (int) (perElementWidth - (2 * ICON_INTERNAL_MARGIN));

        //Log.d(TAG, "horizontal margin=" + HORIZONTAL_MARGIN + "; icon internal margin=" + ICON_INTERNAL_MARGIN + "; perElementWidth=" + perElementWidth + "; iconSize=" + iconSize);

        laidOut = true;

        if(!iconsFetched){
            for(DockElement el : elements){
                if(!el.isValid()) continue;

                final DockElement element = el;
                Log.d(TAG, "Trying to fetch icon for " + element.getActivity());
                Bitmap cachedCopy = IconCache.getInstance().getAppIcon(
                        element.getActivity().getPackageName(),
                        element.getActivity().getClassName(),
                        IconCache.IconFetchPriority.DOCK_ICONS,
                        getHeight(),
                        new IconCache.ItemRetrievalInterface() {
                            @Override
                            public void onRetrievalComplete(Bitmap result) {
                                Log.d(TAG, "Setting icon for element with package " + element.getActivity().getPackageName());
                                element.setIcon(result);
                                invalidate();
                            }
                        });

                element.setIcon(cachedCopy);
                invalidate();
            }
            iconsFetched = true;
        }

        invalidate();
    }

    long startTouchEventTime = -1L;
    int touchIndex = -1;
    boolean longPressing = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touchIndex = calculateIndexForTouch((int) event.getX(), true);
                Log.d(TAG, "Got touch at " + touchIndex);
                if(touchIndex == -1){
                    return false;
                }

                startTouchEventTime = System.currentTimeMillis();

                playAnimation(AnimationReasons.TAP_CIRCLE);
                //Start dragging after 500ms
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(startTouchEventTime == -1L) return;

                        startTouchEventTime = -1L;
                        longPressing = true;

                        clearTouchAnimations();
                        playAnimation(AnimationReasons.TAP_REMOVE);
                    }
                }, 500);
                return true;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onActionUp");

                if(startTouchEventTime != -1L) {
                    DockElement touchedApp = null;
                    for(DockElement de : elements){
                        if(de.getIndex() == touchIndex){
                            touchedApp = de;
                            break;
                        }
                    }

                    if(touchedApp == null){
                        return true;
                    }

                    touchedApp.launch(this);
                    startTouchEventTime = -1L;
                    Palette.from(touchedApp.getIcon()).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            if (palette.getVibrantSwatch() != null) {
                                flashColor = palette.getVibrantSwatch().getRgb();
                            } else if (palette.getMutedSwatch() != null) {
                                flashColor = palette.getMutedSwatch().getRgb();
                            } else {
                                flashColor = whiteColor;
                            }
                            playAnimation(AnimationReasons.FLASH_COLOR);
                        }
                    });
                    clearTouchAnimations();
                    return true;
                } else {
                    clearTouchAnimations();

                    if(longPressing) {
                        DockElement choice = null;
                        for(DockElement de : elements) {
                            if(de.getIndex() == touchIndex){
                                choice = de;
                                break;
                            }
                        }
                        final DockElement toRemove = choice;
                        if(toRemove == null) return false;

                        new MaterialDialog.Builder(getContext())
                                .title(R.string.remove_dock_item)
                                .content(getContext().getString(R.string.remove_dock_item_message, toRemove.getTitle()))
                                .positiveText(R.string.yes)
                                .negativeText(R.string.cancel)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        elements.remove(toRemove);
                                        host.onElementRemoved(toRemove, touchIndex);
                                        invalidate();
                                    }
                                }).show();
                        return true;
                    } else {
                        return false;
                    }
                }
            case MotionEvent.ACTION_CANCEL: //Our touch event has been stolen! Eep!
                Log.d(TAG, "onActionCancel");
                startTouchEventTime = -1L;
                longPressing = false;
                clearTouchAnimations();
                return false;
            case MotionEvent.ACTION_MOVE: //HA! We don't care anymore.
                return true;
            default:
                return false;
        }
    }

    private void clearTouchAnimations(){
        List<Pair<AnimationReasons, Long>> toRemove = new ArrayList<>();
        for(Pair<AnimationReasons, Long> reason : reasons){
            if(reason.first == AnimationReasons.TAP_CIRCLE || reason.first == AnimationReasons.TAP_REMOVE) toRemove.add(reason);
        }
        stopAnimations(toRemove);
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

    private void stopAnimations(List<Pair<AnimationReasons, Long>> toRemove) {
        reasons.removeAll(toRemove);
        if(reasons.isEmpty()){
            mHandler.removeCallbacks(animationHandler);
        }
        invalidate();
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch(event.getAction()){
            case DragEvent.ACTION_DRAG_STARTED:
                isDragging = true;
                invalidate();
                return true; //Nothing to do
            case DragEvent.ACTION_DRAG_LOCATION:
                dragIndex = calculateIndexForTouch((int) event.getX(), false);
                invalidate();
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                dragIndex = -1;
                invalidate();
                return true;
            case DragEvent.ACTION_DROP:
                try {
                    ApplicationIcon ai = (ApplicationIcon) event.getLocalState();
                    ComponentName cn = new ComponentName(ai.getPackageName(), ai.getActivityName());
                    String label = getContext().getPackageManager().getActivityInfo(cn, 0).loadLabel(getContext().getPackageManager()).toString();
                    DockElement newElement = new DockElement(cn, label, dragIndex);
                    DockElement toRemove = null;
                    for(DockElement de : elements){
                        if(de.getIndex() == dragIndex){
                            toRemove = de;
                            break;
                        }
                    }
                    if(toRemove != null) elements.remove(toRemove);
                    elements.add(newElement);
                    host.onElementReplaced(toRemove, newElement, dragIndex);

                    //Gotta fetch new icon
                    iconsFetched = false;
                    requestLayout();
                    return true;
                } catch (Exception e){
                    return false;
                }
            case DragEvent.ACTION_DRAG_ENDED:
                isDragging = false;
                invalidate();
                return true;
        }

        return true;
    }
}
