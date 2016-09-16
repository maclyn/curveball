package com.inipage.homelylauncher.views;

import android.util.Pair;

import com.inipage.homelylauncher.TypeCard;
import com.inipage.homelylauncher.drawer.ApplicationIcon;

import java.util.List;

public interface ShortcutGestureViewHost {
    /* Widget management */

    boolean hasWidget(String packageName);
    void showWidget(String packageName);
    void collapseWidgetDrawer();

    /* Folder data management */

    void showCreateFolderDialog(ApplicationIcon ai);
    void persistList(List<TypeCard> samples);

    /* Tell us we need to change */

    void invalidateGestureView();

    /* Folder options */

    void showEditFolderDialog(int folderIndex);
    void batchOpen(int folderIndex);

    /* Adjust the host's UI */

    void brightenScreen();
    void dimScreen();
    void showTopElements();
    void showBottomElements();
    void hideTopElements();
    void hideBottomElements();

    /* Helpful draw information */

    /**
     * Polling for information about where to draw.
     * @return Pair with top in first and bottom in second
     **/
    Pair<Float, Float> getBoundsWhenNotFullscreen();
    float getTopMargin();
    float getBottomMargin();
}
