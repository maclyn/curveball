package com.inipage.homelylauncher.icons;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import com.inipage.homelylauncher.ApplicationClass;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * A cache for icons. This is a rewrite of an earlier build of IconCache that was hard to maintain.
 */
public class IconCache {
    private static final String TAG = "IconCache";
    private static final boolean QUIET = false; //IconCache can make our logs noisy

    public enum IconFetchPriority {
        /** Needs to get done ASAP (e.g. dock icons on start). **/
        HIGHEST_PRIORITY(6),
        /** Built-in app icons (e.g. white pencil). **/
        BUILT_IN_ICONS(5),
        /** Icons in the dock. **/
        DOCK_ICONS(4),
        /** Icons for swipe folders. **/
        SWIPE_FOLDER_ICONS(3),
        /** Icons for swipe apps. **/
        SWIPE_APP_ICONS(2),
        /** Icons in the drawer. **/
        APP_DRAWER_ICONS(1);

        public int sortValue;

        IconFetchPriority(int sortValue){
            this.sortValue = sortValue;
        }
    }

    private enum IconFetchType {
        APP_ICON_FULLY_QUALIFIED(1),
        APP_ICON_PARTIALLY_QUALIFIED(2),
        PACKAGE_LOCAL_RESOURCE(3),
        PACKAGE_FOREIGN_RESOURCE(4);

        public int fetchValue;

        IconFetchType(int fetchValue){
            this.fetchValue = fetchValue;
        }
    }

    private class IconFetchTask {
        private IconFetchPriority priority;
        private IconFetchType type;
        private String packageName;
        private String componentName;
        private String resourceName;
        private int resourceId;
        private int width;
        private int height;
        private ItemRetrievalInterface callback;

        public IconFetchTask(IconFetchPriority priority, IconFetchType type, String packageName, String componentName, String resourceName, int resourceId, int width, int height, ItemRetrievalInterface callback) {
            this.priority = priority;
            this.type = type;
            this.packageName = packageName;
            this.componentName = componentName;
            this.resourceName = resourceName;
            this.resourceId = resourceId;
            this.width = width;
            this.height = height;
            this.callback = callback;
        }

        public IconFetchPriority getPriority() {
            return priority;
        }

        public IconFetchType getType() {
            return type;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getComponentName() {
            return componentName;
        }

        public String getResourceName() {
            return resourceName;
        }

        public int getResourceId() {
            return resourceId;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public ItemRetrievalInterface getCallback() {
            return callback;
        }

        public void clearCallback() {
            this.callback = null;
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof IconFetchTask)) return false;

            IconFetchTask task = (IconFetchTask) obj;
            if(task.getType() != this.type) return false;

            switch(task.getType()){
                case APP_ICON_FULLY_QUALIFIED:
                    return this.packageName.equals(task.packageName) &&
                            this.componentName.equals(task.componentName) &&
                            this.height == task.height &&
                            this.width == task.width;
                case APP_ICON_PARTIALLY_QUALIFIED:
                    return this.packageName.equals(task.packageName) &&
                            this.height == task.height &&
                            this.width == task.width;
                case PACKAGE_LOCAL_RESOURCE:
                    return this.resourceId == task.resourceId &&
                            this.height == task.height &&
                            this.width == task.width;
                case PACKAGE_FOREIGN_RESOURCE:
                    return this.packageName.equals(task.packageName) &&
                            this.resourceName.equals(task.resourceName) &&
                            this.height == task.height &&
                            this.width == task.width;
            }

            return false;
        }

        //Priority is ignored intentionally so our HashMap will actually do its job.
        @Override
        public int hashCode() {
            switch(this.type){
                case APP_ICON_FULLY_QUALIFIED:
                    return (this.packageName.hashCode() * 31) +
                            (this.componentName.hashCode() * 31) +
                            (this.height * 31) +
                            (this.width * 31);
                case APP_ICON_PARTIALLY_QUALIFIED:
                    return ((this.packageName.hashCode() * 31) +
                            (this.height * 31) +
                            (this.width * 31)) * 23;
                case PACKAGE_LOCAL_RESOURCE:
                    return ((this.resourceId * 31) +
                            (this.height * 31) +
                            (this.width * 31)) * 13;
                case PACKAGE_FOREIGN_RESOURCE:
                    return ((this.packageName.hashCode() * 31) +
                            (this.resourceName.hashCode() * 31) +
                            (this.height * 31) +
                            (this.width * 31)) * 37;
                default:
                    return -1;
            }
        }
    }

