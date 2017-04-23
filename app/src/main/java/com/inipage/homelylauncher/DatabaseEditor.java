package com.inipage.homelylauncher;


import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import com.inipage.homelylauncher.drawer.ApplicationHiderIcon;
import com.inipage.homelylauncher.model.Favorite;

import java.util.ArrayList;
import java.util.List;

public class DatabaseEditor {
    private static DatabaseEditor instance;

    private SQLiteDatabase mDb;

    private DatabaseEditor(){
        mDb = new DatabaseHelper(ApplicationClass.getInstance()).getWritableDatabase();
    }

    public static DatabaseEditor getInstance(){
        return instance == null ? (instance = new DatabaseEditor()) : instance;
    }

    /**
     * Return all favorites, sorted by row.
     * @return All favorites.
     */
    public List<Favorite> getFavorites(){
        List<Favorite> favs = new ArrayList<>();
        Cursor c = mDb.query(DatabaseHelper.TABLE_FAVORITES, null, null, null, null, null, DatabaseHelper.COLUMN_POSITION_Y + " asc");
        if(c.moveToFirst()){
            int idCol = c.getColumnIndex(DatabaseHelper.COLUMN_ID);
            int widthCol = c.getColumnIndex(DatabaseHelper.COLUMN_WIDTH);
            int heightCol = c.getColumnIndex(DatabaseHelper.COLUMN_HEIGHT);
            int posXCol = c.getColumnIndex(DatabaseHelper.COLUMN_POSITION_X);
            int posYCol = c.getColumnIndex(DatabaseHelper.COLUMN_POSITION_Y);
            int favCol = c.getColumnIndex(DatabaseHelper.COLUMN_FAVORITE_TYPE);
            int ds1Col = c.getColumnIndex(DatabaseHelper.COLUMN_DATA_STRING_1);
            int ds2Col = c.getColumnIndex(DatabaseHelper.COLUMN_DATA_STRING_2);
            int di1Col = c.getColumnIndex(DatabaseHelper.COLUMN_DATA_INT_1);
            int cfCol = c.getColumnIndex(DatabaseHelper.COLUMN_CONTAINING_FOLDER);

            while(!c.isAfterLast()){
                favs.add(new Favorite(c.getInt(idCol),
                        c.getInt(favCol),
                        c.getInt(posXCol),
                        c.getInt(posYCol),
                        c.getInt(widthCol),
                        c.getInt(heightCol),
                        c.getInt(cfCol),
                        c.getString(ds1Col),
                        c.getString(ds2Col),
                        c.getInt(di1Col)));
                c.moveToNext();
            }
        }
        c.close();
        return favs;
    }

    /**
     * Save all favorites to the database.
     * @param favorites The favorites to save.
     */
    public void saveFavorites(List<Favorite> favorites){
        mDb.delete(DatabaseHelper.TABLE_FAVORITES, null, null);
        for(Favorite f : favorites){
            ContentValues cv = new ContentValues();

            cv.put(DatabaseHelper.COLUMN_WIDTH, f.getWidth());
            cv.put(DatabaseHelper.COLUMN_HEIGHT, f.getHeight());
            cv.put(DatabaseHelper.COLUMN_POSITION_X, f.getPositionX(false));
            cv.put(DatabaseHelper.COLUMN_POSITION_Y, f.getPositionY(false));
            cv.put(DatabaseHelper.COLUMN_FAVORITE_TYPE, f.getType());
            cv.put(DatabaseHelper.COLUMN_DATA_STRING_1, f.getDataString1());
            cv.put(DatabaseHelper.COLUMN_DATA_STRING_2, f.getDataString2());
            cv.put(DatabaseHelper.COLUMN_DATA_INT_1, f.getDataInt1());
            cv.put(DatabaseHelper.COLUMN_CONTAINING_FOLDER, f.getContainingFolder());

            mDb.insert(DatabaseHelper.TABLE_FAVORITES, null, cv);
        }
    }

    /**
     * Mark an app as hidden.
     * @param activityName The activity.
     * @param packageName The package.
     */
    public void markAppHidden(String activityName, String packageName) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_ACTIVITY_NAME, activityName);
        cv.put(DatabaseHelper.COLUMN_PACKAGE, packageName);
        long result = mDb.insert(DatabaseHelper.TABLE_HIDDEN_APPS, null, cv);
    }

    /**
     * Unmark an app as hidden.
     * @param activityName The activity.
     * @param packageName The package.
     */
    public void unmarkAppHidden(String activityName, String packageName){
        mDb.delete(DatabaseHelper.TABLE_HIDDEN_APPS,
                DatabaseHelper.COLUMN_ACTIVITY_NAME + "=? AND " + DatabaseHelper.COLUMN_PACKAGE + "=?",
                new String[] { activityName, packageName });
    }

    /**
     * Get all currently hidden apps.
     * @return All hidden apps.
     */
    public List<ComponentName> getHiddenApps(){
        List<ComponentName> result = new ArrayList<>();
        Cursor loadItems = mDb.query(DatabaseHelper.TABLE_HIDDEN_APPS, null, null, null, null, null, null);
        if (loadItems.moveToFirst()) {
            int packageColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_PACKAGE);
            int activityColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_ACTIVITY_NAME);

            while (!loadItems.isAfterLast()) {
                String packageName = loadItems.getString(packageColumn);
                String activityName = loadItems.getString(activityColumn);

                result.add(new ComponentName(packageName, activityName));
                loadItems.moveToNext();
            }
        }
        loadItems.close();
        return result;
    }
}
