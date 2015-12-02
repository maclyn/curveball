package com.inipage.homelylauncher;

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

import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.utils.AttributeApplier;
import com.inipage.homelylauncher.utils.SizeAttribute;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.*;

public class ShortcutGestureView extends View {
    private static final String TAG = "ShortcutGestureView";
    private static final boolean NEEDLE = false; //For debug purposes

    //Size in DiPs of needed things
    //Size of drawables onscreen
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
    @SizeAttribute(attrType = SizeAttribute.AttributeType.SP, value = 16)
    float helpTextSize;
    @SizeAttribute(16)
    float helpPadding;
    @SizeAttribute(4)
    float arrowPadding;
    @SizeAttribute(18)
    float helpArrowSize;

    //Options representing images
    private static final Integer[] folderOptionsDrawables = new Integer[] {
            R.drawable.ic_mode_edit_white_24dp,
            R.drawable.ic_clear_white_48dp,
            R.drawable.ic_open_in_new_white_24dp };
    private static final Integer[] folderOptionsTitles = new Integer[] {
            R.string.edit_folder,
            R.string.cancel,
            R.string.open_all_in_folder };

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
        CHOOSING_OPTION
    }

    private enum ScreenSide {
        LEFT_SIDE, RIGHT_SIDE
    }

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

    HomeActivity ha;

    //Data set used
    List<TypeCard> data;

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
        public void onRetrievalStarted() {
            //Nothing happens.
        }

        @Override
        public void onRetrievalComplete(Bitmap result) {
            invalidate();
        }
    };

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
     * ShortcutGestureView is *very* tightly connected to HomeActivity. This isn't ideal, but
     * since the View only makes sense in the context of being hosted by HomeActivity, it's okay
     * for now -- at some point, though, an SgvHostInterface will be used instead.
     */
    public void setActivity(HomeActivity ha){
        this.ha = ha;
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
                    log("Drag started " + startX + " " + startY, false);
                    cm = ViewMode.ADDING_ICON;
                    invalidate();
                    ha.fadeDateTime(0, 300);
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (data.size() != 0) {
                        updateSelectedDragItem(event.getY());
                        invalidate();
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    log("Drag ended", false);
                    if (cm == ViewMode.ADDING_ICON) {
                        ApplicationIcon ai = (ApplicationIcon) event.getLocalState();

                        if (selectedY == data.size()) { //Add a new row
                            ha.showCreateFolderDialog(ai);
                        } else {
                            log("Adding to an old row...", false);
                            data.get(selectedY).getPackages().add(new Pair<>
                                    (ai.getPackageName(), ai.getActivityName()));
                            ha.persistList(ha.samples);
                            preloadCard(data.get(selectedY));
                        }
                    }
                    resetState(1, 300);
                    break;
            }
            return true;
        } else {
            log("Ignoring drag event; not dragging an application icon", false);
            return false;
        }
    }

    private void initTouchOp(MotionEvent event){
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

        float percent = location / (float) (getHeight() - ha.dockBar.getHeight());
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
                        log("Selected Y: " + selectedY, false);

                        if(selectedY != oldSelectedY || oldSelectedY == -1){
                            timer.cancel();
                            timer = new Timer();

                            final String appPackage = data.get(selectedFolder).getPackages().get(selectedY).first;

                            //We just switched selectedY's; check if there's a widget
                            if(ha.hasWidget(appPackage)){
                                //Start timer for 1 second
                                timer.schedule(new TimerTask() { //Open widget
                                    @Override
                                    public void run() {
                                        ha.showWidget(appPackage);
                                        timerCompleted = true;
                                    }
                                }, 1000l);
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
                                }, 0l, 1000 / 60);
                                timerStart = System.currentTimeMillis();
                            } else {
                                timerStart = -1l;
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

        if(timerCompleted){
            resetState(1, 300);
            return;
        }

        switch(cm){
            case CHOOSING_FOLDER:
                resetState(1, 300); //Nothing to do here...
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
                        resetState(1, 1500); //Slow, because launching an app
                    }
                }
                break;
            case CHOOSING_OPTION:
                choosingOptionScope: {
                    switch(selectedY) {
                        case 0:
                            ha.showEditFolderDialog(selectedFolder);
                            break;
                        case 1:
                            break;
                        case 2:
                            ha.batchOpen(selectedFolder);
                            break;
                    }
                    resetState(1, 300);
                }
        }
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        switch(cm) {
            case ADDING_ICON:
                drawAddIcon(canvas);
                break;
            case CHOOSING_FOLDER:
                drawFolderSelection(canvas);
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
            case NONE:
                drawFolderHints(canvas);
                break;
        }
    }

    private void drawHelp(Canvas c, String helpText, String subText, boolean showLeftArrow,
                          boolean showRightArrow){
        //Draw the main text
        labelPaint.setAlpha(180);
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
            labelPaint.setAlpha(180);
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

        Bitmap left = IconCache.getInstance().getSwipeCacheIcon(R.drawable.ic_keyboard_arrow_left_white_18dp,
                helpArrowSize, retrievalInterface);
        scratchRect2.set(0, 0, left.getWidth(), left.getHeight());
        scratchRect.set( (int) (x - arrowPadding - helpArrowSize), (int) (centerYLine - (outRect.height() / 4) - (helpArrowSize / 2)),
                (int) (x - arrowPadding), (int) (centerYLine - (outRect.height() / 4) + (helpArrowSize / 2)));
        transparencyPaint.setAlpha(showLeftArrow ? 180 : 120);
        c.drawBitmap(left, scratchRect2, scratchRect, transparencyPaint);

        Bitmap right = IconCache.getInstance().getSwipeCacheIcon(R.drawable.ic_keyboard_arrow_right_white_18dp,
                helpArrowSize, retrievalInterface);
        scratchRect2.set(0, 0, right.getWidth(), right.getHeight());
        scratchRect.set( (int) (x + outRect.width() + arrowPadding), (int) (centerYLine - (outRect.height() / 4) - (helpArrowSize / 2)),
                (int) (x + outRect.width() + arrowPadding + helpArrowSize),
                (int) (centerYLine - (outRect.height() / 4) + (helpArrowSize / 2)));
        transparencyPaint.setAlpha(showRightArrow ? 180 : 120);
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
                        minFor + " " + maxFor + " " + selectedY, true);
                if(i == selectedY){
                    log("Calculating from selectedY", true);
                    float drawPercent = 1f - ((maxFor - touchY) / perElementSize); //By calculation, this will be between 0 and 1
                    log("Draw percent: " + drawPercent, true);
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
                        log("SelectedY - 1 VALID at position " + i + " difference " + difference, true);
                        sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                textSize + (difference * textSizeDifference)));
                    } else { //Not so much
                        sizeQueue.add(new Pair<>(iconSize, textSize));
                    }
                } else if (i == (selectedY + 1)){
                    //log("Calculating from selectedY + 1", true);
                    if(touchY >= idealPosition){ //We're involved
                        float difference = 0.5f - ((maxFor - touchY) / perElementSize);
                        log("SelectedY + 1 VALID at position " + i + " difference " + difference, true);
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
        if(selectedY >= folderOptionsDrawables.length || selectedY < 0)
            return;

        for (int i = 0; i < folderOptionsDrawables.length ; i++) {
            Pair<Float, Float> sizes = sizeQueue.remove(0);
            drawLineToSide(canvas,
                    IconCache.getInstance().getSwipeCacheIcon(folderOptionsDrawables[i], bigIconSize, retrievalInterface),
                    getResources().getString(folderOptionsTitles[i]), selectedY == i, ScreenSide.LEFT_SIDE, edgeSlop, yPosition, sizes.first,
                    sizes.second, iconPadding);
            yPosition += (sizes.first + iconPadding);
        }
    }

    private void drawFolderHints(Canvas canvas) {
        //Draw icons in a line
        float top = ha.timeDateContainer.getY() + ha.timeDateContainer.getHeight();
        float bottom = ha.dockbarApps.getY();
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

            TypeCard card = data.get(i);
            Bitmap icon = IconCache.getInstance().getSwipeCacheIcon(card.getDrawablePackage(),
                    card.getDrawableName(), bigIconSize, retrievalInterface);

            scratchRectF.set(startX, startY, endX, endY);
            canvas.drawBitmap(icon, null, scratchRectF, transparencyPaint);
        }
    }

    private void drawSuggestion(){

    }

    private void drawAddIcon(Canvas canvas) {
        int divisions = data.size() + 1;
        float roomForEach = (getHeight() - ha.dockBar.getHeight()) / divisions;

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
                            minFor + " " + maxFor + " " + selectedY, true);
                    if(i == selectedY){
                        log("Calculating from selectedY", true);
                        float drawPercent = 1f - ((maxFor - touchY) / perElementSize); //By calculation, this will be between 0 and 1
                        log("Draw percent: " + drawPercent, true);
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
                            log("SelectedY - 1 VALID at position " + i + " difference " + difference, true);
                            sizeQueue.add(new Pair<>(iconSize + (difference * iconSizeDifference),
                                    textSize + (difference * textSizeDifference)));
                        } else { //Not so much
                            sizeQueue.add(new Pair<>(iconSize, textSize));
                        }
                    } else if (i == (selectedY + 1)){
                        //log("Calculating from selectedY + 1", true);
                        if(touchY >= idealPosition){ //We're involved
                            float difference = 0.5f - ((maxFor - touchY) / perElementSize);
                            log("SelectedY + 1 VALID at position " + i + " difference " + difference, true);
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
                        IconCache.getInstance().getSwipeCacheIcon(data.get(i).getDrawablePackage(), data.get(i).getDrawableName(), bigIconSize, retrievalInterface),
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
            Bitmap b = IconCache.getInstance().getSwipeCacheAppIcon(app.first,
                    app.second, bigIconSize, retrievalInterface);

            log("For package " + (j + 1) + " of " + packages.size(), true);

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
                        minFor + " " + maxFor + " " + selectedY, true);
                if(i == selectedY){
                    log("Calculating from selectedY", true);
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
                        log("SelectedY - 1 VALID at position " + i + " difference " + difference, true);
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

            Bitmap b = IconCache.getInstance().getSwipeCacheAppIcon(app.first,
                    app.second, bigIconSize, retrievalInterface);
            String label = grabLabel(cm);

            //Right-justify the icons
            drawLineToSide(canvas, b, label, selectedY == i, ScreenSide.RIGHT_SIDE, getWidth() - edgeSlop,
                    iconsStartY, sizes.first, sizes.second, iconPadding);

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

            Bitmap b = IconCache.getInstance().getSwipeCacheIcon(folderOptionsDrawables[j], previewIconSize, retrievalInterface);

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

        Palette.from(b).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                bitmapColorMap.put(hashCode, grabFromPalette(palette));
                invalidate(); //Stop just drawing white.
            }
        });

        bitmapColorMap.put(b.hashCode(), Color.WHITE);
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
        return drawCenteredLine(c, IconCache.getInstance().getSwipeCacheIcon(resId,
                bigIconSize, retrievalInterface), text, x, y, selected);
    }

    private int drawCenteredLine(Canvas c, String packageName, String resource, String text, int x, int y, boolean selected){
        return drawCenteredLine(c, IconCache.getInstance().getSwipeCacheIcon(packageName,
                resource, bigIconSize, retrievalInterface), text, x, y, selected);
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
     *
     * @param c Canvas to draw on.
     * @param b The Bitmap to draw. Must not be null.
     * @param text The text to draw with the bitmap.
     * @param selected Whether the element is selected (affects transparency).
     * @param side The side (left or right). to draw the line on.
     * @param xStart The start X position of the whole thing (drawing to left pushes things right,
     *               and drawing to right pushes things left).
     * @param yStart The start Y position of the whole line.
     * @param iconSize The size of the icon in pixels.
     * @param textSize The size of the text in pixels.
     * @param margin The margin between different lines.
     * */
    private void drawLineToSide(Canvas c,
                                Bitmap b, String text, boolean selected,
                                ScreenSide side, float xStart, float yStart, float iconSize, float textSize, float margin){
        float x = xStart;
        float y = yStart;

        //Add color to the icon row if selected
        if(selected){
            int transparentColor = getResources().getColor(android.R.color.transparent);
            int color = getIconColorForBitmap(b);
            int paddingOverTwo = (int) (iconPadding / 2);

            int shadingXStart = side == ScreenSide.LEFT_SIDE ? 0 : getWidth() / 2;
            int shadingXEnd = side == ScreenSide.LEFT_SIDE ? getWidth() / 2 : getWidth();

            Shader linear = new LinearGradient(shadingXStart, y - paddingOverTwo, shadingXEnd,
                    y + iconSize + paddingOverTwo, side == ScreenSide.LEFT_SIDE ? color : transparentColor,
                    side == ScreenSide.LEFT_SIDE ? transparentColor : color, Shader.TileMode.CLAMP);
            glowPaint.setShader(linear);
            c.drawRect(shadingXStart, y - paddingOverTwo, shadingXEnd, y + iconSize + paddingOverTwo,
                    glowPaint);
        }

        //Draw the icon
        int bitmapXStart = (int) (side == ScreenSide.LEFT_SIDE ? x : x - iconSize);
        int bitmapXEnd = (int) (side == ScreenSide.LEFT_SIDE ? x + iconSize : x);
        scratchRect.set(bitmapXStart, (int) y, bitmapXEnd, (int) (y + iconSize));
        scratchRect2.set(0, 0, b.getWidth(), b.getHeight());

        transparencyPaint.setAlpha(255);
        c.drawBitmap(b, scratchRect2, scratchRect, transparencyPaint);

        //Draw the text
        labelPaint.setAlpha(selected ? 255 : 180);
        labelPaint.setTextSize(textSize);
        labelPaint.getTextBounds(text, 0, text.length(), outRect);

        //Center text horizontally
        if(side == ScreenSide.LEFT_SIDE) {
            x += iconSize;
            x += margin;
        } else {
            x -= iconSize;
            x -= margin;
            x -= outRect.width(); //Draw text more to left of the icon
        }

        //Center text vertically
        float centerYLine = y + (iconSize / 2); //Now we're at the center of the drawable
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
                log("Motion event action down", false);
                initTouchOp(event);
                ha.fadeDateTime(0, 300);
                cleanTouchEvents();
                break;
            case MotionEvent.ACTION_MOVE:
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

                float dx = startX - event.getX();
                float dy = startY - event.getY();

                switch(cm){
                    case NONE:
                        gestureStartX = touchX;
                        gestureStartY = touchY;
                        cm = ViewMode.CHOOSING_FOLDER;
                        break;
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

                float totalXMovement = startX - event.getX();
                float totalXPercent = totalXMovement / 100;
                if(totalXPercent < -1) totalXPercent = -1;
                if(totalXPercent > 1) totalXPercent = 1;
                long animationXTime = (long) (300 * abs(totalXPercent));
                switch(cm){
                    case NONE:
                        log("Mode none; checking for taps", false);
                        if (Utilities.withinView(event, ha.timeLayout)) {
                            log("Within time", false);
                            ha.timeLayout.performClick();
                            resetState(1, 300);
                        } else if (Utilities.withinView(event, ha.date)) {
                            log("Within date", false);
                            ha.date.performClick();
                            resetState(1, 300);
                        } else { //Open app drawer
                            log("Falling back to app drawer", false);
                            ha.toggleAppsContainer(true);
                            resetState(1, -1);
                        }
                        break;
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
                //Shouldn't do anything
                resetState(1, 1);
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

    public void resetState(int animateTo, int animateTime){
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

        if(animateTime > 0l) ha.fadeDateTime(animateTo, animateTime);

        log("resetState() finished", true);
    }

    private void preloadCard(TypeCard card){
        IconCache.getInstance().getSwipeCacheIcon(card.getDrawablePackage(), card.getDrawableName(),
                bigIconSize, retrievalInterface);
        for(Pair<String, String> icon : card.getPackages()){
            IconCache.getInstance().getSwipeCacheAppIcon(icon.first, icon.second,
                    bigIconSize, retrievalInterface);
        }
    }

    private void preCache(){
        if(data == null) return;

        //Grab all the icons for folder icons + app icons
        for(TypeCard card : data){
            preloadCard(card);
        }

        //Grab the icons we get from internal sources
        IconCache.getInstance().getSwipeCacheIcon(R.drawable.ic_add_circle_outline_white_48dp,
                bigIconSize, retrievalInterface);
        IconCache.getInstance().getSwipeCacheIcon(R.drawable.ic_info_white_48dp,
                bigIconSize, retrievalInterface);
        IconCache.getInstance().getSwipeCacheIcon(R.drawable.ic_clear_white_48dp,
                bigIconSize, retrievalInterface);
    }

    public void setCards(List<TypeCard> cards){
        this.data = cards;
        preCache();
    }

    private void log(String text, boolean hasFocus){
        //noinspection PointlessBooleanExpression
        if(BuildConfig.DEBUG && (hasFocus || !NEEDLE)) Log.d(TAG, text);
    }
}
