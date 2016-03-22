package com.inipage.homelylauncher.icons;

import android.content.ComponentName;
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
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.inipage.homelylauncher.ApplicationClass;
import com.inipage.homelylauncher.drawer.StickyImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * Class for maintaining icons in a memory cache. Very similar to an LRU, although manually
 * tweaked for the application's purposes. Net effect is to great reduce icon redraws.
 */
public class IconCache {
    private static final String TAG = "IconCache";

    /**
     * Maximum tries to get an icon if a failure is only partial (e.g. OOM).
     **/
    private static final int MAX_RETRY_COUNT = 5;

    /**
     * A task that represents an element in the app drawer.
     **/
    public static final int APP_DRAWER_TASK = 1;

    /**
     * A task that represents setting an element on the dock.
     **/
    public static final int DOCK_TASK = 2;

    /**
     * A task that represents grabbing a R.id.{} for the swipe layout.
     **/
    public static final int SWIPE_ICON_LOCAL_RESOURCE_TASK = 3;

    /**
     * A task that represents grabbing a resource from an icon pack. for the swipe layout.
     **/
    public static final int SWIPE_ICON_ICON_PACK_TASK = 4;

    /**
     * A task that represents grabbing a local icon for the swipe layout.
     **/
    public static final int SWIPE_ICON_APP_ICON_TASK = 5;

    private int DEFAULT_ICON_SIZE = 48;

    //For memory management
    long memoryPressureLimit;

    //Needed tools
    Resources baseResources;
    PackageManager pm;

    //Lock for cache access
    private static final Object cacheLock = new Object();

    private Map<String, Pair<Integer, Bitmap>> iconCache;
    private Map<String, Pair<Integer, Bitmap>> swipeCache;

    //Lock for task structure access
    private static final Object taskLock = new Object();

    private List<Pair<Integer, BitmapRetrievalTask>> taskList;
    private Map<String, BitmapRetrievalTask> taskMap;

    private static IconCache instance;

    //Dummy bitmap to use while still retrieving bitmaps
    public Bitmap dummyBitmap;

    private enum RetrievalTaskResponse {
        SUCCESS, PARTIAL_FAILURE, TOTAL_FAILURE
    }

    private class BitmapRetrievalTask extends AsyncTask<Object, Void, RetrievalTaskResponse> {
        Pair<Integer, BitmapRetrievalTask> taskRef;
        String tag;
        View location;
        int taskType;
        ItemRetrievalInterface retrievalInterface;
        Object[] copy;

