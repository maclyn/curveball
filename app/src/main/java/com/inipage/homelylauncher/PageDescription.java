package com.inipage.homelylauncher;

import android.graphics.drawable.Drawable;

public class PageDescription {
    String title;
    String description;
    String phrase;
    String url;
    Drawable icon;

    public PageDescription(String title, String description, String phrase, String url, Drawable icon) {
        this.title = title;
        this.description = description;
        this.phrase = phrase;
        this.url = url;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPhrase() {
        return phrase;
    }

    public String getUrl() {
        return url;
    }

    public Drawable getIcon() {
        return icon;
    }
}
