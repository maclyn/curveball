package com.inipage.homelylauncher.views;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.Comparator;

public class DockElement {
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