        @Override
        protected RetrievalTaskResponse doInBackground(Object... params) {
            try {
                taskType = (Integer) params[0];
                tag = (String) params[1];
                String packageName = (String) params[2];
                taskRef = (Pair<Integer, BitmapRetrievalTask>) params[3];
                Integer dimensionsHint = (Integer) params[4];
                //Up to here is always the same

                Drawable d;
                Bitmap toDraw;
                if (taskType == APP_DRAWER_TASK || taskType == DOCK_TASK) {
                    if (taskType == DOCK_TASK) {
                        location = (ImageView) params[5];
                    } else {
                        location = (StickyImageView) params[5];
                    }
                    String componentName = (String) params[6];
                    ComponentName cm = new ComponentName(packageName, componentName);
                    d = pm.getActivityIcon(cm);

                    logi(this, "Fetched drawable for app drawer/dock task");
                } else if (taskType == SWIPE_ICON_APP_ICON_TASK) {
                    String componentName = (String) params[5];
                    retrievalInterface = (ItemRetrievalInterface) params[6];
                    ComponentName cm = new ComponentName(packageName, componentName);
                    d = pm.getActivityIcon(cm);

                    logi(this, "Fetched drawable for swipe app icon");
                } else if (taskType == SWIPE_ICON_ICON_PACK_TASK) {
                    String resourceName = (String) params[5];
                    retrievalInterface = (ItemRetrievalInterface) params[6];
                    Resources res = pm.getResourcesForApplication(packageName);
                    int resourceId = res.getIdentifier(resourceName, "drawable", packageName);
                    d = res.getDrawable(resourceId);

                    logi(this, "Fetched drawable for swipe icon pack icon");
                } else if (taskType == SWIPE_ICON_LOCAL_RESOURCE_TASK) {
                    Integer resourceId = (Integer) params[5];
                    retrievalInterface = (ItemRetrievalInterface) params[6];
                    d = baseResources.getDrawable(resourceId);

                    logi(this, "Fetched drawable for swipe icon local resource");
                } else {
                    d = pm.getApplicationIcon(packageName);

                    logi(this, "Fetched application icon from package name; unknown type");
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
                evictUntilFree(toDraw.getByteCount() * 4);

                synchronized (cacheLock) {
                    if (taskType == APP_DRAWER_TASK || taskType == DOCK_TASK) {
                        iconCache.put(tag, new Pair<>(1, toDraw));
                    } else if (taskType == SWIPE_ICON_ICON_PACK_TASK || taskType == SWIPE_ICON_LOCAL_RESOURCE_TASK ||
                            taskType == SWIPE_ICON_APP_ICON_TASK) {
                        swipeCache.put(tag, new Pair<>(1, toDraw));
                    }
                }

                logi(this, "Successful icon fetch");
                return RetrievalTaskResponse.SUCCESS;
            } catch (PackageManager.NameNotFoundException e) {
                loge(this, "Unable to retrieve icon for package; " + e.getMessage());
                return RetrievalTaskResponse.TOTAL_FAILURE;
            } catch (Resources.NotFoundException e) {
                loge(this, "Unable to retrieve icon pack entry; " + e.getMessage());
                return RetrievalTaskResponse.TOTAL_FAILURE;
            } catch (OutOfMemoryError e) { //OOM errors, usually
                loge(this, "OutOfMemory while retrieving icon");

                int retryCount = (int) params[params.length - 1];
                if (retryCount > MAX_RETRY_COUNT) {
                    return RetrievalTaskResponse.TOTAL_FAILURE;
                } else {
                    copy = params.clone();
                    return RetrievalTaskResponse.PARTIAL_FAILURE;
                }
            }
        }

        @Override
        protected void onPostExecute(RetrievalTaskResponse result) {
            logi(this, "Result");

            //Remove task from list & map
            synchronized (taskLock) {
                taskMap.remove(tag);
                taskList.remove(taskRef);
            }

            if (result == RetrievalTaskResponse.PARTIAL_FAILURE) {
                delayedRetry(copy);
            } else if (result == RetrievalTaskResponse.TOTAL_FAILURE) {
                return; //Nothing to do here
            }

            synchronized (cacheLock) {
                try {
                    if (taskType == APP_DRAWER_TASK || taskType == DOCK_TASK) {
                        if (location == null || location.getTag() == null) return; //We're done here

                        if (location.getTag().equals(tag)) {
                            Pair<Integer, Bitmap> res = iconCache.get(tag);
                            if (res != null) {
                                if (taskType == DOCK_TASK)
                                    ((ImageView) location).setImageBitmap(res.second);
                                else
                                    ((StickyImageView) location).setImageBitmap(res.second);
                            }
                        } else {
                        } //Location has changed purpose; request is pointless; OK
                    } else if (taskType == SWIPE_ICON_ICON_PACK_TASK || taskType == SWIPE_ICON_LOCAL_RESOURCE_TASK
                            || taskType == SWIPE_ICON_APP_ICON_TASK) {
                        Pair<Integer, Bitmap> res = swipeCache.get(tag);
                        if (res != null) {
                            Bitmap bmpResult = swipeCache.get(tag).second;
                            if (retrievalInterface != null)
                                retrievalInterface.onRetrievalComplete(bmpResult);
                        } else {
                            Log.e(TAG, "Swipe cache unexpected missing element; high memory situation found");
                            if (retrievalInterface != null)
                                retrievalInterface.onRetrievalComplete(dummyBitmap);
                        }
                    }
                } catch (Error memoryError) {
                    Log.e(TAG, "Ran out of memory to set bitmap!");
                }
            }
        }
    }

