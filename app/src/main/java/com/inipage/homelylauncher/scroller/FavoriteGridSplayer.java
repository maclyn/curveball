package com.inipage.homelylauncher.scroller;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.inipage.homelylauncher.DatabaseHelper;
import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.model.Favorite;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class for doing our grid. Due to the requirements of the grid, neither
 * GridLayout nor RecyclerViews with various layout managers are a good fit -
 * the "object can span multiple rows and columns" is a serious issue for both.
 * Thus, I hope this is more comprehensible and maintainable even if it has to
 * use a (shudder) AbsoluteLayout to do positioning (I could use a "custom
 * layout" or whatever, but AbsoluteLayout really, really fits the bill -- I'd
 * just be using a custom layout for the sake of _not_ using an AbsoluteLayout
 * and wind up replicating all of its logic).
 */
public class FavoriteGridSplayer implements View.OnDragListener {
    public static final String TAG = "FavoriteGridSplayer";

    private static final int ANIMATION_DURATION = 500;
    private static final int MAXIMUM_HEIGHT = 100;

    private Context ctx;
    private AbsoluteLayout layout; /** I have a really good reason for this. **/
    private List<Favorite> favoritesRef;
    private Map<Integer, Favorite> idToFavorite;
    private Map<Favorite, View> favoriteToView;
    private int[][] filledMap;
    private int columnCount;
    private int lowestPoint;
    private FavoriteStateCallback callback;

    public interface FavoriteStateCallback {
        void onFavoritesChanged();
        void requestLaunch(ComponentName cn);
        Activity getActivityContext();
    }

