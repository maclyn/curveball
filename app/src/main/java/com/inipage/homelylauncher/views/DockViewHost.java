package com.inipage.homelylauncher.views;

public interface DockViewHost {
    void onElementRemoved(DockElement oldElement, int index);
    void onElementReplaced(DockElement oldElement, DockElement newElement, int index);
}