    //Helpful to cache
    private Resources baseResources;
    private PackageManager pm;

    //The primary caches
    private Map<IconFetchTask, Bitmap> iconCache = Collections.synchronizedMap(new HashMap<IconFetchTask, Bitmap>());

    //Lock for task structure access
    private final List<IconFetchTask> taskList = new LinkedList<IconFetchTask>();
    private final Object taskListLock = new Object();

    private static IconCache instance;

    private Paint defaultPaint;
    //Dummy bitmap to return while still retrieving bitmaps
    public Bitmap dummyBitmap;

    /**
     * The AsyncTask for bitmap retrieval.
     */
    private class BitmapRetrievalTask extends AsyncTask<IconFetchTask, Void, Bitmap> {
        IconFetchTask task;

        @Override
        protected Bitmap doInBackground(IconFetchTask... params) {
            task = params[0];

            try {
                log("Finding bitmap with free memory of " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " MiB");

                //This is worst-case fallback
                if(Utilities.isRunningOutOfHeap()){
                    log("Stalling while running out of heap!");
                    System.gc();
                    while(Runtime.getRuntime().freeMemory() / 1024 < 512){
                        try {
                            Thread.sleep(100);
                        } catch (Exception ignored) {}
                    }
                    log("Resuming after stall...");
                }


                Drawable d;
                switch(task.getType()){
                    case APP_ICON_FULLY_QUALIFIED:
                        ComponentName cm = new ComponentName(task.getPackageName(), task.getComponentName());
                        d = pm.getActivityIcon(cm);
                        break;
                    case APP_ICON_PARTIALLY_QUALIFIED:
                        d = pm.getApplicationIcon(task.getPackageName());
                        break;
                    case PACKAGE_LOCAL_RESOURCE:
                        d = baseResources.getDrawable(task.getResourceId(), null);
                        break;
                    case PACKAGE_FOREIGN_RESOURCE:
                        Resources res = pm.getResourcesForApplication(task.getPackageName());
                        int resourceId = res.getIdentifier(task.getResourceName(), "drawable", task.getPackageName());
                        d = res.getDrawable(resourceId, null);
                        break;
                    default:
                        return null;
                }

                Bitmap toDraw = Bitmap.createBitmap(task.getWidth(), task.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(toDraw);
                d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                d.draw(canvas);

                /* Old way of doing things -- I *think* this results in us keeping refs to Drawables with potentially large bitmaps but I could be wrong
                if (d instanceof BitmapDrawable) {
                    Bitmap iconBitmap = ((BitmapDrawable) d).getBitmap();

                    Canvas canvas = new Canvas(toDraw);
                    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    canvas.drawBitmap(iconBitmap, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), defaultPaint);
                    d.draw(canvas);
                } else { //Draw it to a canvas backed
                    Canvas canvas = new Canvas(toDraw);
                    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    d.draw(canvas);
                }
                */

                log(task, "Successful icon fetch");
                return toDraw;
            } catch (PackageManager.NameNotFoundException e) {
                log(task, "Unable to retrieve icon for package; " + e.getMessage());
                return null;
            } catch (Resources.NotFoundException e) {
                log(task, "Unable to retrieve icon pack entry; " + e.getMessage());
                return null;
            } catch (OutOfMemoryError e) { //OOM errors, usually
                log(task, "OutOfMemory while retrieving icon");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if(result != null) {
                iconCache.put(task, result);
                if(task.getCallback() != null) {
                    task.getCallback().onRetrievalComplete(result);
                    task.clearCallback();
                }
            }
            popFromQueueAndRun();
        }

        @Override
        protected void onCancelled() {
            log("TASK CANCELLED!!!! AHHH!!!!!");
            popFromQueueAndRun();
        }
    }

    private IconCache() {
        pm = ApplicationClass.getInstance().getPackageManager();
        baseResources = ApplicationClass.getInstance().getResources();

        //Create the dummy bitmap
        int dimens = ApplicationClass.getInstance().getResources().getDisplayMetrics().widthPixels / 8;
        dummyBitmap = Bitmap.createBitmap(dimens, dimens, Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(dummyBitmap);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setAlpha(123);
        c.drawCircle(dimens / 2, dimens / 2, dimens / 2, p);

        defaultPaint = new Paint();
        defaultPaint.setAntiAlias(true);
    }

    public static IconCache getInstance() {
        return instance == null ? (instance = new IconCache()) : instance;
    }

    public void invalidateCaches() {
        iconCache.clear();
    }

    public interface ItemRetrievalInterface {
        /**
         * Called when a new item is retrieved. *ONLY* called when the initially returned value wasn't fresh.
         * @param result
         */
        void onRetrievalComplete(Bitmap result);
    }

    public Bitmap getAppIcon(String packageName, String componentName, IconFetchPriority priority, int size, ItemRetrievalInterface retrievalInterface) {
        IconFetchTask task = new IconFetchTask(priority, IconFetchType.APP_ICON_FULLY_QUALIFIED, packageName, componentName, null, -1, size, size, retrievalInterface);
        Bitmap cachedValue = iconCache.get(task);
        if (cachedValue != null) {
            log("Cache hit for getAppIcon " + packageName);
            return cachedValue;
        } else {
            queueItemTask(task);
            return dummyBitmap;
        }
    }

    public Bitmap getPackageIcon(String packageName, IconFetchPriority priority, int size, ItemRetrievalInterface retrievalInterface) {
        IconFetchTask task = new IconFetchTask(priority, IconFetchType.APP_ICON_PARTIALLY_QUALIFIED, packageName, null, null, -1, size, size, retrievalInterface);
        Bitmap cachedValue = iconCache.get(task);
        if (cachedValue != null) {
            log("Cache hit for getPackageIcon " + packageName);
            return cachedValue;
        } else {
            queueItemTask(task);
            return dummyBitmap;
        }
    }

    public Bitmap getLocalResource(int resourceId, IconFetchPriority priority, int size, ItemRetrievalInterface retrievalInterface) {
        IconFetchTask task = new IconFetchTask(priority, IconFetchType.PACKAGE_LOCAL_RESOURCE, null, null, null, resourceId, size, size, retrievalInterface);
        Bitmap cachedValue = iconCache.get(task);
        if (cachedValue != null) {
            log("Cache hit for getLocalResource " + resourceId);
            return cachedValue;
        } else {
            queueItemTask(task);
            return dummyBitmap;
        }
    }

    public Bitmap getForeignResource(String packageName, String resourceName, IconFetchPriority priority, int size, ItemRetrievalInterface retrievalInterface) {
        IconFetchTask task = new IconFetchTask(priority, IconFetchType.PACKAGE_FOREIGN_RESOURCE, packageName, null, resourceName, -1, size, size, retrievalInterface);
        Bitmap cachedValue = iconCache.get(task);
        if (cachedValue != null) {
            log("Cache hit for foreign resource " + resourceName + " from " + packageName);
            return cachedValue;
        } else {
            queueItemTask(task);
            return dummyBitmap;
        }
    }

    private void queueItemTask(IconFetchTask task){
        synchronized (taskListLock) {
            taskList.add(task);
        }
        popFromQueueAndRun();
    }

    private void popFromQueueAndRun() {
        synchronized (taskListLock) {
            //(1) Choose the most relevant task to run and run it
            IconFetchTask bestTask = null;
            for (IconFetchTask task : taskList) {
                if (bestTask == null || task.getPriority().sortValue > bestTask.getPriority().sortValue) {
                    bestTask = task;
                    if (task.getPriority().sortValue == IconFetchPriority.HIGHEST_PRIORITY.sortValue) {
                        break;
                    }
                }
            }
            if (bestTask == null) return;

            BitmapRetrievalTask getter = new BitmapRetrievalTask();

            try {
                getter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bestTask);
                taskList.remove(bestTask);
            } catch (RejectedExecutionException ignored) {
            }
        }
    }

    private void log(String content){
        if(!QUIET) Log.d(TAG, content);
    }

    private void log(IconFetchTask task, String content){
        if(!QUIET) {
            String prefix = "[?]";
            switch(task.getType()){
                case APP_ICON_PARTIALLY_QUALIFIED:
                case APP_ICON_FULLY_QUALIFIED:
                    prefix = "[App icon for " + task.getPackageName() + "] ";
                    break;
                case PACKAGE_LOCAL_RESOURCE:
                    prefix = "[Local res of ID " + task.getResourceId() + "] ";
                    break;
                case PACKAGE_FOREIGN_RESOURCE:
                    prefix = "[Foreign res of pkg/id " + task.getPackageName() + "/" + task.getResourceName() + "] ";
                    break;
            }
            Log.d(TAG, prefix + content);
        }
    }

    private void log(String context, Exception e){
        if(!QUIET) Log.e(TAG, context, e);
    }
}
