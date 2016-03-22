package com.inipage.homelylauncher.views;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.icons.IconCache;

import java.util.Comparator;

public class DockElement {
    public static final String TAG = "DockElement";

    public static final Comparator<DockElement> comparator = new Comparator<DockElement>() {
        @Override
        public int compare(DockElement lhs, DockElement rhs) {
            return lhs.getIndex() - rhs.getIndex();
        }
    };

    private ColorFilter redFilter;
    private ColorFilter greenFilter;

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

    /**
     * The starting time of its movement.
     */
    private long startMovementTime;

    /**
     * The dock that this element is currently attached to.
     */
    private DockView dockView;

    /**
     * The expected end index.
     */
    private int queuedIndex;

    /**
     * The last calculated location of the icon.
     */
    private int lastCalculatedLocation;

    /**
     * The current X location of the drag.
     */
    private int currentDragX = -1;

    /**
     * Whether or not something is being held over the icon.
     */
    private boolean isBeingHoveredOver = false;

    public DockElement(ComponentName activity, String title, int index) {
        this.activity = activity;
        this.title = title;
        this.icon = IconCache.getInstance().dummyBitmap;
        this.index = index;
        this.dockView = null;

        this.startMovementTime = -1;
        this.queuedIndex = -1;
        this.lastCalculatedLocation = -1;

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

    public void attachDockView(DockView dockView){
        this.dockView = dockView;

        redFilter = new PorterDuffColorFilter(this.dockView.getResources().getColor(R.color.red),
                PorterDuff.Mode.SRC_IN);
        greenFilter = new PorterDuffColorFilter(this.dockView.getResources().getColor(R.color.green),
                PorterDuff.Mode.SRC_IN);

        //Fetch the icon
        if(!isValid()) return;

        Bitmap b = IconCache.getInstance().getSwipeCacheAppIcon(activity.getPackageName(), activity.getClassName(),
                dockView.getHeight(), new IconCache.ItemRetrievalInterface() {
                    @Override
                    public void onRetrievalStarted() {}

                    @Override
                    public void onRetrievalComplete(Bitmap result) {
                        DockElement.this.icon = result;
                        if(DockElement.this.dockView != null) DockElement.this.dockView.invalidate();
                    }
                });
        this.icon = b;
        this.dockView.invalidate();
    }

    /**
     * Calculate the location of this element.
     * @return The offset.
     */
    private int calculateLocation(){
        if(dockView == null) return -1;

        if(lastCalculatedLocation == -1){
            int startLocation = 0;
            startLocation += (dockView.getPerElementWidth() * index);
            startLocation += ((dockView.getPerElementWidth() - dockView.getIconSize()) / 2); //We draw in the middle of our draw space

            this.lastCalculatedLocation = startLocation;
        }
        if(queuedIndex == -1 || startMovementTime == -1L) return lastCalculatedLocation; //Old cached value is fine

        //Animations always between lastCalculatedLocation and newLocation
        int newLocation = 0;
        newLocation += (dockView.getPerElementWidth() * index);
        newLocation += ((dockView.getPerElementWidth() - dockView.getIconSize()) / 2); //We draw in the middle of our draw space
        lastCalculatedLocation = newLocation;

        //(1) Find percent our animation is complete
        float percentComplete = ((float) (System.currentTimeMillis() - startMovementTime)) / DockView.DOCK_MOVEMENT_ANIMATION_DURATION_MS;

        if(percentComplete < 0){
            Log.wtf(TAG, "startMovementTime is invalid! Critical failures will likely follow.");
            return Integer.MAX_VALUE; //May as well fail spectacularly!
        } else if (percentComplete > 1){
            Log.d(TAG, "Animation complete; checking if committal valid...");
            if(queuedIndex == index){
                Log.d(TAG, "Already cleared this value; terminating animation");

                lastCalculatedLocation = newLocation;
                startMovementTime = -1;
                queuedIndex = -1;

                return newLocation;
            } else {
                Log.d(TAG, "calculateExpectedLocation(...) still based on different index than queuedIndex; returning max offset");
                return newLocation;
            }
        } else { //In the middle of a perfectly valid animation; continue
            Log.d(TAG, "Interpolating between " + lastCalculatedLocation + " and " + newLocation + " ; at " + percentComplete + "%");
            return (int) (lastCalculatedLocation + (percentComplete * (newLocation - lastCalculatedLocation)));
        }
    }

    /**
     * Queue up the animation to move to a new index. This MUST be called before commitMovementTo
     * or else the display won't reflect the dock's dataset.
     * @param newIndex This elements new location.
     */
    public void queueMovementTo(int newIndex){
        Log.d(TAG, "Queueing movement for " + title + " from " + index + " to " + newIndex);

        startMovementTime = System.currentTimeMillis();
        queuedIndex = newIndex;
    }

    /**
     * Commit movement to a location.
     */
    public void commitMovement(){
        if(queuedIndex == -1){
            Log.w(TAG, "Tried to commit an unqueued move for " + title);
            return;
        }

        Log.d(TAG, "Moving " + title + " from " + index + " to " + queuedIndex);
        this.index = queuedIndex;
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

    /**
     * Draw this dock element to a canvas representing the dock.
     * @param c The canvas.
     */
    public void draw(Canvas c){
        if(icon == null || !isValid()) return;

        if(currentDragX == -1) {
            int startX = calculateLocation();
            int startY = (dockView.getHeight() > dockView.getIconSize() ? (dockView.getHeight() - dockView.getIconSize()) / 2 : 0);

            if(isBeingHoveredOver) dockView.getIconPaint().setColorFilter(greenFilter);

            c.drawBitmap(getIcon(), new Rect(0, 0, getIcon().getWidth(), getIcon().getHeight()),
                    new Rect(startX, startY, startX + dockView.getIconSize(), startY + dockView.getIconSize()),
                    dockView.getIconPaint());

            dockView.getIconPaint().setColorFilter(null);
        } else { //Draw the icon in drag state
            int startX = currentDragX - (dockView.getIconSize() / 2);
            int startY = (dockView.getHeight() > dockView.getIconSize() ? (dockView.getHeight() - dockView.getIconSize()) / 2 : 0);

            dockView.getIconPaint().setAlpha(180);
            c.drawBitmap(getIcon(), new Rect(0, 0, getIcon().getWidth(), getIcon().getHeight()),
                    new Rect(startX, startY, startX + dockView.getIconSize(), startY + dockView.getIconSize()),
                    dockView.getIconPaint());
            dockView.getIconPaint().setAlpha(255);
        }
    }

    public boolean isValid(){
        return activity != null;
    }

    public void indicateDragMotion(int touchX) {
        currentDragX = touchX;
    }

    public void indicateDragEnd(){
        currentDragX = -1;
    }

    public void indicateHoveredOver(){
        this.isBeingHoveredOver = true;
    }

    public void indicateNotHoveredOver(){
        this.isBeingHoveredOver = false;
    }

    public void hollowOut(int replacingElementsIndex) {
        this.activity = null;
        this.title = null;
        this.icon = IconCache.getInstance().dummyBitmap;
        this.index = replacingElementsIndex;

        this.startMovementTime = -1;
        this.queuedIndex = -1;
        this.lastCalculatedLocation = -1;
    }
}
