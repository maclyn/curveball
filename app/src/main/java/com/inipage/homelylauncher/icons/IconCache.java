package com.inipage.homelylauncher.icons;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.LruCache;
import android.widget.ImageView;

import com.inipage.homelylauncher.ApplicationClass;

import java.util.ArrayList;
import java.util.List;

public class IconCache {
    public static final int APP_DRAWER_TASK = 1;
    public static final int DOCK_TASK = 2;
    public static final int SWIPE_ICON_TASK = 3;
    public static final int SMARTBAR_ICON_TASK = 4;

    //Needed tools
    Resources baseResources;
    PackageManager pm;

    private LruCache<String, Bitmap> iconCache;

    private List<Pair<Integer, BitmapRetrievalTask>> taskList;

    private static IconCache instance;

    private class BitmapRetrievalTask extends AsyncTask<Object, Void, Boolean> {
        Pair<Integer, BitmapRetrievalTask> taskRef;
        String tag;
        ImageView location;
        Integer taskType;

        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                taskType = (Integer) params[0];
                tag = (String) params[1];
                location = (ImageView) params[2];
                String packageName = (String) params[3];
                taskRef = (Pair<Integer, BitmapRetrievalTask>) params[4];
                Integer dimensionsHint = (Integer) params[5];
                //Up to here is always the same

                Drawable d;
                Bitmap toDraw;
                try {
                    if(taskType == APP_DRAWER_TASK || taskType ==  DOCK_TASK) {
                        String componentName = (String) params[6];
                        ComponentName cm = new ComponentName(packageName, componentName);
                        d = pm.getActivityIcon(cm);
                    } else if (taskType == SMARTBAR_ICON_TASK){
                        //We're just grabbing the package icon
                        d = pm.getApplicationIcon(packageName);
                    } else if (taskType == SWIPE_ICON_TASK){
                        //TODO
                        d = pm.getApplicationIcon(packageName);
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
            try {
                if (result && location.getTag() != null && location.getTag().equals(tag)) {
                    location.setImageBitmap(iconCache.get(tag));
                } else {
                    location.setImageResource(android.R.drawable.sym_def_app_icon);
                }

                //Remove task from list
                taskList.remove(taskRef);
            } catch (Error memoryError) {
                //Sobs.
            }
        }
    }

    private IconCache(){
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        iconCache =  new LruCache<>(maxMemory / 8); //Reserves 1/8th total memory

        pm = ApplicationClass.getInstance().getPackageManager();
        baseResources = ApplicationClass.getInstance().getResources();

        taskList = new ArrayList<>();
    }

    public static IconCache getInstance(){
        return instance == null ? (instance = new IconCache()) : instance;
    }

    public void cacheSmallIcon(String packageName, String componentName, Bitmap icon){
        iconCache.put(packageName + "|" + componentName, icon);
    }

    public Bitmap retrieveSmallIcon(String packageName, String componentName){
        return iconCache.get(packageName + "|" + componentName);
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

        getter.execute(taskType, key, place, packageName, pair, place.getWidth(), componentName);
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

        getter.execute(SMARTBAR_ICON_TASK, key, place, packageName, pair, place.getWidth());
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
        cancelTaskImpl(SWIPE_ICON_TASK);
    }
}
