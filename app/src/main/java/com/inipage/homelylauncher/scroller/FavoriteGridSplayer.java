package com.inipage.homelylauncher.scroller;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

import com.inipage.homelylauncher.DatabaseHelper;
import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.model.Favorite;
import com.inipage.homelylauncher.model.FavoriteShadow;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private FavoriteStateCallback callback;
    private int columnCount;

    //All favorites in the grid (including those that are in folders)
    List<Favorite> favRef;
    //A list of favorite positions laid out in the grid (not including those in folders)
    List<FavoriteShadow> shadows;
    //A map of (row, col) -> FavoriteShadow
    FavoriteShadow[][] filledMap;
    //A map of (fav) -> (possibly null) backing View
    Map<Favorite, View> favToView;

    public interface FavoriteStateCallback {
        void onFavoritesChanged();
        void onRendered();
        void requestLaunch(ComponentName cn);
        void attachOverlay(View toAttach, int centerX, int centerY, int width, int height);
        void closeOverlay();
        Activity getActivityContext();
    }

    public FavoriteGridSplayer(final AbsoluteLayout layout,
                               List<Favorite> favorites,
                               FavoriteStateCallback callback,
                               int columnCount){
        this.layout = layout;
        this.ctx = this.layout.getContext();
        this.layout.setOnDragListener(this);
        this.callback = callback;
        this.favRef = favorites;
        this.columnCount = columnCount;
        this.favToView = new HashMap<>();
        this.layout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(left != oldLeft || right != oldRight) { //We're only looking for the initial layout call
                    renderLayout(true);
                }
            }
        });

        designLayout(favRef);
        renderLayout(true);
    }

    /**
     * Add an application to the favorites grid. Note that we modify favorites here.
     * @param packageName The app's package name.
     * @param activityName The app's activity.
     * @return Whether or not we successfully added it.
     */
    public boolean addApp(String packageName, String activityName){
        //Place it at a sensible place -- last row or make a new one if we have to
        int lowestPoint = findLowestPoint(shadows);

        int lastRowPlace = -1;
        for(int i = 0; i < columnCount; i++){
            if(filledMap[lowestPoint][i] == null){
                lastRowPlace = i;
                break;
            }
        }

        int newX;
        int newY;
        if(lastRowPlace != -1){
            newY = lowestPoint;
            newX = lastRowPlace;
        } else {
            newY = lowestPoint + 1;
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
        favRef.add(f);
        designLayout(favRef);
        persistFavorites();
        renderLayout(true);
        return true;
    }

    /**
     * Indicate favorites need to be saved. Works by updating the favorites list, and letting our
     * host context know that list is ready to be persisted.
     */
    private void persistFavorites(){
        //Move data from shadows -> favorites
        for(FavoriteShadow fs : shadows){
            fs.getData().updateFromShadow(fs);
        }
        callback.onFavoritesChanged();
    }

    private FavoriteShadow lookupFavoriteShadow(Favorite f){
        for(FavoriteShadow fs : shadows){
            if(f.getId() == fs.getData().getId()) return fs;
        }
        return null;
    }

    private int findLowestPoint(List<FavoriteShadow> positions){
        int lowestPoint = 0;
        for(FavoriteShadow pos : positions){
            if(pos.getY() > lowestPoint) lowestPoint = pos.getY();
        }
        return lowestPoint;
    }

    private void designLayout(List<Favorite> favorites){
        designLayout(favorites, null, -1, -1);
    }

    /**
     * Create a layout of items in the internal grid base on where they want to be placed.
     */
    private void designLayout(List<Favorite> favorites, Favorite firstToLayout, int xBias, int yBias){
        //Downsides to changeable columns: theoretically it's possible that, unfortunately, we changed
        //the column count. Now, we could either do one of two things when this happens: drop everything that's
        //out of view or... move them all to the bottom of the screen... or wrap them on to the
        //next row (that's three, actually). I like the third!

        List<FavoriteShadow> rValue = new ArrayList<>(favorites.size());

        //(0) Sort everything, putting firstToLayout first when relevant
        for(Favorite f : favorites){
            if(firstToLayout == null || f.getId() != firstToLayout.getId()) rValue.add(new FavoriteShadow(f));
        }

        //(0.5) Sort our list by position
        Collections.sort(rValue, FavoriteShadow.getComparator());
        if(firstToLayout != null){
            FavoriteShadow biasedShadow = new FavoriteShadow(firstToLayout);
            biasedShadow.setX(xBias);
            biasedShadow.setY(yBias);
            rValue.add(0, biasedShadow);
        }

        // Also, invalidate filled map
        filledMap = new FavoriteShadow[MAXIMUM_HEIGHT][columnCount];
        for(int i = 0; i < MAXIMUM_HEIGHT; i++){
            for(int j = 0; j < columnCount; j++){
                filledMap[i][j] = null;
            }
        }

        //(1) Verify everything is properly placed, and, if not, move the invalid items.
        //This will cause a cascading invalid placement failure, but this is okay as the check
        //will simply solve itself
        for(FavoriteShadow f : rValue){
            //First pre-check: are we wider than the space allows?
            if(f.getWidth() > columnCount){
                f.setWidth(columnCount);
            }

            //Second: start placement search from positionX and positionY downward
            int newX = -1;
            int newY = -1;
            placementSearch: {
                for (int i = f.getY(); i < MAXIMUM_HEIGHT; i++) {
                    for (int j = (i == f.getY() ? f.getX() : 0); j < columnCount; j++) {
                        if(j + f.getWidth() > columnCount) continue; //Too wide at this spot; let's not waste our time.

                        //Is the starting cell --> size of object okay?
                        boolean hasSpace = true;
                        innerSpaceSearch: {
                            for (int k = f.getY(); k < f.getY() + f.getHeight(); k++) {
                                for (int l = f.getX(); l < f.getX() + f.getWidth(); l++){
                                    if(filledMap[i][j] != null){
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

            f.setX(newX);
            f.setY(newY);
            for (int i = f.getY(); i < f.getY() + f.getHeight(); i++) {
                for (int j = f.getX(); j < f.getX() + f.getWidth(); j++){
                    filledMap[i][j] = f;
                }
            }
        }

        int lowestPoint = findLowestPoint(rValue);

        //(2) Trim "holes" in layout
        //Essentially, if we have an entire row without anything we should delete it
        for(int i = 0; i < lowestPoint; i++){
            boolean empty = true;
            for(int j = 0; j < columnCount; j++){
                if(filledMap[i][j] != null){
                    empty = false;
                    break;
                }
            }

            if(empty){ //This row is useless!
                //Adjust internal representations on Favorites
                for(int row = i; row <= lowestPoint; row++){
                    for(int col = 0; col < columnCount; col++){
                        if(filledMap[row][col] != null){
                            FavoriteShadow fav = filledMap[row][col];
                            fav.setY(fav.getY() - 1);
                        }
                    }
                }

                //Rebuild filledMap with valid data
                for(FavoriteShadow f : shadows){
                    for (int fRow = f.getY(); fRow < f.getY() + f.getHeight(); fRow++) {
                        for (int fCol = f.getX(); fCol < f.getX(); fCol++){
                            filledMap[fRow][fCol] = f;
                        }
                    }
                }
                lowestPoint -= 1;
            }
        }

        shadows = rValue;
    }


    private void renderLayout(final boolean animate){
        //(3) Find views for each favorite and move them to their proper place, or create them
        callback.getActivityContext().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int cellDimension = layout.getWidth() / columnCount;
                if(cellDimension < 1) return; //Nothing to do here; no proper layout has occurred

                for(final FavoriteShadow fs : shadows){
                    final Favorite f = fs.getData();
                    View repr = null;
                    boolean existed = false;
                    if(favToView.containsKey(f)){
                        repr = favToView.get(f);
                        existed = true;
                    } else {
                        switch(f.getType()){
                            case DatabaseHelper.FAVORITE_TYPE_APP:
                                repr = LayoutInflater.from(ctx).inflate(R.layout.item_scr_favorite, layout, false);
                                favToView.put(f, repr);
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
                                        v.startDrag(ClipData.newPlainText("", ""), new View.DragShadowBuilder(v), f, 0);
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
                    AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(
                            cellDimension * fs.getWidth(),
                            cellDimension * fs.getHeight(),
                            cellDimension * fs.getX(),
                            cellDimension * fs.getY());
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

                callback.onRendered();
            }
        });
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
                dragTarget = (Favorite) event.getLocalState();
                Log.d(TAG, "dragTarget=" + dragTarget);
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
                    showItemMenu(dragTarget);
                }

                dragTarget = null;
                dragX = -1;
                dragY = -1;
                currentCellX = -1;
                currentCellY = -1;
                targetCellX = -1;
                targetCellY = -1;

                persistFavorites();
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

    private void showItemMenu(final Favorite menuTarget){
        Bitmap target = null;

        switch(menuTarget.getType()){
            case DatabaseHelper.FAVORITE_TYPE_APP:
                target = ((BitmapDrawable) ((ImageView) favToView.get(menuTarget).findViewById(R.id.favoriteAppIcon)).getDrawable()).getBitmap();
                break;
            case DatabaseHelper.FAVORITE_TYPE_FOLDER:
            case DatabaseHelper.FAVORITE_TYPE_WIDGET:
            case DatabaseHelper.FAVORITE_TYPE_SHORTCUT:
                Utilities.throwNotImplemented();
                break;
        }

        Palette.from(target).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                int color = Color.BLUE;
                if (palette.getDominantSwatch() != null) {
                    color = palette.getDominantSwatch().getRgb();
                } else if (palette.getDarkVibrantSwatch() != null) {
                    color = palette.getDarkVibrantSwatch().getRgb();
                } else if (palette.getLightVibrantSwatch() != null) {
                    color = palette.getLightVibrantSwatch().getRgb();
                } 
                showItemMenuImpl(menuTarget, color);
            }
        });
    }

    private void showItemMenuImpl(final Favorite menuTarget, int shapeColor) {
        final View target = favToView.get(menuTarget);
        int[] location = new int[2];
        target.getLocationInWindow(location);

        switch(menuTarget.getType()) {
            case DatabaseHelper.FAVORITE_TYPE_APP: {
                View v = LayoutInflater.from(ctx).inflate(R.layout.view_app_opts, null);
                int x = location[0] + (target.getWidth() / 2);
                int y = location[1] + (target.getHeight() / 2);
                Shape ovalShape = new OvalShape();
                ShapeDrawable sd = new ShapeDrawable(ovalShape);
                sd.getPaint().setColor(shapeColor);
                v.findViewById(R.id.app_opts_bg).setBackground(sd);
                int oneTwoEight = (int) Utilities.convertDpToPixel(128, ctx);
                v.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callback.closeOverlay();

                        View container = target.findViewById(R.id.scrollerItemContainer);
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
                                layout.removeView(target);
                                shadows.remove(lookupFavoriteShadow(menuTarget));
                                favRef.remove(menuTarget);
                                favToView.remove(menuTarget);
                                designLayout(favRef);
                                renderLayout(true);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                layout.removeView(target);
                                shadows.remove(lookupFavoriteShadow(menuTarget));
                                favRef.remove(menuTarget);
                                favToView.remove(menuTarget);
                                designLayout(favRef);
                                renderLayout(true);
                                callback.onFavoritesChanged();
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }
                        });
                        set.start();
                    }
                });
                callback.attachOverlay(v, x, y, oneTwoEight, oneTwoEight);
                break;
            }
        }
    }

    private void reactToDrag(){
        //Calculate if the target's in a different cell; if so set update and call
        //layout(true) again
        float actualX = dragX; //no adjustment needed
        float actualY = dragY;

        //"Cell" it should be in is easy enough
        float cellDimension = layout.getWidth() / columnCount;
        currentCellX = (int) Math.floor(actualX / cellDimension);
        currentCellY = (int) Math.floor(actualY / cellDimension);
        if(currentCellY < 0) currentCellY = 0;
        if(currentCellX < 0) currentCellX = 0;
        if(targetCellX == -1 && targetCellY == -1){
            targetCellX = currentCellX;
            targetCellY = currentCellY;
        }

//        Log.d(TAG, "In cell " + currentCellX + "; " + currentCellY);

        //We actually set the position on this guy
        if(currentCellX != dragTarget.getX() || currentCellY != dragTarget.getY()){
            Log.d(TAG, "Change noted; adjusting!!!");
            Log.d(TAG, "Cell position = " + currentCellX + ", " + currentCellY);

            dragTarget.setX(currentCellX);
            dragTarget.setY(currentCellY);

            designLayout(favRef, dragTarget, currentCellX, currentCellY);
            renderLayout(true);
            Log.d(TAG, toString());
        }
    }

    public String toString() {
        String result = "Grid with " + shadows.size() + " elements: \n";
        int lowestPoint = findLowestPoint(shadows);
        for(int i = 0; i <= lowestPoint; i++){
            String row = i + ": ";
            for(int j = 0; j < columnCount; j++){
                row += "[" + (filledMap[i][j] == null ? "---@(-,-)" : filledMap[i][j]) + "]";
            }
            row += "\n";
            result += row;
        }
        return result;
    }
}