    /**
     * Retry getting an icon if its initial failure was for memory pressure/strange reasons and
     * can be expected to succeed if tried again.
     *
     * @param params The params object from the original task.
     */
    private void delayedRetry(Object[] params) {
        int taskType = (Integer) params[0];
        int retryCount = ((int) params[params.length - 1]) + 1;

        switch (taskType) {
            case APP_DRAWER_TASK:
                break;
            case DOCK_TASK:
                break;
            case SWIPE_ICON_LOCAL_RESOURCE_TASK:
                break;
            case SWIPE_ICON_APP_ICON_TASK:
                break;
            case SWIPE_ICON_ICON_PACK_TASK:
                break;
        }
    }

    //Only evicts from app cache.
    private void evictUntilFree(int byteCount) {
        Log.i(TAG, "Checking if space present for " + byteCount + " bytes");

        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        if (usedMemory < memoryPressureLimit) { //Used > pressure value
            Log.i(TAG, "Only " + usedMemory + " bytes used; pressure fine for now");
            return;
        } else {
            Log.i(TAG, "Memory pressure tight; trying to evict");
        }

        synchronized (cacheLock) {
            int bytesFreed = 0;
            int maximumUsageNumber = 0;
            int usageNumberPass = 1;
            List<String> toEvict = new ArrayList<>();

            //Get max usage
            for (Map.Entry<String, Pair<Integer, Bitmap>> entry : iconCache.entrySet()) {
                if (entry.getValue().first > maximumUsageNumber)
                    maximumUsageNumber = entry.getValue().first;
            }

            Log.i(TAG, "Most used element has been used " + maximumUsageNumber + " times");

            usageLoop:
            {
                while (bytesFreed < byteCount && usageNumberPass <= maximumUsageNumber) { //Run until we've freed enough space or at last
                    for (Map.Entry<String, Pair<Integer, Bitmap>> entry : iconCache.entrySet()) {
                        int usageCount = entry.getValue().first;

                        if (usageCount == usageNumberPass) {
                            toEvict.add(entry.getKey());
                            bytesFreed += entry.getValue().second.getByteCount();
                        }

                        if (bytesFreed >= byteCount){
                            Log.i(TAG, "Freed enough bytes; process complete");
                            break usageLoop;
                        }
                    }

                    usageNumberPass++;
                }
            }

            Log.i(TAG, toEvict.size() + " elements removed to free " + bytesFreed + " bytes");

            for (String s : toEvict) {
                Log.i(TAG, "Evicting: " + s);
                iconCache.remove(s);
            }
        }
    }

    public interface ItemRetrievalInterface {
        void onRetrievalStarted();

        void onRetrievalComplete(Bitmap result);
    }

    private IconCache() {
        memoryPressureLimit = (long) (Runtime.getRuntime().maxMemory() * 0.8f); //Max memory in kilobytes

        iconCache = new HashMap<>();
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
        p.setAlpha(123);
        c.drawCircle(dimens / 2, dimens / 2, dimens / 2, p);

        DEFAULT_ICON_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                ApplicationClass.getInstance().getResources().getDisplayMetrics());
    }

    public static IconCache getInstance() {
        return instance == null ? (instance = new IconCache()) : instance;
    }

    public void invalidateCaches() {
        synchronized (cacheLock) {
            iconCache.clear();
        }

        synchronized (taskLock) {
            for (Pair<Integer, BitmapRetrievalTask> pair : taskList) {
                if (!pair.second.isCancelled()) pair.second.cancel(true);
            }
            taskList.clear();
            taskMap.clear();
        }
    }

