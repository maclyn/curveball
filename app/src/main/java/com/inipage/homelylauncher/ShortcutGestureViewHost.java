package com.inipage.homelylauncher;

public interface ShortcutGestureViewHost {
    public boolean hasWidget(String packageName);
    public void showWidget(String packageName);
    public void hideWidgetOverlay();
}
