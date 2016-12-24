package com.inipage.homelylauncher.views;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.inipage.homelylauncher.BuildConfig;
import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.utils.AttributeApplier;
import com.inipage.homelylauncher.utils.SizeAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.*;

public class ShortcutGestureView extends View {
    //region Constants
    private static final String TAG = "ShortcutGestureView";
    private static final boolean NEEDLE = true; //For debug purposes
    private static final long WIDGET_HOLD_DURATION = 1200;
    //endregion

    //region Related classes
    public static class ShortcutCard {
        public enum TypeCardType {
            APP, ROW
        }

        private String drawablePackage;
        private String drawableName;
        private String title;
        private List<Pair<String, String>> apps;
        private TypeCardType type;

        public ShortcutCard(String title, String drawableName, List<Pair<String, String>> apps) {
            this.title = title;
            this.drawablePackage = getClass().getPackage().getName();
            this.drawableName = drawableName;
            this.apps = apps;
            this.type = TypeCardType.ROW;
        }

        public ShortcutCard(Pair<String, String> appName){
            this.title = "";
            this.drawablePackage = getClass().getPackage().getName();
            this.drawableName = "ic_launcher";
            this.apps = new ArrayList<>();
            this.apps.add(appName);
            this.type = TypeCardType.APP;
        }

        public ShortcutCard(String title, String drawablePackage, String drawableName, List<Pair<String, String>> apps) {
            this.title = title;
            this.drawablePackage = drawablePackage;
            this.drawableName = drawableName;
            this.apps = apps;
        }

        public String getDrawableName() {
            return drawableName;
        }

        public String getDrawablePackage(){
            return drawablePackage;
        }

        public void setDrawable(String dRes, String dPkg){
            this.drawableName = dRes;
            this.drawablePackage = dPkg;
        }

        public List<Pair<String, String>> getPackages() {
            return apps;
        }

        public String getTitle(){
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public TypeCardType getType(){
            return this.type;
        }
    }

    public static interface ShortcutGestureViewHost {
        /* Widget management */

        boolean hasWidget(String packageName);

        void showWidget(String packageName);

        void collapseWidgetDrawer();

        /* Folder data management */

        void showCreateFolderDialog(ApplicationIcon ai);

        void persistList(List<ShortcutCard> samples);

        /* Tell us we need to change */

        void invalidateGestureView();

        /* Folder options */

        void showEditFolderDialog(int folderIndex);

        void batchOpen(int folderIndex);

        /* Adjust the host's UI */

        void clearBackgroundTint();

        void setAndAnimateToDarkBackgroundTint();

        void showTopElements();

        void showBottomElements();

        void hideTopElements();

        void hideBottomElements();

        /* Helpful draw information */

        /**
         * Polling for information about where to draw.
         *
         * @return Pair with top in first and bottom in second
         **/
        Pair<Float, Float> getBoundsWhenNotFullscreen();

        float getTopMargin();

        float getBottomMargin();
    }
    //endregion

    //region Enums
    /**
     * What is the program's drawing state?
     */
    private enum ViewMode {
        /** Dragging an ApplicationIcon over this. **/
        ADDING_ICON,
        /** Not interacting with the view. **/
        NONE,
        /** Choosing a folder to open. **/
        CHOOSING_FOLDER,
        /** Choosing an app to open. **/
        CHOOSING_APP,
        /** Choosing an option when in a folder. **/
        CHOOSING_OPTION,
        /** Showing nothing -- widget drawer in front */
        SHOWING_NOTHING
    }

    private enum ScreenSide {
        LEFT_SIDE, RIGHT_SIDE
    }
    //endregion

    //region Size attributes
    @SizeAttribute(value = 48, setting = "icon_size_pref")
    float iconSize;
    @SizeAttribute(value = 60, setting = "big_icon_size_pref")
    float bigIconSize;
    @SizeAttribute(28)
    float previewIconSize;
    @SizeAttribute(40)
    float previewIconPadding;
    @SizeAttribute(8)
    float previewIconHorizontalPadding;
    @SizeAttribute(10)
    float strokeWidth;

    //Movement values
    @SizeAttribute(value = 60, setting = "element_size_pref")
    float maxTouchElementSize;
    @SizeAttribute(80)
    float horizontalDx;

    //Layout values
    @SizeAttribute(10)
    float verticalOffset;
    @SizeAttribute(12)
    float verticalPadding;
    @SizeAttribute(8)
    float iconPadding;

    //Size of text onscreen
    @SizeAttribute(attrType = SizeAttribute.AttributeType.SP, value = 22)
    float textSize;
    @SizeAttribute(attrType = SizeAttribute.AttributeType.SP, value = 28)
    float bigTextSize;

    //Help attributes
    @SizeAttribute(attrType = SizeAttribute.AttributeType.SP, value = 18)
    float helpTextSize;
    @SizeAttribute(16)
    float helpPadding;
    @SizeAttribute(4)
    float arrowPadding;
    @SizeAttribute(24)
    float helpArrowSize;
    @SizeAttribute(64)
    float widgetPadding;
    //endregion

    //Options representing images
    private static final Integer[] folderOptionsDrawables = new Integer[] {
            R.drawable.ic_mode_edit_white_24dp,
            R.drawable.ic_clear_white_48dp,
            R.drawable.ic_open_in_new_white_24dp };
    private static final Integer[] folderOptionsTitles = new Integer[] {
            R.string.edit_folder,
            R.string.cancel,
            R.string.open_all_in_folder };

    float iconSizeDifference;
    float textSizeDifference;

    //Slop values
    float touchSlop;
    float edgeSlop;

    String hint;

    Paint labelPaint; //Paint for the text on each icon
    Paint touchPaint; //Paint for touch trail
    Paint glowPaint; //Paint for side glows
    Paint transparencyPaint; //Paint for drawing "transparent" bitmaps

    Rect outRect = new Rect(); //Rectangle used for centering

    RectF scratchRectF = new RectF(); //Scratch rectangles used for drwaing operations
    RectF scratchRectF2 = new RectF();
    Rect scratchRect = new Rect();
    Rect scratchRect2 = new Rect();

    ShortcutGestureViewHost host;

    //Data set used
    List<ShortcutCard> data;

    //Initial data when you start
    float startX = 0;
    float startY = 0;

    //Gesture start locations (when the gesture was actually detected by the system)
    float gestureStartX = 0;
    float gestureStartY = 0;

    //Current touch/drag location (this changes)
    float touchX = 0;
    float touchY = 0;
    float lastTouchY = Integer.MIN_VALUE;
    float lastTouchX = Integer.MIN_VALUE;

    //Start time
    long startTime = -1;

    //Mode we're in
    ViewMode cm;

    //Folder we're working in
    int selectedFolder = 0;

    //Selected item (could be an (a) folder, (b) app, or (c) option
    int selectedY = 0;

    //Values calculated at start
    float drawStartY = 0;
    float scrollYPerPixel = 0;

    //List of touch events (logged from ACTION_MOVE)
    final List<Pair<Float, Float>> touchEventList = new ArrayList<>();
    long lastTouchEvent = 0;

    Runnable touchEventRunnable;

    /** Map for caching color of drawables -- HashCode of Bitmap --> Color **/
    Map<Integer, Integer> bitmapColorMap;
    /** Map for caching the "label" of apps -- Package + | + Component --> Label **/
    Map<String, String> labelMap;

    //Interface to pass to IconCache(...) after it's found a given Bitmap (only run if the Bitmap we wan't hasn't
    //been cached yet)
    private IconCache.ItemRetrievalInterface retrievalInterface = new IconCache.ItemRetrievalInterface() {
        @Override
        public void onRetrievalComplete(Bitmap result) {
            if(!bitmapColorMap.containsKey(result.hashCode()))
                getIconColorForBitmap(result);
            invalidate();
        }
    };

    //Whether we should reject the next touch event (return false from onTouchEvent)
    boolean rejectNextEvent = false;

    //For widget
    Timer timer;
    private long timerStart = -1l;
    private boolean timerCompleted = false;

    public ShortcutGestureView(Context context) {
        super(context);
        init();
    }