    private void setIconImpl(String packageName, String componentName, View place, int taskType, int retryCount) {
        final String key = packageName + "|" + componentName;
        if (place != null) place.setTag(key);

        Bitmap icon = null;
        synchronized (cacheLock) {
            icon = iconCache.get(key) == null ? null : iconCache.get(key).second;
        }
        if (icon != null) {
            try {
                if (place != null) {
                    if (taskType == DOCK_TASK)
                        ((ImageView) place).setImageBitmap(icon);
                    else
                        ((StickyImageView) place).setImageBitmap(icon);
                }
                return;
            } catch (Exception ignored) { //This means the bitmap has been recycled/otherwise trashed
                Log.i(TAG, "Icon recycled!");
            }
        }

        //Start a task
        BitmapRetrievalTask getter = new BitmapRetrievalTask();
        Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(taskType, getter);

        synchronized (taskLock) {
            taskList.add(pair);
            taskMap.put(key, getter);
        }

        try {
            getter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, taskType, key, packageName, pair,
                    place != null ? place.getWidth() : DEFAULT_ICON_SIZE, place, componentName,
                    retryCount);
        } catch (RejectedExecutionException tooManyRunning) {
            try {
                getter.execute(taskType, key, packageName, pair,
                        place != null ? place.getWidth() : DEFAULT_ICON_SIZE, place, componentName,
                        retryCount);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Set the icon for a given drawable on an icon. Begins a set of tasks for setting icons.
     *
     * @param packageName   The package of the icon.
     * @param componentName The component of the icon.
     * @param place         Where to set it (can be null just to cache it).
     */
    public void setIcon(String packageName, String componentName, StickyImageView place) {
        setIconImpl(packageName, componentName, place, APP_DRAWER_TASK, 0);
    }

    public Bitmap getSwipeCacheIcon(int resource, float sizeHint, ItemRetrievalInterface callback) {
        return getSwipeCacheIcon(resource, sizeHint, callback, 0);
    }

    private Bitmap getSwipeCacheIcon(int resource, float sizeHint, ItemRetrievalInterface callback, int retryCount) {
        String key = "-1|" + resource;
        Pair<Integer, Bitmap> cachedCopy = swipeCache.get(key);
        if (cachedCopy != null && cachedCopy.second != null && !cachedCopy.second.isRecycled()) {
            swipeCache.put(key, new Pair<>(cachedCopy.first + 1, cachedCopy.second));
            return cachedCopy.second;
        }

        //Start a task if needed
        if (!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_LOCAL_RESOURCE_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            try {
                getter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, SWIPE_ICON_LOCAL_RESOURCE_TASK,
                        key, "com.inipage.homelylauncher", pair, (int) sizeHint, resource, callback,
                        retryCount);
            } catch (RejectedExecutionException tooManyRunning) {
                try {
                    getter.execute(SWIPE_ICON_LOCAL_RESOURCE_TASK, key, "com.inipage.homelylauncher",
                            pair, (int) sizeHint, resource, callback, retryCount);
                } catch (Exception ignored) {
                }
            }
        }
        return dummyBitmap;
    }

    public Bitmap getSwipeCacheIcon(String iconPackPackage, String resource, float sizeHint, ItemRetrievalInterface callback) {
        return getSwipeCacheIcon(iconPackPackage, resource, sizeHint, callback, 0);
    }

    private Bitmap getSwipeCacheIcon(String iconPackPackage, String resource, float sizeHint, ItemRetrievalInterface callback, int retryCount) {
        String key = iconPackPackage + "|" + resource;
        Pair<Integer, Bitmap> cachedCopy = swipeCache.get(key);
        if (cachedCopy != null && cachedCopy.second != null && !cachedCopy.second.isRecycled()) {
            swipeCache.put(key, new Pair<>(cachedCopy.first + 1, cachedCopy.second));
            return cachedCopy.second;
        }

        //Start a task if needed
        if (!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_LOCAL_RESOURCE_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            try {
                getter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, SWIPE_ICON_ICON_PACK_TASK, key,
                        iconPackPackage, pair, (int) sizeHint, resource, callback, 0);
            } catch (RejectedExecutionException tooManyRunning) {
                try {
                    getter.execute(SWIPE_ICON_ICON_PACK_TASK, key, iconPackPackage, pair, (int) sizeHint,
                            resource, callback, 0);
                } catch (Exception ignored) {
                }
            }
        }
        return dummyBitmap;
    }

