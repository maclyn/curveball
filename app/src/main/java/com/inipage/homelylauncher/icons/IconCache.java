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
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.widget.ImageView;

import com.inipage.homelylauncher.ApplicationClass;
import com.inipage.homelylauncher.ShortcutGestureView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconCache {
    private static final String TAG = "IconCache";

    public static final int APP_DRAWER_TASK = 1;
    public static final int DOCK_TASK = 2;
    public static final int SWIPE_ICON_LOCAL_RESOURCE_TASK = 3;
    public static final int SWIPE_ICON_ICON_PACK_TASK = 4;
    public static final int SWIPE_ICON_APP_ICON_TASK = 5;
    public static final int SMARTBAR_ICON_TASK = 6;

    private int DEFAULT_ICON_SIZE = 48;

    //For memory management
    int memoryPressureLimit;

    //Needed tools
    Resources baseResources;
    PackageManager pm;

    private Map<String, Pair<Integer, Bitmap>> iconCache;
    private Map<String, Pair<Integer, Bitmap>> swipeCache;

    private List<Pair<Integer, BitmapRetrievalTask>> taskList;
    private Map<String, BitmapRetrievalTask> taskMap;

    private static IconCache instance;

    //Dummy bitmap to use while still retrieving bitmaps
    public Bitmap dummyBitmap;

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
                    } else if (taskType == SMARTBAR_ICON_TASK) {
                        //We're just grabbing the package icon
                        location = (ImageView) params[5];
                        d = pm.getApplicationIcon(packageName);
                    } else if (taskType == SWIPE_ICON_APP_ICON_TASK){
                        String componentName = (String) params[5];
                        retrievalInterface = (ItemRetrievalInterface) params[6];
                        ComponentName cm = new ComponentName(packageName, componentName);
                        d = pm.getActivityIcon(cm);
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

                //Check if over memory pressure limit; if so, evict the least used element
                if(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory() > memoryPressureLimit){ //Used > pressure value
                    evictUntilFree(toDraw.getByteCount());
                }

                if(taskType == APP_DRAWER_TASK || taskType ==  DOCK_TASK || taskType == SMARTBAR_ICON_TASK) {
                    iconCache.put(tag, new Pair<>(1, toDraw));
                } else if (taskType == SWIPE_ICON_ICON_PACK_TASK || taskType == SWIPE_ICON_LOCAL_RESOURCE_TASK ||
                        taskType == SWIPE_ICON_APP_ICON_TASK) {
                    swipeCache.put(tag, new Pair<>(1, toDraw));
                }

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
                    if(location == null) return; //We're done here

                    if (result && location.getTag() != null && location.getTag().equals(tag)) {
                        location.setImageBitmap(iconCache.get(tag).second);
                    } else {
                        location.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                } else if (taskType == SWIPE_ICON_ICON_PACK_TASK || taskType == SWIPE_ICON_LOCAL_RESOURCE_TASK
                        || taskType == SWIPE_ICON_APP_ICON_TASK){
                    Bitmap bmpResult = result ? swipeCache.get(tag).second :
                            swipeCache.put(tag, new Pair<>(1, dummyBitmap)).second;
                    if(retrievalInterface != null) retrievalInterface.onRetrievalComplete(bmpResult);
                }
            } catch (Error memoryError) {
                //Sobs.
            }
        }
    }

    //Only evicts from app cache.
    private void evictUntilFree(int byteCount) {
        int bytesFreed = 0;

        int usageNumberPass = 0;
        List<String> toEvict = new ArrayList<>();
        usageLoop: {
            while (bytesFreed < byteCount || usageNumberPass > 5) { //Run until we've freed enough space or
                // gotten to stuff that's been used 5 times
                for (Map.Entry<String, Pair<Integer, Bitmap>> entry : iconCache.entrySet()) {
                    if (entry.getValue().first == usageNumberPass) {
                        toEvict.add(entry.getKey());
                        bytesFreed += entry.getValue().first;
                    }

                    if (bytesFreed >= byteCount) break usageLoop;
                }
                usageNumberPass++;
            }
        }

        for(String s : toEvict){
            iconCache.remove(s);
        }
    }

    public interface ItemRetrievalInterface {
        void onRetrievalStarted();
        void onRetrievalComplete(Bitmap result);
    }

    private IconCache(){
        memoryPressureLimit = (int) (Runtime.getRuntime().maxMemory() * 0.7f); //Max memory in kilobytes

        iconCache =  new HashMap<>();
        swipeCache = new HashMap<>();

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

        DEFAULT_ICON_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                ApplicationClass.getInstance().getResources().getDisplayMetrics());
    }

    public static IconCache getInstance(){
        return instance == null ? (instance = new IconCache()) : instance;
    }

    public void invalidateCaches() {
        iconCache.clear();
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
        if(place != null) place.setTag(key);

        Bitmap icon = iconCache.get(key).second;
        if(icon != null){
            try {
                if(place != null) place.setImageBitmap(icon);
                return;
            } catch (Exception ignored){ //This means the bitmap has been recycled
            }
        }

        //Start a task
        BitmapRetrievalTask getter = new BitmapRetrievalTask();
        Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(taskType, getter);
        taskList.add(pair);
        taskMap.put(key, getter);

        getter.execute(taskType, key, packageName, pair, place != null ?
                place.getWidth() : DEFAULT_ICON_SIZE, place, componentName);
    }

    /**
     * Set the icon for a given drawable on an icon. Begins a set of tasks for setting icons.
     * @param packageName The package of the icon.
     * @param componentName The component of the icon.
     * @param place Where to set it (can be null just to cache it).
     */
    public void setIcon(String packageName, String componentName, ImageView place){
        setIconImpl(packageName, componentName, place, APP_DRAWER_TASK);
    }

    public Bitmap getSwipeCacheIcon(int resource, float sizeHint, ItemRetrievalInterface callback){
        String key = "-1|" + resource;
        Pair<Integer, Bitmap> cachedCopy = swipeCache.get(key);
        if(cachedCopy != null && cachedCopy.second != null && !cachedCopy.second.isRecycled()){
            swipeCache.put(key, new Pair<>(cachedCopy.first + 1, cachedCopy.second));
            return cachedCopy.second;
        }

        //Start a task if needed
        if(!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_LOCAL_RESOURCE_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            getter.execute(SWIPE_ICON_LOCAL_RESOURCE_TASK, key, "com.inipage.homelylauncher", pair,
                    (int) sizeHint, resource, callback);
        }
        return dummyBitmap;
    }

    public Bitmap getSwipeCacheIcon(String iconPackPackage, String resource, float sizeHint, ItemRetrievalInterface callback){
        String key = iconPackPackage + "|" + resource;
        Pair<Integer, Bitmap> cachedCopy = swipeCache.get(key);
        if(cachedCopy != null && cachedCopy.second != null && !cachedCopy.second.isRecycled()){
            swipeCache.put(key, new Pair<>(cachedCopy.first + 1, cachedCopy.second));
            return cachedCopy.second;
        }

        //Start a task if needed
        if(!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_LOCAL_RESOURCE_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            getter.execute(SWIPE_ICON_ICON_PACK_TASK, key, iconPackPackage, pair,
                    (int) sizeHint, resource, callback);
        }
        return dummyBitmap;
    }

    public Bitmap getSwipeCacheAppIcon(String iconPackPackage, String activityName, float sizeHint, ItemRetrievalInterface callback){
        String key = iconPackPackage + "|" + activityName;
        Pair<Integer, Bitmap> cachedCopy = swipeCache.get(key);
        if(cachedCopy != null && cachedCopy.second != null && !cachedCopy.second.isRecycled()){
            swipeCache.put(key, new Pair<>(cachedCopy.first + 1, cachedCopy.second));
            return cachedCopy.second;
        }


        //Start a task if needed
        if(!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_APP_ICON_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            getter.execute(SWIPE_ICON_APP_ICON_TASK, key, iconPackPackage, pair,
                    (int) sizeHint, activityName, callback);
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

        Bitmap icon = iconCache.get(key).second;
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
