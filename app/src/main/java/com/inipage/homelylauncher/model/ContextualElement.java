package com.inipage.homelylauncher.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;

import com.inipage.homelylauncher.R;

public class ContextualElement {
    public static final String INTERNAL_HINT_OPEN_SEARCH = "open_search";

    private Intent openIntent;
    private boolean isInternal;
    private String internalHint;
    private String label;
    private Drawable mainDrawable;
    private Drawable secondaryDrawable;

    public ContextualElement(Intent openIntent, String label, Drawable mainDrawable, Drawable secondaryDrawable) {
        this.openIntent = openIntent;
        this.isInternal = false;
        this.label = label;
        this.mainDrawable = mainDrawable;
        this.secondaryDrawable = secondaryDrawable;
    }

    public ContextualElement(String internalHint, String label, Drawable mainDrawable, Drawable secondaryDrawable) {
        this.internalHint = internalHint;
        this.isInternal = true;
        this.label = label;
        this.mainDrawable = mainDrawable;
        this.secondaryDrawable = secondaryDrawable;
    }

    public static ContextualElement createInternalSearch(Context ctx){
        String label = ctx.getString(R.string.search_apps);
        Drawable primaryDrawable = ctx.getResources().getDrawable(R.drawable.ic_apps_white_48dp);
        Drawable secondaryDrawable = ctx.getResources().getDrawable(R.drawable.ic_search_white_48dp);
        return new ContextualElement(INTERNAL_HINT_OPEN_SEARCH, label, primaryDrawable, secondaryDrawable);
    }

    public static ContextualElement createFromApp(String label, Context ctx, int resourceId, ComponentName cn){
        Intent open = new Intent();
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        open.setComponent(cn);

        try {
            ActivityInfo ai = ctx.getPackageManager().getActivityInfo(cn, 0);
            Drawable secondaryDrawable = ai.loadIcon(ctx.getPackageManager());
            Drawable primaryDrawable = ctx.getResources().getDrawable(resourceId);
            return new ContextualElement(open, label, primaryDrawable, secondaryDrawable);
        } catch (Exception e){
            return null;
        }
    }
}
