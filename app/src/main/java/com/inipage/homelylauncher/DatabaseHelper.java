package com.inipage.homelylauncher;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String TAG = "DatabaseHelper";

    //Database basics
    public static final String DATABASE_NAME = "database.db";
    public static final int DATABASE_VERSION = 8;

    //Base columns
    public static final String COLUMN_ID = "_id";

    //Rows table
    public static final String TABLE_ROWS = "rows_table";
    public static final String COLUMN_DATA = "data_string";
    public static final String COLUMN_GRAPHIC = "graphic_res";
    public static final String COLUMN_GRAPHIC_PACKAGE = "graphic_package";
    public static final String COLUMN_ORDER = "ordering";
    public static final String COLUMN_TITLE = "title";

    //Hidden apps table
    public static final String TABLE_HIDDEN_APPS = "hidden_apps_table";
    public static final String COLUMN_PACKAGE = "package_name";
    public static final String COLUMN_ACTIVITY_NAME = "activity_name";

    //Widget table
    public static final String TABLE_WIDGETS = "widgets_table";
    public static final String COLUMN_WIDGET_ID = "widget_id";

    //Smartapps table
    public static final String TABLE_SMARTAPPS = "smartapps_table";
    //Re-uses: COLUMN_PACKAGE
    public static final String COLUMN_WHEN_TO_SHOW = "when_to_show";
    public static final int SMARTAPP_SHOW_ALWAYS = 1;
    public static final int SMARTAPP_SHOW_NEVER = 2;


    //Favorites table
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_FAVORITE_TYPE = "fav_type";
    public static final int FAVORITE_TYPE_APP = 1; /* Package name, activity name {str@2} */
    public static final int FAVORITE_TYPE_WIDGET = 2; /* Widget ID {int@1} */
    public static final int FAVORITE_TYPE_SHORTCUT = 3; /* TODO: Find out what this entails {?} */
    public static final int FAVORITE_TYPE_FOLDER = 4; /* Handled by CONTAINING_FOLDER, POSITION, but also a name {str@1} */
    public static final String COLUMN_POSITION_X = "position_x";
    public static final String COLUMN_POSITION_Y = "position_y";
    public static final String COLUMN_HEIGHT = "height";
    public static final String COLUMN_WIDTH = "width";
    public static final String COLUMN_CONTAINING_FOLDER = "in_folder";
    public static final String COLUMN_DATA_STRING_1 = "ds1";
    public static final String COLUMN_DATA_STRING_2 = "ds2";
    public static final String COLUMN_DATA_INT_1 = "di1";

    //Functions for creating it
    private static final String ROWS_TABLE_CREATE = "create table "
            + TABLE_ROWS +
            "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_DATA + " text not null, "
            + COLUMN_GRAPHIC + " text not null, "
            + COLUMN_GRAPHIC_PACKAGE + " text not null, "
            + COLUMN_ORDER + " integer not null, "
            + COLUMN_TITLE + " text not null);";
    private static final String HIDDEN_APPS_TABLE_CREATE = "create table "
            + TABLE_HIDDEN_APPS +
            "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_PACKAGE + " text not null, "
            + COLUMN_ACTIVITY_NAME + " text not null);";
    private static final String WIDGET_TABLE_CREATE = "create table "
            + TABLE_WIDGETS +
            "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_WIDGET_ID + " integer not null);";
    private static final String SMARTAPPS_TABLE_CREATE = "create table "
            + TABLE_SMARTAPPS +
            "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_PACKAGE + " text not null, "
            + COLUMN_WHEN_TO_SHOW + " integer not null" + ");";

    private static final String FAVORITES_TABLE_CREATE = "CREATE TABLE "
            + TABLE_FAVORITES
            + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_POSITION_X + " integer not null, "
            + COLUMN_POSITION_Y + " integer not null, "
            + COLUMN_HEIGHT + " integer not null, "
            + COLUMN_WIDTH + " integer not null, "
            + COLUMN_CONTAINING_FOLDER + " integer not null, " // Has to be either -1 or a valid ID!
            + COLUMN_FAVORITE_TYPE + " integer not null, "
            + COLUMN_DATA_STRING_1 + " text, "
            + COLUMN_DATA_STRING_2 + " text, "
            + COLUMN_DATA_INT_1 + " integer);";

    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(ROWS_TABLE_CREATE);
            db.execSQL(WIDGET_TABLE_CREATE);
            db.execSQL(HIDDEN_APPS_TABLE_CREATE);
            db.execSQL(SMARTAPPS_TABLE_CREATE);
            db.execSQL(FAVORITES_TABLE_CREATE);
            Log.d(TAG, "Created!");
        } catch (Exception e) {
            Log.d(TAG, "Failed to create!");
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) { //v1 didn't have a widget table
            db.execSQL(WIDGET_TABLE_CREATE);
            db.execSQL(SMARTAPPS_TABLE_CREATE);
        }

        if (oldVersion < 2){ //v2 didn't have a smartapps table
            db.execSQL(SMARTAPPS_TABLE_CREATE);
        }

        if (oldVersion < 5) { //v3-v4's widget table is corrupt!
            db.execSQL("DROP TABLE " + TABLE_WIDGETS);
            db.execSQL(WIDGET_TABLE_CREATE);
        }

        if (oldVersion < 6){
            db.execSQL("DROP TABLE " + TABLE_SMARTAPPS);
            db.execSQL(SMARTAPPS_TABLE_CREATE);
        }

        if (oldVersion < 8){
            try {
                db.execSQL("DROP TABLE " + TABLE_FAVORITES);
            } catch (Exception ignored) {} //Might fail; whatever
            db.execSQL(FAVORITES_TABLE_CREATE);
        }
    }
}