    public FavoriteGridSplayer(final AbsoluteLayout layout, List<Favorite> favorites,
                               FavoriteStateCallback callback, int columnCount){
        this.layout = layout;
        this.ctx = this.layout.getContext();
        this.layout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(left != oldLeft || right != oldRight) //We're only looking for the initial layout call
                    layout(true);
            }
        });
        this.layout.setOnDragListener(this);
        this.callback = callback;
        this.favoritesRef = favorites;
        this.columnCount = columnCount;
        this.lowestPoint = 0;
        this.idToFavorite  = new HashMap<>();
        this.favoriteToView = new HashMap<>();

        layout(true);
    }

    /**
     * Layout the items in the internal grid base don where they want to be placed.
     * @param animate Animate the layout (requires existing grid elements).
     */
    private synchronized void layout(final boolean animate){
        //Downsides to changeable columns: theoretically it's possible that, unfortunately, we changed
        //the column count. Now, we could either do one of two things when this happens: drop everything that's
        //out of view or... move them all to the bottom of the screen... or wrap them on to the
        //next row (that's three, actually). I like the third!

        final boolean dragActive = dragTarget != null;

        //(0) Sort everything according to row, and then according to the column it's in
        Collections.sort(favoritesRef, new Comparator<Favorite>() {
            @Override
            public int compare(Favorite o1, Favorite o2) {
                return o1.getPositionY(dragActive && o1 != dragTarget) - o2.getPositionY(dragActive && o2 != dragTarget);
            }
        });
        Collections.sort(favoritesRef, new Comparator<Favorite>() {
            @Override
            public int compare(Favorite o1, Favorite o2) {
                return o1.getPositionX(dragActive && o1 != dragTarget) - o2.getPositionX(dragActive && o2 != dragTarget);
            }
        });

        if(dragActive) {
            favoritesRef.remove(dragTarget);
            favoritesRef.add(0, dragTarget); //Put it at the start so its position is respected
        }

        // Also, invalidate filled map
        this.filledMap = new int[MAXIMUM_HEIGHT][columnCount];
        for(int i = 0; i < MAXIMUM_HEIGHT; i++){
            for(int j = 0; j < columnCount; j++){
                filledMap[i][j] = -1;
            }
        }

        //(1) Verify everything is properly placed, and, if not, move the invalid items.
        //This will cause a cascading invalid placement failure, but this is okay as the check
        //will simply solve itself
        idToFavorite.clear();
        lowestPoint = 0;
        for(Favorite f : favoritesRef){
            //First pre-check: are we wider than the space allows?
            if(f.getWidth() > columnCount){
                f.setWidth(columnCount);
            }

            if(f.getContainingFolder() != -1) continue; //You don't matter (here)

            //Second: start placement search from positionX and positionY downward
            int newX = -1;
            int newY = -1;
            placementSearch: {
                for (int i = f.getPositionY(dragActive && f != dragTarget); i < MAXIMUM_HEIGHT; i++) {
                    for (int j = (i == f.getPositionY(dragActive && f != dragTarget) ? f.getPositionX(dragActive && f != dragTarget) : 0); j < columnCount; j++) {
                        if(j + f.getWidth() > columnCount) continue; //Too wide at this spot; let's not waste our time.

                        //Is the starting cell --> size of object okay?
                        boolean hasSpace = true;
                        innerSpaceSearch: {
                            for (int k = f.getPositionY(dragActive && f != dragTarget); k < f.getPositionY(dragActive && f != dragTarget) + f.getHeight(); k++) {
                                for (int l = f.getPositionX(dragActive && f != dragTarget); l < f.getPositionX(dragActive && f != dragTarget) + f.getWidth(); l++){
                                    if(filledMap[i][j] != -1){
                                        hasSpace = false;
                                        break innerSpaceSearch;
                                    }
                                }
                            }
                        }

                        if(hasSpace) {
                            newX = j;
                            newY = i;
                            break placementSearch;
                        } else {} //No space at the selected i, j; continue our sad search
                    }
                }
            }

            //Mark changes complete
            if(newX == -1 || newY == -1){
                throw new RuntimeException("Better crash here 'cuz we ain't got no valid layout...");
            }

            f.setPositionX(newX, !dragActive);
            f.setPositionY(newY, !dragActive);
            for (int i = f.getPositionY(false); i < f.getPositionY(false) + f.getHeight(); i++) {
                for (int j = f.getPositionX(false); j < f.getPositionX(false) + f.getWidth(); j++){
                    filledMap[i][j] = f.getId();
                }

                if(i > lowestPoint) lowestPoint = i;
            }

            idToFavorite.put(f.getId(), f);
        }

        //(2) Trim "holes" in layout
        //Essentially, if we have an entire row without anything we should delete it
        for(int i = 0; i < lowestPoint; i++){
            boolean empty = true;
            for(int j = 0; j < columnCount; j++){
                if(filledMap[i][j] != -1){
                    empty = false;
                    break;
                }
            }

            if(empty){ //This row is useless!
                //Adjust internal representations on Favorites
                for(int row = i; row <= lowestPoint; row++){
                    for(int col = 0; col < columnCount; col++){
                        if(filledMap[row][col] != -1){
                            Favorite fav = idToFavorite.get(filledMap[row][col]);
                            fav.setPositionY(fav.getPositionY(false) - 1, !dragActive && fav != dragTarget);
                        }
                    }
                }

                //Rebuild filledMap with valid data
                for(Favorite f : favoritesRef){
                    if(f.getContainingFolder() != -1) continue;

                    for (int fRow = f.getPositionY(false); fRow < f.getPositionY(false) + f.getHeight(); fRow++) {
                        for (int fCol = f.getPositionX(false); fCol < f.getPositionX(false); fCol++){
                            filledMap[fRow][fCol] = f.getId();
                        }
                    }
                }
                lowestPoint -= 1;
            }
        }

        //TODO: actually only call this when there's a material change to favorite layout
        callback.onFavoritesChanged();

        //(3) Find views for each favorite and move them to their proper place, or create them
        callback.getActivityContext().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int cellDimension = layout.getWidth() / columnCount;
                if(cellDimension < 1) return; //Nothing to do here; no proper layout has occurred

                for(final Favorite f : favoritesRef){
                    if(f.getContainingFolder() != -1) continue; //Don't display "foldered" things

                    View repr = null;
                    boolean existed = false;
                    if(favoriteToView.containsKey(f)){
                        repr = favoriteToView.get(f);
                        existed = true;
                    } else {
                        switch(f.getType()){
                            case DatabaseHelper.FAVORITE_TYPE_APP:
                                repr = LayoutInflater.from(ctx).inflate(R.layout.item_scr_favorite, layout, false);
                                favoriteToView.put(f, repr);
                                break;
                            case DatabaseHelper.FAVORITE_TYPE_SHORTCUT:
                            case DatabaseHelper.FAVORITE_TYPE_WIDGET:
                            case DatabaseHelper.FAVORITE_TYPE_FOLDER:
                                Utilities.throwNotImplemented();
                                break;
                        }

                        switch(f.getType()){
                            case DatabaseHelper.FAVORITE_TYPE_APP:
                                final View finalRepr = repr;
                                ComponentName cn = f.getComponentName();
                                final ImageView favAppIcon = (ImageView) repr.findViewById(R.id.favoriteAppIcon);
                                favAppIcon.setImageBitmap(IconCache.getInstance().getAppIcon(
                                        cn.getPackageName(),
                                        cn.getClassName(),
                                        IconCache.IconFetchPriority.APP_DRAWER_ICONS,
                                        cellDimension,
                                        new IconCache.ItemRetrievalInterface() {
                                            @Override
                                            public void onRetrievalComplete(Bitmap result) {
                                                favAppIcon.setImageBitmap(result);
                                            }
                                        }));
                                repr.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        callback.requestLaunch(f.getComponentName());
                                    }
                                });
                                repr.setOnLongClickListener(new View.OnLongClickListener() {
                                    @Override
                                    public boolean onLongClick(View v) {
                                        //TODO: Add a radial menu to support choosing between resize, move, and delete
                                        v.startDrag(ClipData.newPlainText("", ""), new View.DragShadowBuilder(v), f, 0);

                                /*
                                View container = finalRepr.findViewById(R.id.scrollerItemContainer);
                                ObjectAnimator oa = ObjectAnimator.ofFloat(container, "scaleX", 1.0f, 0.0f);
                                ObjectAnimator oa2 = ObjectAnimator.ofFloat(container, "scaleY", 1.0f, 0.0f);
                                ObjectAnimator oa3 = ObjectAnimator.ofFloat(container, "rotation", 0, 360);
                                AnimatorSet set = new AnimatorSet();
                                set.setDuration(ANIMATION_DURATION);
                                set.playTogether(oa, oa2, oa3);
                                set.addListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        favoritesRef.remove(f);
                                        layout.removeView(finalRepr);
                                        layout(true);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                        favoritesRef.remove(f);
                                        layout.removeView(finalRepr);
                                        layout(true);
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {
                                    }
                                });
                                set.start();
                                */
                                        return true;
                                    }
                                });
                                break;
                            case DatabaseHelper.FAVORITE_TYPE_SHORTCUT:
                            case DatabaseHelper.FAVORITE_TYPE_WIDGET:
                            case DatabaseHelper.FAVORITE_TYPE_FOLDER:
                                Utilities.throwNotImplemented();
                                break;
                        }
                    }

                    //Now layout the repr
                    AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(cellDimension * f.getWidth(),
                            cellDimension * f.getHeight(),
                            cellDimension * f.getPositionX(false),
                            cellDimension * f.getPositionY(false));
                    if(!existed) {
                        repr.setLayoutParams(params);
                        repr.setTag(f);
                        layout.addView(repr);
                        repr.requestLayout();

                        //We also like cute animations -- so we do a scale from 0.2-100% in!
                        if(animate) {
                            View container = repr.findViewById(R.id.scrollerItemContainer);
                            ObjectAnimator oa = ObjectAnimator.ofFloat(container, "scaleX", 0.2f, 1.0f);
                            ObjectAnimator oa2 = ObjectAnimator.ofFloat(container, "scaleY", 0.2f, 1.0f);
                            AnimatorSet set = new AnimatorSet();
                            set.setDuration(ANIMATION_DURATION);
                            set.setInterpolator(new BounceInterpolator());
                            set.playTogether(oa, oa2);
                            set.start();
                        }
                    } else {
                        if(animate){
                            Utilities.animateAbsoluteLayoutChange(repr, params, 250L);
                        } else {
                            repr.setLayoutParams(params);
                        }
                    }
                }
            }
        });
    }

    /**
     * Add an application to the favorites grid. Note that we modify favorites here.
     * @param packageName The app's package name.
     * @param activityName The app's activity.
     * @return Whether or not we successfully added it.
     */
    public boolean addApp(String packageName, String activityName){
        //Place it at a sensible place -- last row or make a new one if we have to
        int lastRowPlace = -1;
        for(int i = 0; i < columnCount; i++){
            if(filledMap[lowestPoint][i] == -1){
                lastRowPlace = i;
                break;
            }
        }

        int newX = 0;
        int newY = 0;
        if(lastRowPlace != -1){
            newY = lowestPoint;
            newX = lastRowPlace;
        } else {
            lowestPoint++;
            newY = lowestPoint;
            newX = 0;
        }

        Favorite f = new Favorite(DatabaseHelper.FAVORITE_TYPE_APP,
                newX,
                newY,
                1,
                1,
                -1,
                packageName,
                activityName,
                -1);
        favoritesRef.add(f);
        layout(true);
        return true;
    }

    Favorite dragTarget = null;
    float dragX = -1;
    float dragY = -1;
    int currentCellX = -1;
    int currentCellY = -1;
    int targetCellX = -1;
    int targetCellY = -1;

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch(event.getAction()){
            case DragEvent.ACTION_DRAG_STARTED:
                Log.d(TAG, "Drag started");
                Log.d(TAG, "dragTarget=" + dragTarget);
                dragTarget = (Favorite) event.getLocalState();
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                dragX = event.getX();
                dragY = event.getY();
                reactToDrag();
                break;
            case DragEvent.ACTION_DROP:
                Log.d(TAG, "Drag ended/dropped");
                Log.d(TAG, "dragTarget=" + dragTarget);
                reactToDrag();

                //If dropped where started, show menu
                if(currentCellX == targetCellX && currentCellY == targetCellY) {
                    Toast.makeText(ctx, "Would menu", Toast.LENGTH_SHORT).show();
                }

                dragTarget = null;
                dragX = -1;
                dragY = -1;
                currentCellX = -1;
                currentCellY = -1;
                targetCellX = -1;
                targetCellY = -1;
                layout(true); //Everything is good; just needs to be "committed" now
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                if(!event.getResult()){ //We need to cancel stuff ourselves
                    dragTarget = null;
                    dragX = -1;
                    dragY = -1;
                    currentCellX = -1;
                    currentCellY = -1;
                    targetCellX = -1;
                    targetCellY = -1;
                }
                break;
        }
        return true;
    }

    private void reactToDrag(){
        //Calculate if the target's in a different cell; if so set update and call
        //layout(true) again
        float actualX = dragX; //no adjustment needed
        float actualY = dragY;

        //"Cell" it should be in is easy enough
        int cellDimension = layout.getWidth() / columnCount;
        currentCellX = (int) Math.floor(actualX / cellDimension);
        currentCellX = (int) Math.floor(actualY / cellDimension);
        if(currentCellY < 0) currentCellY = 0;
        if(targetCellX == -1 && targetCellY == -1){
            targetCellX = currentCellX;
            targetCellY = currentCellY;
        }

        //We actually set the real position on this guy
        if(currentCellX != dragTarget.getPositionX(false) || currentCellY != dragTarget.getPositionY(false)){
            Log.d(TAG, "Change noted; adjusting!!!");
            Log.d(TAG, "Cell position = " + currentCellX + ", " + currentCellY);

            dragTarget.setPositionX(currentCellX, true);
            dragTarget.setPositionY(currentCellY, true);
            layout(true);
        }
    }
}