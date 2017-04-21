package com.inipage.homelylauncher.model;

import android.content.ComponentName;
import android.database.Cursor;

import com.inipage.homelylauncher.DatabaseHelper;

import java.util.Random;

public class Favorite {
    private int mId;
    private int mType;
    private int mPositionX;
    private int mPositionY;
    private int mWidth;
    private int mHeight;
    private int mContainingFolder;
    private String mDataString1;
    private String mDataString2;
    private int mDataInt1;

    /**
     * Construct for inflating a Favorite from the database. Params are self-explanatory.
     */
    public Favorite(int mId, int mType, int mPositionX, int mPositionY, int mWidth, int mHeight,
                    int mContainingFolder, String mDataString1, String mDataString2, int mDataInt1) {
        this.mId = mId;
        this.mType = mType;
        this.mPositionX = mPositionX;
        this.mPositionY = mPositionY;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mContainingFolder = mContainingFolder;
        this.mDataString1 = mDataString1;
        this.mDataString2 = mDataString2;
        this.mDataInt1 = mDataInt1;
    }

    /**
     * Constructor for creating a Favorite on-the-fly. Just missing ID (which is set to nonsense).
     */
    public Favorite(int mType, int mPositionX, int mPositionY, int mWidth, int mHeight,
                    int mContainingFolder, String mDataString1, String mDataString2, int mDataInt1) {
        this.mId = new Random().nextInt();
        this.mType = mType;
        this.mPositionX = mPositionX;
        this.mPositionY = mPositionY;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mContainingFolder = mContainingFolder;
        this.mDataString1 = mDataString1;
        this.mDataString2 = mDataString2;
        this.mDataInt1 = mDataInt1;
    }

    public boolean isFavoriteApp(){
        return mType == DatabaseHelper.FAVORITE_TYPE_APP;
    }

    public boolean isFavoriteFolder(){
        return mType == DatabaseHelper.FAVORITE_TYPE_FOLDER;
    }

    public boolean isFavoriteWidget(){
        return mType == DatabaseHelper.FAVORITE_TYPE_SHORTCUT;
    }

    public boolean isFavoriteShortcut(){
        return mType == DatabaseHelper.FAVORITE_TYPE_SHORTCUT;
    }

    public ComponentName getComponentName(){
        if(!isFavoriteApp()) throw new RuntimeException("Tried to get Component out of non-app!");
        return new ComponentName(mDataString1, mDataString2);
    }

    public int getWidgetId(){
        if(!isFavoriteWidget()) throw new RuntimeException("Tried to get widget ID of non-widget!");
        return mDataInt1;
    }

    public void getShortcutData(){
        throw new RuntimeException("not impl!");
    }

    public String getFolderName(){
        if(!isFavoriteFolder()) throw new RuntimeException("Tried to get folder name for non-folder!");
        return mDataString1;
    }

    public int getPositionX() {
        return mPositionX;
    }

    public int getPositionY() {
        return mPositionY;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getContainingFolder() {
        return mContainingFolder;
    }

    public int getType() {
        return mType;
    }

    public String getDataString1() {
        return mDataString1;
    }

    public String getDataString2() {
        return mDataString2;
    }

    public int getDataInt1() {
        return mDataInt1;
    }

    public Favorite setType(int mType) {
        this.mType = mType;
        return this;
    }

    public Favorite setPositionX(int mPositionX) {
        this.mPositionX = mPositionX;
        return this;
    }

    public Favorite setPositionY(int mPositionY) {
        this.mPositionY = mPositionY;
        return this;
    }

    public Favorite setWidth(int mWidth) {
        this.mWidth = mWidth;
        return this;
    }

    public Favorite setHeight(int mHeight) {
        this.mHeight = mHeight;
        return this;
    }

    public Favorite setContainingFolder(int mContainingFolder) {
        this.mContainingFolder = mContainingFolder;
        return this;
    }

    public Favorite setDataString1(String mDataString1) {
        this.mDataString1 = mDataString1;
        return this;
    }

    public Favorite setDataString2(String mDataString2) {
        this.mDataString2 = mDataString2;
        return this;
    }

    public Favorite setDataInt1(int mDataInt1) {
        this.mDataInt1 = mDataInt1;
        return this;
    }

    public int getId() {
        return mId;
    }
}