    public Bitmap getSwipeCacheAppIcon(String iconPackPackage, String activityName, float sizeHint, ItemRetrievalInterface callback) {
        return getSwipeCacheAppIcon(iconPackPackage, activityName, sizeHint, callback, 0);
    }

    private Bitmap getSwipeCacheAppIcon(String iconPackPackage, String activityName, float sizeHint, ItemRetrievalInterface callback, int retryCount) {
        String key = iconPackPackage + "|" + activityName;
        Pair<Integer, Bitmap> cachedCopy = swipeCache.get(key);
        if (cachedCopy != null && cachedCopy.second != null && !cachedCopy.second.isRecycled()) {
            swipeCache.put(key, new Pair<>(cachedCopy.first + 1, cachedCopy.second));
            return cachedCopy.second;
        }


        //Start a task if needed
        if (!taskMap.containsKey(key)) {
            BitmapRetrievalTask getter = new BitmapRetrievalTask();
            Pair<Integer, BitmapRetrievalTask> pair = new Pair<>(SWIPE_ICON_APP_ICON_TASK, getter);
            taskList.add(pair);
            taskMap.put(key, getter);
            try {
                getter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, SWIPE_ICON_APP_ICON_TASK, key,
                        iconPackPackage, pair, (int) sizeHint, activityName, callback, retryCount);
            } catch (RejectedExecutionException tooManyRunning) {
                try {
                    getter.execute(SWIPE_ICON_APP_ICON_TASK, key, iconPackPackage, pair,
                            (int) sizeHint, activityName, callback, retryCount);
                } catch (Exception ignored) {
                }
            }
        }

        return dummyBitmap;
    }

    /**
     * Set the icon for a given drawable on an dock element.
     *
     * @param packageName   The package of the icon.
     * @param componentName The component of the icon.
     * @param place         Where to set it.
     */
    public void setDockIcon(String packageName, String componentName, ImageView place) {
        setIconImpl(packageName, componentName, place, DOCK_TASK, 0);
    }

    private void cancelTaskImpl(int taskType) {
        for (Pair<Integer, BitmapRetrievalTask> task : taskList) {
            if (task.first == taskType && !task.second.isCancelled()) {
                task.second.cancel(true);
            }
        }
    }

    public void cancelPendingDockTasks() {
        cancelTaskImpl(DOCK_TASK);
    }

    public void cancelPendingIconTasks() {
        cancelTaskImpl(APP_DRAWER_TASK);
    }

    public void cancelPendingSwipeTasks() {
        cancelTaskImpl(SWIPE_ICON_LOCAL_RESOURCE_TASK);
        cancelTaskImpl(SWIPE_ICON_ICON_PACK_TASK);
    }

    public void cancelPendingIconTaskIfRunning(String tag) {
        synchronized (taskLock) {
            if (taskMap.containsKey(tag) && !taskMap.get(tag).isCancelled()) {
                taskMap.get(tag).cancel(true);
            }
        }
    }

    private void logi(BitmapRetrievalTask task, String content){
        Log.i(TAG, "[" + task.toString() + "] " + content);
    }

    private void logd(BitmapRetrievalTask task, String content){
        Log.d(TAG, "[" + task.toString() + "] " + content);
    }

    private void loge(BitmapRetrievalTask task, String content){
        Log.e(TAG, "[" + task.toString() + "] " + content);
    }
}
