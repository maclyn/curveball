package com.inipage.homelylauncher.icons;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.LruCache;
import android.widget.ImageView;

import com.inipage.homelylauncher.ApplicationClass;
import com.inipage.homelylauncher.ShortcutGestureView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconCache {
    public static final int APP_DRAWER_TASK = 1;
    public static final int DOCK_TASK = 2;
    public static final int SWIPE_ICON_LOCAL_RESOURCE_TASK = 3;
    public static final int SWIPE_ICON_ICON_PACK_TASK = 4;
    public static final int SMARTBAR_ICON_TASK = 5;

    //Needed tools
    Resources baseResources;
    PackageManager pm;

    private LruCache<String, Bitmap> iconCache;
    private LruCache<String, Bitmap> swipeCache;

    private List<Pair<Integer, BitmapRetrievalTask>> taskList;
    private Map<String, BitmapRetrievalTask> taskMap;

    private static IconCache instance;

    //Dummy bitmap to use while still retrieving bitmaps
    Bitmap dummyBitmap;

    private class BitmapRetrievalTask extends AsyncTask<Object, Void, Boolean> {
        Pair<Integer, BitmapRetrievalTask> taskRef;
        String tag;
        ImageView location;
        Integer taskType;
        ItemRetrievalInterface retrievalInterface;

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                taskType = (Integer) params[0];
                tag = (String) params[1];
                String packageName = (String) params[2];
                taskRef = (Pair<Integer, BitmapRetrievalTask>) params[3];
                Integer dimensionsHint = (Integer) params[4];
                //Up to here is always the same

                Drawable d;
                Bitmap toDraw;
                try {
                    if(taskType == APP_DRAWER_TASK || taskType ==  DOCK_TASK) {
                        location = (ImageView) params[5];
                        String componentName = (String) params[6];
                        ComponentName cm = new ComponentName(packageName, componentName);
                        d = pm.getActivityIcon(cm);
                    } else if (taskType == SMARTBAR_ICON_TASK){
                        //We're just grabbing the package icon
                        location = (ImageView) params[5];
                        d = pm.getApplicationIcon(packageName);
                    } else if (taskType == SWIPE_ICON_ICON_PACK_TASK){
                        String resourceName = (String) params[5];
                        retrievalInterface = (ItemRetrievalInterface) params[6];

                        Resources res = pm.getResourcesForApplication(packageName);
                        int resourceId = res.getIdentifier(resourceName, "drawable", packageName);
                        d = res.getDrawable(resourceId);
                    } else if (taskType == SWIPE_ICON_LOCAL_RESOURCE_TASK){
                        Integer resourceId = (Integer) params[5];
                        retrievalInterface = (ItemRetrievalInterface) params[6];

                        d = baseResources.getDrawable(resourceId);
                    } else {
                        d = pm.getApplicationIcon(packageName);
                    }
                } catch (Exception e) {
                    return false;
                }

                if (d instanceof BitmapDrawable) {
                    toDraw = ((BitmapDrawable) d).getBitmap();
                } else { //Draw it to a canvas backed
                    toDraw = Bitmap.createBitmap(dimensionsHint, dimensionsHint, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(toDraw);
                    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    d.draw(canvas);
                }

                iconCache.put(tag, toDraw);
                return true;
            } catch (Exception e) {
                return false;
            } catch (Error e) { //OOM errors, usually
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result){
            //Remove task from list & map
            taskMap.remove(tag);
            taskList.remove(taskRef);

            try {
                if(taskType == APP_DRAWER_TASK || taskType == DOCK_TASK || taskType == SMARTBAR_ICON_TASK) {
                    if (result && location.getTag() != null && location.getTag().equals(tag)) {
                        location.setImageBitmap(iconCache.get(tag));
                    } else {
                        location.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                } else if (taskType == SWIPE_ICON_ICON_PACK_TASK || taskType == SWIPE_ICON_LOCAL_RESOURCE_TASK){
                    Bitmap bmpResult = result ? swipeCache.get(tag) : swipeCache.put(tag, dummyBitmap);
                    retrievalInterface.onRetrievalComplete(bmpResult);
                }
            } catch (Error memoryError) {
                //Sobs.
            }
        }
    }

    public interface ItemRetrievalInterface {
        void onRetrievalStarted();
        void onRetrievalComplete(Bitmap result);
    }

    private IconCache(){
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        iconCache =  new LruCache<>(maxMemory / 8); //Reserves 1/8th total memory
        swipeCache = new LruCache<>(maxMemory / 16); //Reserves 1/6th total memory

        pm = ApplicationClass.getInstance().getPackageManager();
        baseResources = ApplicationClass.getInstance().getResources();

        taskList = new ArrayList<>();
        taskMap = new HashMap<>();

        //Create the dummy bitmap
        int dimens = ApplicationClass.getInstance().getResources().getDisplayMetrics().widthPixels / 8;
        dummyBitmap = Bitmap.createBitmap(dimens, dimens, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dummyBitmap);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setAlpha(180);
        c.drawCircle(dimens / 2, dimens / 2, dimens / 2, p);
    }

    public static IconCache getInstance(){
        return instance == null ? (instance = new IconCache()) : instance;
    }

    public void invalidateCaches() {
        iconCache.evictAll();
        for(Pair<Integer, BitmapRetrievalTask> pair : taskList){
            if(!pair.second.isCancelled()) pair.second.cancel(true);
        }
        taskList.clear();
        taskMap.clear();
    }

    public void selectivelyEvictCaches(String packageToEvict){

    }

    private void setIconImpl(String packageName, String componentName, ImageView place, int taskType){
        final String key = packageName + "|" + componentName;
        place.setTag(key);

        Bitmap icon = iconCache.get(key);
        if(icon != null){
            try {
                place.setImageBitmap(icon);
                return;
            } catch (Exception ignored){ //This means the bitmap has been recycled
            }
        }

        //Start a task
        BitmapRetrievalTask getter = new BitmapRetrievalTask();
        Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(taskType, getter);
        taskList.add(pair);
        taskMap.put(key, getter);

        getter.execute(taskType, key, packageName, pair, place.getWidth(), place, componentName);
    }

    /**
     * Set the icon for a given drawable on an icon. Begins a set of tasks for setting icons.
     * @param packageName The package of the icon.
     * @param componentName The component of the icon.
     * @param place Where to set it.
     */
    public void setIcon(String packageName, String componentName, ImageView place){
        setIconImpl(packageName, componentName, place, APP_DRAWER_TASK);
    }

    public Bitmap getSwipeCacheIcon(int resource, int sizeHint, ItemRetrievalInterface callback){
        String key = "-1|" + resource;
        Bitmap cachedCopy = swipeCache.get(key);
        if(cachedCopy != null && !cachedCopy.isRecycled()){
            return cachedCopy;
        }

        //Start a task if needed
        if(!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_LOCAL_RESOURCE_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            getter.execute(SWIPE_ICON_LOCAL_RESOURCE_TASK, key, "com.inipage.homelylauncher", pair,
                    sizeHint, resource, callback );
        }
        return dummyBitmap;
    }

    public Bitmap getSwipeCacheIcon(String iconPackPackage, String resource, int sizeHint, ItemRetrievalInterface callback){
        String key = iconPackPackage + "|" + resource;
        Bitmap cachedCopy = swipeCache.get(key);
        if(cachedCopy != null && !cachedCopy.isRecycled()){
            return cachedCopy;
        }

        //Start a task if needed
        if(!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_LOCAL_RESOURCE_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            getter.execute(SWIPE_ICON_LOCAL_RESOURCE_TASK, key, iconPackPackage, pair,
                    sizeHint, resource, callback);
        }
        return dummyBitmap;
    }

    /**
     * Set the icon for a given drawable on an dock element.
     * @param packageName The package of the icon.
     * @param componentName The component of the icon.
     * @param place Where to set it.
     */
    public void setDockIcon(String packageName, String componentName, ImageView place){
        setIconImpl(packageName, componentName, place, DOCK_TASK);
    }

    /**
     * Set the icon for a given drawable on a smartbar element.
     * @param packageName The package name of the icon.
     * @param place Where to set it.
     */
    public void setSmartbarIcon(String packageName, ImageView place){
        final String key = packageName + "|" + "-1";
        place.setTag(key);

        Bitmap icon = iconCache.get(key);
        if(icon != null){
            try {
                place.setImageBitmap(icon);
                return;
            } catch (Exception ignored){ //This means the bitmap has been recycled
            }
        }

        //Start a task
        BitmapRetrievalTask getter = new BitmapRetrievalTask();
        Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SMARTBAR_ICON_TASK, getter);
        taskList.add(pair);

        getter.execute(SMARTBAR_ICON_TASK, key, packageName, pair, place.getWidth(), place);
    }

    private void cancelTaskImpl(int taskType){
        for(Pair<Integer, BitmapRetrievalTask> task : taskList){
            if(task.first == taskType && !task.second.isCancelled()){
                task.second.cancel(true);
            }
        }
    }

    public void cancelPendingDockTasks(){
        cancelTaskImpl(DOCK_TASK);
    }

    public void cancelPendingIconTasks() {
        cancelTaskImpl(APP_DRAWER_TASK);
    }

    public void cancelPendingSwipeTasks() {
        cancelTaskImpl(SWIPE_ICON_LOCAL_RESOURCE_TASK);
        cancelTaskImpl(SWIPE_ICON_ICON_PACK_TASK);
    }

    public void cancelPendingIconTaskIfRunning(String tag){
        if(taskMap.containsKey(tag) && !taskMap.get(tag).isCancelled()){
            taskMap.get(tag).cancel(true);
        }
    }
}
