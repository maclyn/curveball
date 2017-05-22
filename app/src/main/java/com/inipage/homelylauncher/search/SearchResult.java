package com.inipage.homelylauncher.search;

import android.graphics.drawable.Drawable;

import com.inipage.homelylauncher.drawer.ApplicationIcon;

public class SearchResult {
    public enum SearchResultType {
        APP_RESULT, WEB_SUGGESTION
    }

    private String title;
    private ApplicationIcon appData;
    private String urlData;
    private SearchResultType type;

    public SearchResult(ApplicationIcon ai){
        this.appData = ai;
        this.type = SearchResultType.APP_RESULT;
        this.title = ai.getName();
    }

    public SearchResult(String suggestion){
        this.urlData = "https://www.google.com/search?q=" + suggestion;
        this.type = SearchResultType.WEB_SUGGESTION;
        this.title = suggestion;
    }

    public SearchResultType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public ApplicationIcon getAppData() {
        return appData;
    }

    public String getUrlData() {
        return urlData;
    }
}