    public ShortcutGestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShortcutGestureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        touchEventRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (touchEventList) {
                    if (lastTouchEvent + 100 < System.currentTimeMillis()) {
                        if (touchEventList.size() > 0) {
                            touchEventList.remove(touchEventList.size() - 1);
                            invalidate();
                        }
                    }
                    cleanTouchEvents();
                }
            }
        };

        AttributeApplier.ApplyDensity(this, getContext());

        iconSizeDifference = bigIconSize - iconSize;
        textSizeDifference = bigTextSize - textSize;

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        edgeSlop = ViewConfiguration.get(getContext()).getScaledEdgeSlop();

        labelPaint = new Paint();
        labelPaint.setAntiAlias(true);
        labelPaint.setColor(getResources().getColor(R.color.white));
        labelPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        touchPaint = new Paint();
        touchPaint.setAntiAlias(true);
        touchPaint.setColor(getResources().getColor(R.color.white));
        touchPaint.setStrokeCap(Paint.Cap.ROUND);
        touchPaint.setStyle(Paint.Style.STROKE);
        touchPaint.setStrokeWidth(strokeWidth);
        touchPaint.setPathEffect(new CornerPathEffect(strokeWidth / 2));

        glowPaint = new Paint();
        glowPaint.setAntiAlias(true);
        glowPaint.setColor(getResources().getColor(android.R.color.white));
        glowPaint.setAlpha(180);
        glowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        transparencyPaint = new Paint();
        transparencyPaint.setAntiAlias(true);
        transparencyPaint.setAlpha(255);

        hint = getContext().getString(R.string.drop_icon_hint);

        labelMap = new HashMap<>();
        bitmapColorMap = new HashMap<>();

        timer = new Timer();

        cm = ViewMode.NONE;
    }

    /**
     * ShortcutGestureView is tightly integrated into
     */
    public void setActivity(ShortcutGestureViewHost host){
        this.host = host;
    }

    public void onActivityResumed(){
        AttributeApplier.ApplyDensity(this, getContext());
    }

    @Override
    public synchronized boolean onDragEvent(DragEvent event) {
        if (event.getLocalState() instanceof ApplicationIcon) {
            touchX = event.getX();
            touchY = event.getY();
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    startX = event.getX();
                    startY = event.getY();
                    startTime = System.currentTimeMillis();
                    log("Drag started " + startX + " " + startY, true);
                    cm = ViewMode.ADDING_ICON;

                    invalidate();
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    selectedY = -1;
                    invalidate();
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    updateSelectedDragItem(event.getY());
                    invalidate();
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    log("Drag ended", true);
                    if (cm == ViewMode.ADDING_ICON && selectedY >= 0) {
                        ApplicationIcon ai = (ApplicationIcon) event.getLocalState();

                        if (selectedY == data.size()) { //Add a new row
                            log("Adding to new row...", true);
                            host.showCreateFolderDialog(ai);
                        } else {
                            log("Adding to an old row...", true);
                            data.get(selectedY).getPackages().add(new Pair<>
                                    (ai.getPackageName(), ai.getActivityName()));
                            host.persistList(data);
                            preloadCard(data.get(selectedY));
                        }
                    } else {
                        log("Invalid state to end drag...", true);
                    }
                    resetState();
                    break;
            }
            return true;
        } else {
            log("Ignoring drag event; not dragging an application icon", false);
            return false;
        }
    }

    private void initTouchOp(MotionEvent event){
        startTime = System.currentTimeMillis();
        startX = event.getX();
        startY = event.getY();

        log("Start X : " + startX + "/Start Y : " + startY, false);
        log("Height: " + getHeight() + "/Width: " + getWidth(), false);

        if (data.size() > 0) {
            float totalSize = (data.size() - 1) * iconSize;
            totalSize += bigIconSize;
            drawStartY = (getHeight() / 2) - (totalSize / 2);
            if (drawStartY >= 0) {
                scrollYPerPixel = 0;
            } else {
                float totalNeededToMove = abs(drawStartY) * 2;
                scrollYPerPixel = totalNeededToMove / ((getY() + getHeight()) - startY);
            }
        }
    }

    //Update element we internally noted as selected
    private void updateSelectedDragItem(float location){
        int numRows = data.size() + 1; //At least one/two

        float percent = location / (float) (getHeight() - host.getBottomMargin());
        log("Percent is: " + percent, false);
        float selection = percent * numRows;
        log("Raw selection is: " + selection, false);
        selectedY = (int) floor(selection);
        if (selectedY < 0) selectedY = 0;
        if (selectedY > data.size()) selectedY = data.size();

        log("Selected element is : " + selectedY, false);
        invalidate();
    }

    //Update element we internally note as selected as an app
    private void updateSelectedTouchItem(){
        int numRows = data.size(); //We actually can have 0 here

        if (numRows == 0) {
            selectedY = -1; //Show "no folders"; nothing to do here, actually
        } else { //Calculate selected item
            switch(cm){
                case CHOOSING_FOLDER:
                    choosingFolderScope: {
                        if ((touchX - gestureStartX) > horizontalDx) {
                            log("Switching modes with values (touchY, gSY, tS): " + touchY + " " +
                                    gestureStartY + " " + touchSlop, false);
                            gestureStartY = touchY;
                            gestureStartX = touchX;
                            selectedFolder = selectedY;
                            selectedY = -1;
                            cm = ViewMode.CHOOSING_APP;

                            //To avoid glitches, we call this again
                            updateSelectedTouchItem();
                            return;
                        }

                        float topLocation = getY();
                        float bottomLocation = getY() + getHeight();
                        boolean closerToTop = (gestureStartY - topLocation) < (bottomLocation - gestureStartY);
                        float workingSpace;
                        if (closerToTop) {
                            workingSpace = (gestureStartY - topLocation) * 2f;
                        } else {
                            workingSpace = (bottomLocation - gestureStartY) * 2f;
                        }

                        int icons = data.size();
                        float perElementSize = workingSpace / icons;
                        if (perElementSize > maxTouchElementSize) {
                            perElementSize = maxTouchElementSize;
                        }

                        //Update workingSpace
                        workingSpace = (icons * perElementSize);

                        //(2) Center touch location around the start
                        log("getY() and start and icons" + topLocation + " " + bottomLocation + "" + icons, false);
                        log("Per element size: " + perElementSize, false);

                        float centerLocation = gestureStartY;
                        float startLocation = gestureStartY - (workingSpace / 2);
                        float endLocation = gestureStartY + (workingSpace / 2);

                        float percent = (touchY - startLocation) / workingSpace;
                        log("Percent: " + percent, false);

                        if (percent < 0f) {
                            selectedY = 0;
                        } else if (percent > 1f) {
                            selectedY = icons - 1;
                        } else {
                            log("Valid bounds; selecting from defaults", false);
                            selectedY = (int) (percent * (float) icons);
                        }
                        log("Selected Y: " + selectedY, false);
                    }
                    break;
                case CHOOSING_APP:
                    appScope: {
                        //Switch to folder options
                        if (touchX < (gestureStartX - edgeSlop)) {
                            gestureStartY = touchY;
                            gestureStartX = touchX;
                            cm = ViewMode.CHOOSING_OPTION;

                            timer.cancel();
                            timer = new Timer();

                            //To avoid glitches, we call this again
                            updateSelectedTouchItem();
                            return;
                        }

                        int icons = data.get(selectedFolder).getPackages().size();
                        if (icons == 0) { //This would be a serious problem
                            selectedY = -1;
                            invalidate();
                            return;
                        }

                        //Calculate valid bounds
                        //(1) We want even space amounts
                        float topLocation = getY();
                        float bottomLocation = getY() + getHeight();
                        boolean closerToTop = (gestureStartY - topLocation) < (bottomLocation - gestureStartY);
                        float workingSpace;
                        if (closerToTop) {
                            workingSpace = (gestureStartY - topLocation) * 2f;
                        } else {
                            workingSpace = (bottomLocation - gestureStartY) * 2f;
                        }
                        float perElementSize = workingSpace / icons;
                        if (perElementSize > maxTouchElementSize) {
                            perElementSize = maxTouchElementSize;
                        }
                        //Update workingSpace
                        workingSpace = (icons * perElementSize);

                        //(2) Center touch location around the start
                        log("getY() and start and icons" + topLocation + " " + bottomLocation + "" + icons, false);
                        log("Per element size: " + perElementSize, false);

                        float centerLocation = gestureStartY;
                        float startLocation = gestureStartY - (workingSpace / 2);
                        float endLocation = gestureStartY + (workingSpace / 2);

                        float percent = (touchY - startLocation) / workingSpace;
                        log("Percent: " + percent, false);

                        int oldSelectedY = selectedY;
                        if (percent < 0f) {
                            selectedY = 0;
                        } else if (percent > 1f) {
                            selectedY = icons - 1;
                        } else {
                            log("Valid bounds; selecting from defaults", false);
                            selectedY = (int) (percent * (float) icons);
                        }
                        if(selectedY >= data.get(selectedFolder).getPackages().size()){
                            selectedY = data.get(selectedFolder).getPackages().size() - 1;
                        }
                        log("Selected Y: " + selectedY, false);

                        if(selectedY != oldSelectedY || oldSelectedY == -1){
                            timer.cancel();
                            timer = new Timer();

                            final String appPackage = data.get(selectedFolder).getPackages().get(selectedY).first;

                            //We just switched selectedY's; check if there's a widget
                            if(host.hasWidget(appPackage)){
                                //Start timer for 1 second
                                timer.schedule(new TimerTask() { //Open widget
                                    @Override
                                    public void run() {
                                        resetState();
                                        cm = ViewMode.SHOWING_NOTHING;
                                        postInvalidate();

                                        rejectNextEvent = true;
                                        timerCompleted = true;
                                        host.showWidget(appPackage);
                                    }
                                }, WIDGET_HOLD_DURATION);
                                timer.scheduleAtFixedRate(new TimerTask() { //Update display
                                    @Override
                                    public void run() {
                                        getHandler().post(new Runnable() {
                                            @Override
                                            public void run() {
                                                invalidate(); //Animation the widget open circle.
                                            }
                                        });
                                    }
                                }, 0L, 1000 / 60);
                                timerStart = System.currentTimeMillis();
                            } else {
                                timerStart = -1L;
                            }
                        }
                        break;
                    }
                case CHOOSING_OPTION:
                    optionScope: {
                        float topLocation = getY();
                        float bottomLocation = getY() + getHeight();
                        boolean closerToTop = (gestureStartY - topLocation) < (bottomLocation - gestureStartY);
                        float workingSpace;
                        if (closerToTop) {
                            workingSpace = (gestureStartY - topLocation) * 2f;
                        } else {
                            workingSpace = (bottomLocation - gestureStartY) * 2f;
                        }

                        int icons = folderOptionsDrawables.length;
                        float perElementSize = workingSpace / icons;
                        if (perElementSize > maxTouchElementSize) {
                            perElementSize = maxTouchElementSize;
                        }

                        //Update workingSpace
                        workingSpace = (icons * perElementSize);

                        //(2) Center touch location around the start
                        log("getY() and start and icons" + topLocation + " " + bottomLocation + "" + icons, false);
                        log("Per element size: " + perElementSize, false);

                        float centerLocation = gestureStartY;
                        float startLocation = gestureStartY - (workingSpace / 2);
                        float endLocation = gestureStartY + (workingSpace / 2);

                        float percent = (touchY - startLocation) / workingSpace;
                        log("Percent: " + percent, false);

                        if (percent < 0f) {
                            selectedY = 0;
                        } else if (percent > 1f) {
                            selectedY = icons - 1;
                        } else {
                            log("Valid bounds; selecting from defaults", false);
                            selectedY = (int) (percent * (float) icons);
                        }
                        log("Selected Y: " + selectedY, false);
                        break;
                    }
            }
        }
        log("Selected Y element is : " + selectedY, false);
        invalidate();
    }

    private void openSelectedItem() {
        log("Open item with a selected folder/icon of: " + selectedFolder + "/" + selectedY +
                "and mode =" + cm.name(), true);

        switch(cm){
            case CHOOSING_FOLDER:
                resetState();
                host.clearBackgroundTint();
                host.showTopElements();
                host.showBottomElements();
                break;
            case CHOOSING_APP:
                choosingAppScope: {
                    if(selectedY >= 0 && selectedFolder != -1){
                        Pair<String, String> appToLaunch = data.get(selectedFolder).getPackages()
                                .get(selectedY);
                        Intent appLaunch = new Intent();
                        appLaunch.setClassName(appToLaunch.first, appToLaunch.second);
                        appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            getContext().startActivity(appLaunch);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Unable to start. Application may be uninstalled/upgrading.", Toast.LENGTH_SHORT).show();
                        }

                        resetState();
                        host.clearBackgroundTint();
                        host.showTopElements();
                        host.showBottomElements();
                    }
                }
                break;
            case CHOOSING_OPTION:
                choosingOptionScope: {
                    switch(selectedY) {
                        case 0:
                            host.showEditFolderDialog(selectedFolder);
                            break;
                        case 1:
                            break;
                        case 2:
                            host.batchOpen(selectedFolder);
                            break;
                    }

                    resetState();
                    host.clearBackgroundTint();
                    host.showTopElements();
                    host.showBottomElements();
                }
        }
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if(isInEditMode()) return;

        switch(cm) {
            case ADDING_ICON:
                drawAddIcon(canvas);
                break;
            case CHOOSING_FOLDER:
                drawFolderSelection(canvas);
                if(data.size() != 0)
                    drawHelp(canvas, "Swipe right to select folder", "", false, true);
                break;
            case CHOOSING_APP:
                drawAppSelection(canvas);
                drawHelp(canvas, "Release to open app", "Swipe left for folder options", true, false);
                break;
            case CHOOSING_OPTION:
                drawFolderOptions(canvas);
                drawHelp(canvas, "Release to select option", "", false, false);
                break;
            case SHOWING_NOTHING:
                break; //Nothing!
            case NONE:
                drawFolderHints(canvas);
                break;
        }
    }

    private void drawHelp(Canvas c, String helpText, String subText, boolean showLeftArrow,
                          boolean showRightArrow){
        //For the first 1000ms of display, it's a "fade in"
        float durationOfEvent = System.currentTimeMillis() - startTime;
        float alphaMultiplier;
        if(durationOfEvent > 1000){
            alphaMultiplier = 1;
        } else {
            alphaMultiplier = durationOfEvent / 1000f;
        }

        //Draw the main text
        labelPaint.setAlpha((int) (240f * alphaMultiplier));
        labelPaint.setTextSize(helpTextSize);
        labelPaint.getTextBounds(helpText, 0, helpText.length(), outRect);

        //Center text horizontally
        int x = getWidth() / 2;
        x -= (outRect.width() / 2);

        //Center text vertically
        float centerYLine = helpPadding;
        centerYLine -= (outRect.height() / 2);
        centerYLine += outRect.height();

        c.drawText(helpText, x, centerYLine, labelPaint);

        if(subText.length() > 0){
            float subCenterYLine = helpPadding + outRect.height() + (helpPadding / 4);

            //Draw the sub-text
            labelPaint.setAlpha((int) (240f * alphaMultiplier));
            labelPaint.setTextSize((float) (helpTextSize * 0.75));
            labelPaint.getTextBounds(subText, 0, subText.length(), outRect);

            //Center text horizontally
            int subX = getWidth() / 2;
            subX -= (outRect.width() / 2);

            //Center text vertically
            subCenterYLine -= (outRect.height() / 2);
            subCenterYLine += outRect.height();

            c.drawText(subText, subX, subCenterYLine, labelPaint);
        }

        Bitmap left = IconCache.getInstance().getLocalResource(R.drawable.ic_keyboard_arrow_left_white_18dp, IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) helpArrowSize, retrievalInterface);
        scratchRect2.set(0, 0, left.getWidth(), left.getHeight());
        scratchRect.set( (int) (x - arrowPadding - helpArrowSize), (int) (centerYLine - (outRect.height() / 4) - (helpArrowSize / 2)),
                (int) (x - arrowPadding), (int) (centerYLine - (outRect.height() / 4) + (helpArrowSize / 2)));
        transparencyPaint.setAlpha((int) (alphaMultiplier * (showLeftArrow ? 240f : 0f)));
        c.drawBitmap(left, scratchRect2, scratchRect, transparencyPaint);

        Bitmap right = IconCache.getInstance().getLocalResource(R.drawable.ic_keyboard_arrow_right_white_18dp, IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) helpArrowSize, retrievalInterface);
        scratchRect2.set(0, 0, right.getWidth(), right.getHeight());
        scratchRect.set( (int) (x + outRect.width() + arrowPadding), (int) (centerYLine - (outRect.height() / 4) - (helpArrowSize / 2)),
                (int) (x + outRect.width() + arrowPadding + helpArrowSize),
                (int) (centerYLine - (outRect.height() / 4) + (helpArrowSize / 2)));
        transparencyPaint.setAlpha((int) (alphaMultiplier * (showRightArrow ? 240f : 0f)));
        c.drawBitmap(right, scratchRect2, scratchRect, transparencyPaint);
    }

    private void drawFolderOptions(Canvas canvas) {
        List<Pair<Float, Float>> sizeQueue = new ArrayList<>();

        float totalSize = (folderOptionsDrawables.length - 1) * (iconSize + iconPadding);
        totalSize += bigIconSize;

        float iconsStartY = (getHeight() / 2) - (totalSize / 2);
        float iconsScrollY = 0;
        if(iconsStartY >= 0){
            iconsScrollY = 0;
        } else {
            float totalNeededToMove = abs(iconsStartY) * 2;
            iconsScrollY = totalNeededToMove / gestureStartY;
        }

        int numIcons = folderOptionsDrawables.length;

        float topLocation = getY();
        float bottomLocation = getY() + getHeight();
        boolean closerToTop = (gestureStartY - topLocation) < (bottomLocation - gestureStartY);

        float workingSpace;
        if(closerToTop){
            workingSpace = (gestureStartY - topLocation) * 2f;
        } else {
            workingSpace = (bottomLocation - gestureStartY) * 2f;
        }

        float perElementSize = workingSpace / numIcons;
        if (perElementSize > maxTouchElementSize) {
            perElementSize = maxTouchElementSize;
        }
        workingSpace = (numIcons * perElementSize);

        float centerLocation = gestureStartY;
        float startLocation = gestureStartY - (workingSpace / 2);
        float endLocation = gestureStartY + (workingSpace / 2);

        float percent = (touchY - startLocation) / workingSpace;
        log("Percent: " + percent, false);

        float halfPerElementSize = (float) (perElementSize * 0.5);

        if(touchY < (startLocation + perElementSize / 2)){
            sizeQueue.add(new Pair<>(bigIconSize, bigTextSize));
            for(int i = 1; i < folderOptionsDrawables.length; i++){
                sizeQueue.add(new Pair<>(iconSize, textSize));
            }
        } else if (touchY > (endLocation - (perElementSize / 2))){
            for(int i = 0; i < folderOptionsDrawables.length - 1; i++){
                sizeQueue.add(new Pair<>(iconSize, textSize));
            }
            sizeQueue.add(new Pair<>(bigIconSize, bigTextSize));
        } else {
            float idealPosition = (startLocation + (perElementSize * selectedY) +
                    halfPerElementSize);
            float minFor = idealPosition - halfPerElementSize;
            float maxFor = idealPosition + halfPerElementSize;
            for(int i = 0; i < folderOptionsDrawables.length; i++){
                log("At position __, touchY/ideal/min/max/selectedY: " + i + " " + touchY + " " + idealPosition + " " +
                        minFor + " " + maxFor + " " + selectedY, false);
                if(i == selectedY){
                    log("Calculating from selectedY", false);
                    float drawPercent = 1f - ((maxFor - touchY) / perElementSize); //By calculation, this will be between 0 and 1
                    log("Draw percent: " + drawPercent, false);
                    float diff;
                    if(drawPercent > 0.5f){
                        diff = 1.5f - drawPercent;
                    } else {
                        diff = 0.5f + drawPercent;
                    }

                    sizeQueue.add(new Pair<>(iconSize + (diff * iconSizeDifference),
                            textSize + (diff * textSizeDifference)));
                } else if (i == (selectedY - 1)){
                    //log("Calculating from selectedY - 1", true);
                    if(touchY <= idealPosition){ //We're involved
                        float difference = 0.5f - ((touchY - minFor) / perElementSize);
                        log("SelectedY - 1 VALID at position " + i + " difference " + difference, false);
                        sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                textSize + (difference * textSizeDifference)));
                    } else { //Not so much
                        sizeQueue.add(new Pair<>(iconSize, textSize));
                    }
                } else if (i == (selectedY + 1)){
                    //log("Calculating from selectedY + 1", true);
                    if(touchY >= idealPosition){ //We're involved
                        float difference = 0.5f - ((maxFor - touchY) / perElementSize);
                        log("SelectedY + 1 VALID at position " + i + " difference " + difference, false);
                        sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                textSize + (difference * textSizeDifference)));
                    } else { //Not so much
                        sizeQueue.add(new Pair<>(iconSize, textSize));
                    }
                } else { //Default case
                    sizeQueue.add(new Pair<>(iconSize, textSize));
                }
            }
        }

        float folderSize = (folderOptionsTitles.length - 1) * iconSize;
        folderSize += bigIconSize;
        float yPosition = (getHeight() / 2) - (folderSize / 2);;

        //Occasionally this'll temporarily glitch
        if(selectedY >= folderOptionsDrawables.length || selectedY < 0)
            return;

        for (int i = 0; i < folderOptionsDrawables.length ; i++) {
            Pair<Float, Float> sizes = sizeQueue.remove(0);
            drawLineToSide(canvas,
                    IconCache.getInstance().getLocalResource(folderOptionsDrawables[i], IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) bigIconSize, retrievalInterface),
                    getResources().getString(folderOptionsTitles[i]), selectedY == i, ScreenSide.LEFT_SIDE, edgeSlop, yPosition, sizes.first,
                    sizes.second, iconPadding);
            yPosition += (sizes.first + iconPadding);
        }
    }

    private void drawFolderHints(Canvas canvas) {
        //Draw icons in a line
        Pair<Float, Float> bounds = host.getBoundsWhenNotFullscreen();
        float top = bounds.first;
        float bottom = bounds.second;
        float space = bottom - top - (previewIconPadding * 2);

        if(space < 0) return; //Low-res display..? WEIRD.

        float spaceForEach = space / data.size();
        float startX = (getWidth() / 2) - (previewIconSize / 2);
        float endX = startX + previewIconSize;

        transparencyPaint.setAlpha(180);

        //Draw each folder icon with 1/2 opacity and a size of 10dp x 10dp in the center of the screen
        for(int i = 0; i < data.size(); i++){
            float startY = (float) (top + ((i + 0.5) * spaceForEach) - (previewIconSize / 2));
            float endY = startY + previewIconSize;

            ShortcutCard card = data.get(i);
            Bitmap icon = IconCache.getInstance().getForeignResource(card.getDrawablePackage(), card.getDrawableName(), IconCache.IconFetchPriority.SWIPE_FOLDER_ICONS, (int) bigIconSize, retrievalInterface);

            scratchRectF.set(startX, startY, endX, endY);
            canvas.drawBitmap(icon, null, scratchRectF, transparencyPaint);
        }
    }

    private void drawSuggestion(){

    }

    private void drawAddIcon(Canvas canvas) {
        int divisions = data.size() + 1;
        float roomForEach = (getHeight() - host.getBottomMargin()) / divisions;

        for(int i = 0; i < data.size(); i++){
            float start = roomForEach * i;
            float end = start + roomForEach;
            float middle = (start + end) / 2;

            //log("At i of " + i + ", start/end/middle: " + start + ", " + end + ", " + middle);

            drawCenteredLine(canvas, data.get(i).getDrawablePackage(), data.get(i).getDrawableName(),
                    data.get(i).getTitle(), getWidth() / 2, (int) middle, selectedY == i);
        }

        //Draw add icon at data.size()
        float addPosition = ((roomForEach * data.size()) + (roomForEach / 2));
        drawCenteredLine(canvas, R.drawable.ic_add_circle_outline_white_48dp, "Add row", getWidth() / 2,
                (int) addPosition, selectedY == data.size());
    }

    private void drawFolderSelection(Canvas canvas){
        List<Pair<Float, Float>> sizeQueue = new ArrayList<>();

        if (selectedY == -1){
            //Draw "no folder message" message
            drawCenteredLine(canvas, R.drawable.ic_info_white_48dp, "No folders yet",
                    getWidth() / 2, getHeight() / 2, true);
        } else { //Thar be valid data
            float totalSize = (data.size() - 1) * (iconSize + iconPadding);
            totalSize += bigIconSize;

            float iconsStartY = (getHeight() / 2) - (totalSize / 2);
            float iconsScrollY = 0;
            if(iconsStartY >= 0){
                iconsScrollY = 0;
            } else {
                float totalNeededToMove = abs(iconsStartY) * 2;
                iconsScrollY = totalNeededToMove / gestureStartY;
            }

            int numIcons = data.size();

            //NOTE: This is a wasteful recalculation; at some point this should be
            //stored during the switch from choosing a folder to choosing a set of icons.
            //This code is duplicated from earlier code.
            float topLocation = getY();
            float bottomLocation = getY() + getHeight();
            boolean closerToTop = (gestureStartY - topLocation) < (bottomLocation - gestureStartY);

            float workingSpace;
            if(closerToTop){
                workingSpace = (gestureStartY - topLocation) * 2f;
            } else {
                workingSpace = (bottomLocation - gestureStartY) * 2f;
            }

            float perElementSize = workingSpace / numIcons;
            if (perElementSize > maxTouchElementSize) {
                perElementSize = maxTouchElementSize;
            }
            workingSpace = (numIcons * perElementSize);

            float centerLocation = gestureStartY;
            float startLocation = gestureStartY - (workingSpace / 2);
            float endLocation = gestureStartY + (workingSpace / 2);

            float percent = (touchY - startLocation) / workingSpace;
            log("Percent: " + percent, false);

            float halfPerElementSize = (float) (perElementSize * 0.5);

            if(touchY < (startLocation + perElementSize / 2)){
                sizeQueue.add(new Pair<>(bigIconSize, bigTextSize));
                for(int i = 1; i < data.size(); i++){
                    sizeQueue.add(new Pair<>(iconSize, textSize));
                }
            } else if (touchY > (endLocation - (perElementSize / 2))){
                for(int i = 0; i < data.size() - 1; i++){
                    sizeQueue.add(new Pair<>(iconSize, textSize));
                }
                sizeQueue.add(new Pair<>(bigIconSize, bigTextSize));
            } else {
                float idealPosition = (startLocation + (perElementSize * selectedY) +
                        halfPerElementSize);
                float minFor = idealPosition - halfPerElementSize;
                float maxFor = idealPosition + halfPerElementSize;
                for(int i = 0; i < data.size(); i++){
                    log("At position __, touchY/ideal/min/max/selectedY: " + i + " " + touchY + " " + idealPosition + " " +
                            minFor + " " + maxFor + " " + selectedY, false);
                    if(i == selectedY){
                        log("Calculating from selectedY", false);
                        float drawPercent = 1f - ((maxFor - touchY) / perElementSize); //By calculation, this will be between 0 and 1
                        log("Draw percent: " + drawPercent, false);
                        float diff;
                        if(drawPercent > 0.5f){
                            diff = 1.5f - drawPercent;
                        } else {
                            diff = 0.5f + drawPercent;
                        }

                        sizeQueue.add(new Pair<>(iconSize + (diff * iconSizeDifference),
                                textSize + (diff * textSizeDifference)));
                    } else if (i == (selectedY - 1)){
                        if(touchY <= idealPosition){ //We're involved
                            float difference = 0.5f - ((touchY - minFor) / perElementSize);
                            log("SelectedY - 1 VALID at position " + i + " difference " + difference, false);
                            sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                    textSize + (difference * textSizeDifference)));
                        } else { //Not so much
                            sizeQueue.add(new Pair<>(iconSize, textSize));
                        }
                    } else if (i == (selectedY + 1)){
                        if(touchY >= idealPosition){ //We're involved
                            float difference = 0.5f - ((maxFor - touchY) / perElementSize);
                            log("SelectedY + 1 VALID at position " + i + " difference " + difference, false);
                            sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                    textSize + (difference * textSizeDifference)));
                        } else { //Not so much
                            sizeQueue.add(new Pair<>(iconSize, textSize));
                        }
                    } else { //Default case
                        //log("Calculating from default case", true);
                        sizeQueue.add(new Pair<>(iconSize, textSize));
                    }
                }
            }

            float yPosition = drawStartY;

            //Occasionally this'll temporarily glitch
            if(selectedY >= data.size())
                return;

            for (int i = 0; i < data.size(); i++) {
                Pair<Float, Float> sizes = sizeQueue.remove(0);
                drawLineToSide(canvas,
                        IconCache.getInstance().getForeignResource(data.get(i).getDrawablePackage(), data.get(i).getDrawableName(), IconCache.IconFetchPriority.SWIPE_FOLDER_ICONS, (int) bigIconSize, retrievalInterface),
                        data.get(i).getTitle(), selectedY == i, ScreenSide.LEFT_SIDE, edgeSlop, yPosition, sizes.first,
                        sizes.second, iconPadding);
                yPosition += (sizes.first + iconPadding);
            }

            drawFolderChildIcons(canvas, selectedY);
        }
    }

    private void drawFolderChildIcons(Canvas canvas, int folder){
        //Draw "folder child" icons in a line
        List<Pair<String, String>> packages = data.get(folder).getPackages();
        int subIconDivisions = packages.size();
        float subIconRoom = getHeight() / subIconDivisions;

        if(subIconRoom > (previewIconSize * 2)){
            subIconRoom = previewIconSize * 2;
        } else if (subIconRoom < previewIconSize) {
            subIconRoom = previewIconSize;
        }

        float spaceToUse = subIconDivisions * subIconRoom;
        float subIconStart = (getHeight() / 2) - (spaceToUse / 2);
        float offsetInDrawSpace = (subIconRoom - previewIconSize) / 2;

        float endX = getWidth() - previewIconHorizontalPadding;
        float startX = endX - previewIconSize;

        transparencyPaint.setAlpha(180);

        for(int j = 0; j < packages.size(); j++){
            float startY = subIconStart + (j * subIconRoom) + offsetInDrawSpace;
            float endY = startY + previewIconSize;

            Pair<String, String> app = packages.get(j);
            Bitmap b = IconCache.getInstance().getAppIcon(app.first, app.second, IconCache.IconFetchPriority.SWIPE_APP_ICONS, (int) bigIconSize, retrievalInterface);

            log("For package " + (j + 1) + " of " + packages.size(), false);

                /* Uncomment for debugging purposes
                Paint thickLine = new Paint();
                thickLine.setColor(getResources().getColor(R.color.white));
                thickLine.setStrokeWidth(10);
                canvas.drawLineToSide(0, startY, getWidth(), startY, thickLine);
                canvas.drawLineToSide(0, endY, getWidth(), endY, thickLine);
                */

            scratchRectF.set(startX, startY, endX, endY);
            canvas.drawBitmap(b, null, scratchRectF, transparencyPaint);
        }
    }

    private void drawAppSelection(Canvas canvas) {
        if(selectedY == -1){ //No apps flag
            drawCenteredLine(canvas, R.drawable.ic_info_white_48dp, "No apps in this folder",
                    getWidth() / 2, getHeight() / 2, true);
            return;
        }

        List<Pair<Float, Float>> sizeQueue = new ArrayList<>();
        List<Pair<String, String>> packages = data.get(selectedFolder).getPackages();

        float totalSize = (packages.size() - 1) * (iconSize + iconPadding);
        totalSize += bigIconSize;
        float iconsStartY = (getHeight() / 2) - (totalSize / 2);
        float iconsScrollY = 0;
        if(iconsStartY >= 0){
            iconsScrollY = 0;
        } else {
            float totalNeededToMove = abs(iconsStartY) * 2;
            iconsScrollY = totalNeededToMove / gestureStartY;
        }

        int numIcons = packages.size();

        //NOTE: This is a wasteful recalculation; at some point this should be
        //stored during the switch from choosing a folder to choosing a set of icons.
        //This code is duplicated from earlier code.
        float topLocation = getY();
        float bottomLocation = getY() + getHeight();
        boolean closerToTop = (gestureStartY - topLocation) < (bottomLocation - gestureStartY);
        float workingSpace;
        if(closerToTop){
            workingSpace = (gestureStartY - topLocation) * 2f;
        } else {
            workingSpace = (bottomLocation - gestureStartY) * 2f;
        }
        float perElementSize = workingSpace / numIcons;
        if (perElementSize > maxTouchElementSize) {
            perElementSize = maxTouchElementSize;
        }
        workingSpace = (numIcons * perElementSize);

        float centerLocation = gestureStartY;
        float startLocation = gestureStartY - (workingSpace / 2);
        float endLocation = gestureStartY + (workingSpace / 2);

        float percent = (touchY - startLocation) / workingSpace;
        log("Percent: " + percent, false);

        float halfPerElementSize = (float) (perElementSize * 0.5);

        if(touchY < (startLocation + halfPerElementSize)){
            sizeQueue.add(new Pair<>(bigIconSize, bigTextSize));
            for(int i = 1; i < numIcons; i++){
                sizeQueue.add(new Pair<>(iconSize, textSize));
            }
        } else if (touchY > (endLocation - halfPerElementSize)){
            for(int i = 0; i < numIcons - 1; i++){
                sizeQueue.add(new Pair<>(iconSize, textSize));
            }
            sizeQueue.add(new Pair<>(bigIconSize, bigTextSize));
        } else {
            float idealPosition = (startLocation + (perElementSize * selectedY) +
                    halfPerElementSize);
            float minFor = idealPosition - halfPerElementSize;
            float maxFor = idealPosition + halfPerElementSize;
            for(int i = 0; i < numIcons; i++){
                log("At position __, touchY/ideal/min/max/selectedY: " + i + " " + touchY + " " + idealPosition + " " +
                        minFor + " " + maxFor + " " + selectedY, false);
                if(i == selectedY){
                    log("Calculating from selectedY", false);
                    float drawPercent = 1f - ((maxFor - touchY) / perElementSize); //By calculation, this will be between 0 and 1
                    log("drawpercent: " + drawPercent, false);
                    float diff;
                    if(drawPercent > 0.5f){
                        diff = 1.5f - drawPercent;
                    } else {
                        diff = 0.5f + drawPercent;
                    }

                    sizeQueue.add(new Pair<>(iconSize + (diff * iconSizeDifference),
                            textSize + (diff * textSizeDifference)));
                } else if (i == (selectedY - 1)){
                    //log("Calculating from selectedY - 1", true);
                    if(touchY <= idealPosition){ //We're involved
                        float difference = 0.5f - ((touchY - minFor) / perElementSize);
                        log("SelectedY - 1 VALID at position " + i + " difference " + difference, false);
                        sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                textSize + (difference * textSizeDifference)));
                    } else { //Not so much
                        sizeQueue.add(new Pair<>(iconSize, textSize));
                    }
                } else if (i == (selectedY + 1)){
                    //log("Calculating from selectedY + 1", true);
                    if(touchY >= idealPosition){ //We're involved
                        float difference = 0.5f - ((maxFor - touchY) / perElementSize);
                        log("SelectedY + 1 VALID at position " + i + " difference " + difference, false);
                        sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                textSize + (difference * textSizeDifference)));
                    } else { //Not so much
                        sizeQueue.add(new Pair<>(iconSize, textSize));
                    }
                } else { //Default case
                    //log("Calculating from default case", true);
                    sizeQueue.add(new Pair<>(iconSize, textSize));
                }
            }
        }

        for(int i = 0; i < numIcons; i++){ //Package name/activity name
            Pair<Float, Float> sizes = sizeQueue.remove(0);

            Pair<String, String> app = packages.get(i);
            ComponentName cm = new ComponentName(app.first, app.second);

            Bitmap b = IconCache.getInstance().getAppIcon(app.first, app.second, IconCache.IconFetchPriority.SWIPE_APP_ICONS, (int) bigIconSize, retrievalInterface);
            String label = grabLabel(cm);

            //Right-justify the icons
            if(selectedY != i || timerStart == -1L) {
                drawLineToSide(canvas, b, label, selectedY == i, ScreenSide.RIGHT_SIDE, getWidth() - edgeSlop,
                        iconsStartY, sizes.first, sizes.second, iconPadding);
            } else { //SelectedY is a slightly modified version of drawLineToSide(...) but with text offset for widget data
                float x = getWidth() - edgeSlop;
                float y = iconsStartY;

                float margin = iconPadding;
                float iconSize = sizes.first;
                float textSize = sizes.second;

                //Add color to the icon row if selected
                int transparentColor = getResources().getColor(android.R.color.transparent);
                int color = getIconColorForBitmap(b);
                int paddingOverTwo = (int) (iconPadding / 2);

                //xStart/xEnd extremes are moderated by how much we've actually drawn
                int extendOfShading = getWidth() / 2;
                extendOfShading += (getWidth() / 4) * ((1 - (bigTextSize - textSize) / textSizeDifference));

                int shadingXStart = getWidth() - extendOfShading;
                int shadingXEnd = getWidth();

                Shader linear = new LinearGradient(shadingXStart,
                        y - paddingOverTwo,
                        shadingXEnd,
                        y + iconSize + paddingOverTwo,
                        transparentColor,
                        color,
                        Shader.TileMode.CLAMP);
                glowPaint.setShader(linear);
                canvas.drawRect(shadingXStart, y - paddingOverTwo, shadingXEnd, y + iconSize + paddingOverTwo,
                        glowPaint);

                //Draw the icon
                int bitmapXStart = (int) ((int) x - iconSize);
                int bitmapXEnd = (int) x;
                scratchRect.set(bitmapXStart, (int) y, bitmapXEnd, (int) (y + iconSize));
                scratchRect2.set(0, 0, b.getWidth(), b.getHeight());

                transparencyPaint.setAlpha(255);
                canvas.drawBitmap(b, scratchRect2, scratchRect, transparencyPaint);

                //Draw text from the text + subtext
                String text = label;
                String widgetText = "Hold for widget";

                //Calculations for text drawing place
                int spaceRemainingAfterText = (int) (iconSize + iconPadding);

                float adjustedTextSize = (float) (textSize * 0.8);
                float adjustedHelpTextSize = (float) (adjustedTextSize * 0.6);

                labelPaint.setAlpha(255);
                labelPaint.setTextSize(adjustedTextSize);
                labelPaint.getTextBounds(text, 0, text.length(), outRect);

                float mainTextHeight = outRect.height();
                float mainTextWidth = outRect.width();
                spaceRemainingAfterText -= mainTextHeight;

                labelPaint.setTextSize(adjustedHelpTextSize);
                labelPaint.getTextBounds(widgetText, 0, widgetText.length(), outRect);

                float subTextHeight = outRect.height();
                float subTextWidth = outRect.width();
                spaceRemainingAfterText -= subTextHeight;

                labelPaint.setTextSize(adjustedTextSize);
                labelPaint.getTextBounds(text, 0, text.length(), outRect);

                //Adjust x for text
                x -= iconSize;
                x -= margin;

                //Draw the text
                canvas.drawText(text, x - mainTextWidth, y - paddingOverTwo + (spaceRemainingAfterText / 2)
                        + (mainTextHeight / 2), labelPaint);

                //Work on the subtext
                labelPaint.setTextSize(adjustedHelpTextSize);
                labelPaint.getTextBounds(widgetText, 0, widgetText.length(), outRect);

                int white = getResources().getColor(R.color.white);
                int lighterWhite = Color.argb(120, 255, 255, 255);

                float startTransparency = ((float) (System.currentTimeMillis() - timerStart)) / ((float) WIDGET_HOLD_DURATION);
                float endTransparency = startTransparency + 0.1f > 1f ? 1f : startTransparency + 0.1f;

                log(startTransparency + " to " + endTransparency, false);

                LinearGradient scratchGradient = new LinearGradient(x - outRect.width(),
                        y - paddingOverTwo + (spaceRemainingAfterText / 2) + mainTextHeight + (mainTextHeight / 2),
                        x,
                        y - paddingOverTwo + (spaceRemainingAfterText / 2) + mainTextHeight + (mainTextHeight / 2),
                        new int[] { white, white, lighterWhite, lighterWhite },
                        new float[] { 0, startTransparency, endTransparency, 1 },
                        Shader.TileMode.CLAMP);
                labelPaint.setShader(scratchGradient);
                canvas.drawText(widgetText, x - outRect.width(), y - paddingOverTwo +
                        (spaceRemainingAfterText / 2) + mainTextHeight + (mainTextHeight / 2), labelPaint);
                labelPaint.setShader(null);
            }

            iconsStartY += sizes.first + iconPadding;
        }

        //Draw option icons in a line
        int subIconDivisions = folderOptionsDrawables.length;
        float subIconRoom = getHeight() / folderOptionsDrawables.length;

        if(subIconRoom > (previewIconSize * 2)){
            subIconRoom = previewIconSize * 2;
        } else if (subIconRoom < previewIconSize) {
            subIconRoom = previewIconSize;
        }

        float spaceToUse = subIconDivisions * subIconRoom;
        float subIconStart = (getHeight() / 2) - (spaceToUse / 2);
        float offsetInDrawSpace = (subIconRoom - previewIconSize) / 2;

        float startX = previewIconHorizontalPadding;
        float endX = startX + previewIconSize;

        transparencyPaint.setAlpha(180);

        for(int j = 0; j < folderOptionsDrawables.length; j++){
            float startY = subIconStart + (j * subIconRoom) + offsetInDrawSpace;
            float endY = startY + previewIconSize;

            Bitmap b = IconCache.getInstance().getLocalResource(folderOptionsDrawables[j], IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) bigIconSize, retrievalInterface);

            scratchRectF.set(startX, startY, endX, endY);
            canvas.drawBitmap(b, null, scratchRectF, transparencyPaint);
        }
    }

    private void drawTouchTrail(Canvas canvas) {
        Path p = new Path();

        synchronized (touchEventList) {
            if (touchEventList.size() > 0) {
                for (int i = 0; i < touchEventList.size(); i += 1) {
                    if (i == 0) {
                        p.moveTo(touchEventList.get(i).first, touchEventList.get(i).second);
                    } else {
                        p.lineTo(touchEventList.get(i).first, touchEventList.get(i).second);
                    }
                }

                touchPaint.setShader(
                        new LinearGradient(touchEventList.get(0).first,
                                touchEventList.get(0).second,
                                touchEventList.get(touchEventList.size() - 1).first,
                                touchEventList.get(touchEventList.size() - 1).second,
                                Color.TRANSPARENT, Color.WHITE, Shader.TileMode.CLAMP));
                canvas.drawPath(p, touchPaint);
            }
        }
    }

    public void invalidateCaches() {
        bitmapColorMap.clear();
        labelMap.clear();
    }

    /*
     * Call when the shortcuts represented in this view change.
     */
    public void notifyShortcutsChanged(){
        invalidate();
    }

    private synchronized int getIconColorForBitmap(Bitmap b){
        final int hashCode = b.hashCode();

        if(bitmapColorMap.containsKey(b.hashCode())){
            return bitmapColorMap.get(b.hashCode());
        }

        if(b == IconCache.getInstance().dummyBitmap){ //Wait until we have something valid to find the color of
            return Color.WHITE;
        }

        //TODO: Migrate to custom AsyncTask
        AsyncTask<Bitmap, Void, Palette> task = new AsyncTask<Bitmap, Void, Palette>(){
            @Override
            protected Palette doInBackground(Bitmap... params) {
                return Palette.from(params[0]).generate();
            }

            @Override
            protected void onPostExecute(Palette palette) {
                bitmapColorMap.put(hashCode, grabFromPalette(palette));
                invalidate(); //Stop just drawing white.;
            }
        };

        try {
            task.execute(b);
        } catch (Exception tooManyTasks){
            //Whoopsies. Better to do the improper thing (draw WHITE) than crash.
        }
        bitmapColorMap.put(hashCode, Color.WHITE);
        return Color.WHITE;
    }

    private int grabFromPalette(Palette p){
        int choice = Color.argb(200, 255, 255, 255);

        if(p.getVibrantSwatch() != null){
            choice = p.getVibrantSwatch().getRgb();
        } else if (p.getLightVibrantSwatch() != null) {
            choice = p.getLightVibrantSwatch().getRgb();
        } else if (p.getDarkVibrantSwatch() != null){
            choice = p.getDarkVibrantSwatch().getRgb();
        } else if (p.getLightMutedSwatch() != null){
            choice = p.getLightMutedSwatch().getRgb();
        } else if (p.getDarkMutedSwatch() != null){
            choice = p.getDarkMutedSwatch().getRgb();
        }

        return choice;
    }

    private String grabLabel(ComponentName cm){
        String key = cm.getPackageName() + cm.getClassName() + "-1";
        if(labelMap.containsKey(key)){
            return labelMap.get(key);
        }

        String label;
        try {
            label = (String)
                    getContext().getPackageManager().getActivityInfo(cm, 0)
                            .loadLabel(getContext().getPackageManager());
            if(label.length() > 16) label = label.substring(0, 16) + "...";
            labelMap.put(key, label);
        } catch (Exception ignored) {
            label = cm.getPackageName();
        }
        return label;
    }

    private int drawCenteredLine(Canvas c, int resId, String text, int x, int y, boolean selected){
        return drawCenteredLine(c, IconCache.getInstance().getLocalResource(resId, IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) bigIconSize, retrievalInterface),
                text, x, y, selected);
    }

    private int drawCenteredLine(Canvas c, String packageName, String resource, String text, int x, int y, boolean selected){
        return drawCenteredLine(c, IconCache.getInstance().getForeignResource(packageName, resource, IconCache.IconFetchPriority.SWIPE_FOLDER_ICONS, (int) bigIconSize, retrievalInterface),
                text, x, y, selected);
    }

    private int drawCenteredLine(Canvas c, Bitmap bitmap, String text, int x, int y, boolean selected){
        float totalHeight = 0;
        float totalWidth = 0;

        if(text.length() > 20) text = text.substring(0, 20) + "...";

        //totalHeight += iconSize + padding;
        totalWidth += iconSize + touchSlop;

        labelPaint.getTextBounds(text, 0, text.length(), outRect);
        totalWidth += outRect.width();

        //log("Total height/width: " + totalHeight + " " + totalWidth);
        //log("Icon size: " + iconSize);
        //log("Text size: " + outRect);

        //Draw icon at left-most
        int iconYStart = (int) (y - (iconSize / 2));
        int iconYEnd = (int) (iconYStart + iconSize);
        int iconXStart = (int) (x - (totalWidth / 2));
        int iconXEnd = (int) (iconXStart + iconSize);
        //log("X s/e, Y s/e: " + iconXStart + " " + iconXEnd + " " + iconYStart + " " + iconYEnd);

        transparencyPaint.setAlpha(selected ? 255 : 180);
        scratchRect2.set(iconXStart, iconYStart, iconXEnd, iconYEnd);
        scratchRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        c.drawBitmap(bitmap, scratchRect, scratchRect2, transparencyPaint);

        //Draw text too
        int textXStart = (int) (iconXEnd + touchSlop);
        //log("OutRect height: " + outRect.height());
        int textYStart = (y + (outRect.height() / 2));
        labelPaint.setAlpha(selected ? 255 : 180);
        labelPaint.setTextSize(textSize);
        c.drawText(text, textXStart, textYStart, labelPaint);

        return iconXStart;
    }

    /**
     * Draw a line. The line isn't centered by x, but the text is centered relative to the
     * icon.
     *  @param c Canvas to draw on.
     * @param b The Bitmap to draw. Must not be null.
     * @param text The text to draw with the bitmap.
     * @param selected Whether the element is selected (affects transparency).
     * @param side The side (left or right). to draw the line on.
     * @param xStart The start X position of the whole thing (drawing to left pushes things right,
*               and drawing to right pushes things left).
     * @param yStart The start Y position of the whole line.
     * @param lineIconSize The size of the icon in pixels.
     * @param lineTextSize The size of the text in pixels.
     * @param margin The margin between different lines.
     */
    private void drawLineToSide(Canvas c, Bitmap b, String text, boolean selected, ScreenSide side,
                                float xStart, float yStart, float lineIconSize, float lineTextSize,
                                float margin){
        float x = xStart;
        float y = yStart;

        //Add color to the icon row if selected
        if(selected){
            int transparentColor = getResources().getColor(android.R.color.transparent);
            int color = getIconColorForBitmap(b);
            int paddingOverTwo = (int) (iconPadding / 2);

            //xStart/xEnd extremes are moderated by how much we've actually drawn
            int extendOfShading = getWidth() / 2;
            extendOfShading += (getWidth() / 4) * ((1 - (bigTextSize - lineTextSize) / textSizeDifference));

            int shadingXStart = side == ScreenSide.LEFT_SIDE ? 0 : getWidth() - extendOfShading;
            int shadingXEnd = side == ScreenSide.LEFT_SIDE ? extendOfShading : getWidth();

            Shader linear = new LinearGradient(shadingXStart,
                    y - paddingOverTwo,
                    shadingXEnd,
                    y + lineIconSize + paddingOverTwo,
                    side == ScreenSide.LEFT_SIDE ? color : transparentColor,
                    side == ScreenSide.LEFT_SIDE ? transparentColor : color,
                    Shader.TileMode.CLAMP);
            glowPaint.setShader(linear);
            c.drawRect(shadingXStart, y - paddingOverTwo, shadingXEnd, y + lineIconSize + paddingOverTwo,
                    glowPaint);
        }

        //Draw the icon
        int bitmapXStart = (int) (side == ScreenSide.LEFT_SIDE ? x : x - lineIconSize);
        int bitmapXEnd = (int) (side == ScreenSide.LEFT_SIDE ? x + lineIconSize : x);
        scratchRect.set(bitmapXStart, (int) y, bitmapXEnd, (int) (y + lineIconSize));
        scratchRect2.set(0, 0, b.getWidth(), b.getHeight());

        transparencyPaint.setAlpha(255);
        c.drawBitmap(b, scratchRect2, scratchRect, transparencyPaint);

        //Draw the text
        labelPaint.setAlpha(selected ? 255 : 180);
        labelPaint.setTextSize(lineTextSize);
        labelPaint.getTextBounds(text, 0, text.length(), outRect);

        //Center text horizontally
        if(side == ScreenSide.LEFT_SIDE) {
            x += lineIconSize;
            x += margin;
        } else {
            x -= lineIconSize;
            x -= margin;
            x -= outRect.width(); //Draw text more to left of the icon
        }

        //Center text vertically
        float centerYLine = y + (lineIconSize / 2); //Now we're at the center of the drawable
        centerYLine -= (outRect.height() / 2); //Center it
        centerYLine += outRect.height();

        c.drawText(text, x, centerYLine, labelPaint);
    }

    void cleanTouchEvents(){
        if(getHandler() != null) getHandler().postDelayed(touchEventRunnable, 100);
    }

    @Override
    public synchronized boolean onTouchEvent(@NonNull MotionEvent event) {
        lastTouchEvent = System.currentTimeMillis();
        log("onTouchEvent at " + lastTouchEvent, false);

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(rejectNextEvent) return false;

                log("Motion event action down", false);

                if(cm == ViewMode.SHOWING_NOTHING){ //If a drawer or some such thing is open
                    host.collapseWidgetDrawer();
                    host.showTopElements();
                    host.showBottomElements();
                    host.clearBackgroundTint();
                    resetState();
                    return false;
                }

                cleanTouchEvents();
                initTouchOp(event);

                host.setAndAnimateToDarkBackgroundTint();
                host.hideTopElements();
                host.hideBottomElements();

                touchX = event.getX();
                touchY = event.getY();
                lastTouchX = touchX;
                lastTouchY = touchY;
                gestureStartX = touchX;
                gestureStartY = touchY;
                cm = ViewMode.CHOOSING_FOLDER;

                updateTouchEventsList();
                updateSelectedTouchItem();
                break;
            case MotionEvent.ACTION_MOVE:
                if(rejectNextEvent) return false;

                if(lastTouchY == Integer.MIN_VALUE){
                    lastTouchY = startY;
                } else {
                    lastTouchY = touchY;
                }
                if(lastTouchX == Integer.MIN_VALUE){
                    lastTouchX = startX;
                } else {
                    lastTouchX = touchX;
                }
                log("Motion event action move: " + event.getX() + " " + event.getY(), false);

                touchX = event.getX();
                touchY = event.getY();

                switch(cm){
                    case NONE:
                    case ADDING_ICON:
                        break;
                    case CHOOSING_FOLDER:
                    case CHOOSING_APP:
                    case CHOOSING_OPTION:
                        updateTouchEventsList();
                        updateSelectedTouchItem();
                        break;
                }
                break;
            case MotionEvent.ACTION_UP: //Up means we ought select something if we are in the right mode
                log("Motion event action up", false);

                if(rejectNextEvent){
                    rejectNextEvent = false;
                    return false;
                }

                switch(cm){
                    case NONE:
                    case ADDING_ICON:
                        break;
                    case CHOOSING_APP:
                    case CHOOSING_FOLDER:
                    case CHOOSING_OPTION:
                        log("Mode select icon; opening something perhaps...", false);
                        openSelectedItem();
                        break;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                log("Motion event action cancel", false);

                if(rejectNextEvent){
                    rejectNextEvent = false;
                    return false;
                }

                resetState();
                host.clearBackgroundTint();
                host.showTopElements();
                host.showBottomElements();
                break;
        }
        return true;
    }

    private void updateTouchEventsList() {
        //This updates the item so onDraw can do the right thing;
        //strictly speaking, this doesn't need to be in a function
        synchronized (touchEventList) {
            if (touchEventList.size() < 5) {
                touchEventList.add(new Pair<>(touchX, touchY));
            } else {
                touchEventList.remove(0);
                touchEventList.add(new Pair<>(touchX, touchY));
            }
        }
    }

    public void resetState(){
        cm = ViewMode.NONE;

        //Reset widget timer tasks
        timer.cancel();
        timer = new Timer();
        timerStart = -1;
        timerCompleted = false;

        startX = 0;
        startY = 0;
        touchX = 0;
        touchY = 0;
        startTime = -1;
        gestureStartX = 0;
        gestureStartY = 0;
        selectedY = 0;
        selectedFolder = 0;
        selectedY = 0;
        drawStartY = 0;
        scrollYPerPixel = 0;
        lastTouchY = Integer.MIN_VALUE;
        lastTouchX = Integer.MIN_VALUE;
        synchronized (touchEventList) { touchEventList.clear(); }

        this.postInvalidate();

        log("resetState() finished", false);
    }

    private void preloadCard(ShortcutCard card){
        IconCache.getInstance().getForeignResource(card.getDrawablePackage(), card.getDrawableName(), IconCache.IconFetchPriority.SWIPE_FOLDER_ICONS, (int) bigIconSize, retrievalInterface);
        for(Pair<String, String> icon : card.getPackages()){
            IconCache.getInstance().getAppIcon(icon.first, icon.second, IconCache.IconFetchPriority.SWIPE_APP_ICONS, (int) bigIconSize, retrievalInterface);
        }
    }

    private void preCache(){
        if(data == null) return;

        //Grab all the icons for folder icons + app icons
        for(ShortcutCard card : data){
            preloadCard(card);
        }

        //Grab the icons we get from internal sources
        IconCache.getInstance().getLocalResource(R.drawable.ic_add_circle_outline_white_48dp, IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) bigIconSize, retrievalInterface);
        IconCache.getInstance().getLocalResource(R.drawable.ic_info_white_48dp, IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) bigIconSize, retrievalInterface);
        IconCache.getInstance().getLocalResource(R.drawable.ic_clear_white_48dp, IconCache.IconFetchPriority.BUILT_IN_ICONS, (int) bigIconSize, retrievalInterface);
    }

    public void setCards(List<ShortcutCard> cards){
        this.data = cards;
        preCache();
    }

    private void log(String text, boolean hasFocus){
        //noinspection PointlessBooleanExpression
        if(BuildConfig.DEBUG && (hasFocus || !NEEDLE)) Log.d(TAG, text);
    }
}
