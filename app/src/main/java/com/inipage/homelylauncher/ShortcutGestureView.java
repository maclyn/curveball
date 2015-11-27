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
import android.graphics.RadialGradient;
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

    //Options representing images
    private static final Integer[] folderOptionsDrawables = new Integer[] {
            R.drawable.ic_mode_edit_white_24dp,
            R.drawable.ic_clear_white_48dp,
            R.drawable.ic_open_in_new_white_24dp };
    private static final Integer[] folderOptionsTitles = new Integer[] {
            R.string.edit_folder,
            R.string.cancel,
            R.string.open_all_in_folder };
    private static final Integer[] folderOptionsColors = new Integer[] {
            R.color.yellow,
            R.color.red,
            R.color.blue };

    private enum SGTypes {
        MODE_ADD_ICON, MODE_SELECT_ICON, MODE_NONE
    }

    private enum ScreenSide {
        LEFT_SIDE, RIGHT_SIDE
    }

    /**
     * When in MODE_SELECT_ICON, what state are we in?
     */
    private enum ChoiceMode {
        CHOOSING_FOLDER, CHOOSING_APP, CHOOSING_OPTION
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
    RectF scratchRect = new RectF(); //Scratch rectangle used for drwaing ops

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
    ChoiceMode cm;

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

    //Map for caching the "color" of these drawables
    Map<String, Integer> colorMap;
    //Map for caching the "label" of apps
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

    private IconCache.ItemRetrievalInterface dummyInterface = new IconCache.ItemRetrievalInterface() {
        @Override
        public void onRetrievalStarted() {
        }

        @Override
        public void onRetrievalComplete(Bitmap result) {
        }
    };

    //Mode we're in
    private SGTypes sgt;

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

    /**
     * ShortcutGestureView is *very* tightly connected to HomeActivity. This isn't ideal, but
     * since the View only makes sense in the context of being hosted by HomeActivity, it's okay
     * for now -- at some point, though, an SgvHostInterface will be used instead.
     */
    public void setActivity(HomeActivity ha){
        this.ha = ha;
    }

    private void init(){
        sgt = SGTypes.MODE_NONE;

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
        glowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        transparencyPaint = new Paint();
        transparencyPaint.setAntiAlias(true);
        transparencyPaint.setAlpha(255);

        hint = getContext().getString(R.string.drop_icon_hint);

        labelMap = new HashMap<>();
        colorMap = new HashMap<>();

        timer = new Timer();
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
                    sgt = SGTypes.MODE_ADD_ICON;
                    invalidate();
                    ha.fadeDateTime(0, 300);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    resetState(1, 300);
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (data.size() != 0) {
                        updateSelectedDragItem(event.getY());
                        invalidate();
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    log("Drag ended", false);
                    if (sgt == SGTypes.MODE_ADD_ICON) {
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
                            cm = ChoiceMode.CHOOSING_APP;

                            //To avoid glitches, we call this again
                            updateSelectedTouchItem();
                            invalidate();
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
                            cm = ChoiceMode.CHOOSING_OPTION;

                            //To avoid glitches, we call this again
                            updateSelectedTouchItem();
                            invalidate();
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
                    optionScope:
                    {
                        //Unlike the other options, you can't back out from this one
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
        switch(sgt) {
            case MODE_ADD_ICON:
                drawAddIcon(canvas);
                break;
            case MODE_SELECT_ICON:
                //Draw touch trail
                drawTouchTrail(canvas);
                switch(cm){
                    case CHOOSING_FOLDER:
                        drawFolderSelection(canvas);
                        break;
                    case CHOOSING_APP:
                        drawAppSelection(canvas);
                        break;
                    case CHOOSING_OPTION:
                        drawFolderOptions(canvas);
                        break;
                }
                break;
            case MODE_NONE:
                drawFolderHints(canvas);
                break;
        }
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
        if(selectedY >= folderOptionsDrawables.length)
            return;

        drawColor(canvas, getResources().getColor(folderOptionsColors[selectedY]), ScreenSide.LEFT_SIDE);

        for (int i = 0; i < folderOptionsDrawables.length ; i++) {
            Pair<Float, Float> sizes = sizeQueue.remove(0);
            drawAbsoluteLineInternalLeftJustified(canvas,
                    IconCache.getInstance().getSwipeCacheIcon(folderOptionsDrawables[i], bigIconSize, retrievalInterface),
                        getResources().getString(folderOptionsTitles[i]), edgeSlop, yPosition, sizes.first,
                        sizes.second, iconPadding, selectedY == i);
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

            scratchRect.set(startX, startY, endX, endY);
            canvas.drawBitmap(icon, null, scratchRect, transparencyPaint);
        }
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

            drawColor(canvas,
                    getIconColor(data.get(selectedY).getDrawablePackage(), data.get(selectedY).getDrawableName()),
                    ScreenSide.LEFT_SIDE);

            for (int i = 0; i < data.size(); i++) {
                Pair<Float, Float> sizes = sizeQueue.remove(0);
                drawAbsoluteLineInternalLeftJustified(canvas,
                        IconCache.getInstance().getSwipeCacheIcon(data.get(i).getDrawablePackage(), data.get(i).getDrawableName(), bigIconSize, retrievalInterface),
                        data.get(i).getTitle(), edgeSlop, yPosition, sizes.first,
                        sizes.second, iconPadding, selectedY == i);
                yPosition += (sizes.first + iconPadding);
            }

            //Draw "folder child" icons in a line
            List<Pair<String, String>> packages = data.get(selectedY).getPackages();
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
                canvas.drawLine(0, startY, getWidth(), startY, thickLine);
                canvas.drawLine(0, endY, getWidth(), endY, thickLine);
                */

                scratchRect.set(startX, startY, endX, endY);
                canvas.drawBitmap(b, null, scratchRect, transparencyPaint);
            }
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

        //Draw selected icon "glow"
        drawColor(canvas, getIconColorForApp(packages.get(selectedY).first,
                packages.get(selectedY).second), ScreenSide.RIGHT_SIDE);

        for(int i = 0; i < numIcons; i++){ //Package name/activity name
            Pair<Float, Float> sizes = sizeQueue.remove(0);

            Pair<String, String> app = packages.get(i);
            ComponentName cm = new ComponentName(app.first, app.second);

            Bitmap b = IconCache.getInstance().getSwipeCacheAppIcon(app.first,
                    app.second, bigIconSize, retrievalInterface);
            String label = grabLabel(cm);

            //Right-justify the icons
            drawAbsoluteLineInternalRightJustified(canvas, b, label, getWidth() - edgeSlop,
                    iconsStartY, sizes.first, sizes.second, iconPadding, selectedY == i);

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

            scratchRect.set(startX, startY, endX, endY);
            canvas.drawBitmap(b, null, scratchRect, transparencyPaint);
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

    private void drawColor(Canvas c, int iconColor, ScreenSide side) {
        int xStart;
        int xEnd;
        int quarter = getWidth()/ 4;
        int eighth = getWidth() / 8;
        int sixteenth = getWidth() / 16;

        if(side == ScreenSide.LEFT_SIDE){
            xStart = -(eighth + sixteenth);
            xEnd = eighth;
        } else {
            xStart = getWidth() - eighth;
            xEnd = getWidth() + (eighth + sixteenth);
        }

        int transparentColor = getResources().getColor(android.R.color.transparent);

        Shader radial = new RadialGradient((xStart + xEnd) / 2, //Center X
                getHeight() / 2, //Center Y
                eighth, //"Radius"
                iconColor, //Center color
                transparentColor, //Edge color
                Shader.TileMode.REPEAT);

        Shader linear = new LinearGradient(
                side == ScreenSide.LEFT_SIDE ? 0 : getWidth(),
                getHeight() / 2,
                side == ScreenSide.LEFT_SIDE ? eighth : getWidth() - eighth,
                getHeight() / 2,
                Color.argb(120, Color.red(iconColor), Color.green(iconColor), Color.blue(iconColor)),
                transparentColor,
                Shader.TileMode.CLAMP);

        glowPaint.setShader(linear);

        RectF rectF = new RectF();
        float twentyDp = Utilities.convertDpToPixel(20, getContext());
        rectF.set(xStart, twentyDp, xEnd, getHeight() - twentyDp);
        c.drawOval(rectF, glowPaint);
    }

    public void invalidateCaches() {
        colorMap.clear();
        labelMap.clear();
    }

    /*
     * Call when the shortcuts represented in this view change.
     */
    public void notifyShortcutsChanged(){
        invalidate();
    }

    private int getIconColor(String packageName, String resourceName){
        String key = packageName + "_res_" + resourceName;
        if(colorMap.containsKey(key)){
            return colorMap.get(key);
        }

        Bitmap toDecode = IconCache.getInstance().getSwipeCacheIcon(packageName, resourceName,
                bigIconSize, retrievalInterface);
        if(toDecode == IconCache.getInstance().dummyBitmap){ //Wait until we have something valid to find the color of
            return Color.WHITE;
        }

        Palette p = Palette.generate(toDecode);
        int choice = grabFromPalette(p);

        colorMap.put(key, choice);
        return choice;
    }

    private int getIconColorForApp(String packageName, String activityName) {
        String key = packageName + "_app_" + activityName;
        if(colorMap.containsKey(key)){
            return colorMap.get(key);
        }

        Bitmap toDecode = IconCache.getInstance().getSwipeCacheAppIcon(packageName, activityName,
                bigIconSize, retrievalInterface);
        if(toDecode == IconCache.getInstance().dummyBitmap){ //Wait until we have something valid to find the color of
            return Color.WHITE;
        }

        Palette p = Palette.generate(toDecode);
        int choice = grabFromPalette(p);

        colorMap.put(key, choice);
        return choice;
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
        c.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                new Rect(iconXStart, iconYStart, iconXEnd, iconYEnd), transparencyPaint);

        //Draw text too
        int textXStart = (int) (iconXEnd + touchSlop);
        //log("OutRect height: " + outRect.height());
        int textYStart = (y + (outRect.height() / 2));
        labelPaint.setAlpha(selected ? 255 : 180);
        labelPaint.setTextSize(textSize);
        c.drawText(text, textXStart, textYStart, labelPaint);

        return iconXStart;
    }

    /* Draw a line. The line isn't centered by x, but the text is centered relative to the icon. */
    private void drawAbsoluteLineInternalLeftJustified(Canvas c, Bitmap b, String text, float xStart, float yStart,
                                         float iconSize, float textSize, float margin, boolean selected){
        float x = xStart;
        float y = yStart;

        if(text.length() > 20) text = text.substring(0, 20) + "...";

        transparencyPaint.setAlpha(255);
        c.drawBitmap(b, new Rect(0, 0, b.getWidth(), b.getHeight()), new Rect((int) x,
                (int) y, (int) (x + iconSize), (int) (y + iconSize)), transparencyPaint);

        x += iconSize;
        x += margin;

        labelPaint.setAlpha(selected ? 255 : 180);
        labelPaint.setTextSize(textSize);

        float centerYLine = y + (iconSize / 2); //Now we're at the center of the drawable
        labelPaint.getTextBounds(text, 0, text.length(), outRect);
        centerYLine -= (outRect.height() / 2); //Center it
        centerYLine += outRect.height();

        c.drawText(text, x, centerYLine, labelPaint);
    }

    /* Draw a line. The line isn't centered by x (aligned right, actually, but the text is centered relative to the icon. */
    private void drawAbsoluteLineInternalRightJustified(Canvas c, Bitmap b,  String text, float xStart, float yStart,
                                          float iconSize, float textSize, float margin, boolean selected){
        log("Xstart/Ystart: " + xStart + " " + yStart, false);
        float x = xStart;
        float y = yStart;

        if(text.length() > 20) text = text.substring(0, 20) + "...";

        transparencyPaint.setAlpha(255);
        c.drawBitmap(b, new Rect(0, 0, b.getWidth(), b.getHeight()), new Rect((int) (x - iconSize),
                (int) y, (int) (x), (int) (y + iconSize)), transparencyPaint);

        x -= iconSize;
        x -= margin;

        log("New X: " + x, false);

        labelPaint.setAlpha(selected ? 255 : 180);
        labelPaint.setTextSize(textSize);

        float centerYLine = y + (iconSize / 2); //Now we're at the center of the drawable
        log("Centered, line is at: " + centerYLine, false);
        labelPaint.getTextBounds(text, 0, text.length(), outRect);
        centerYLine -= (outRect.height() / 2); //Center it
        log("Centered relative to text height, line is at: " + centerYLine, false);
        x -= (outRect.width()); //Draw text more to left of the icon
        centerYLine += outRect.height();
        log("Final x is at: " + x, false);
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
                log("Handling for " + sgt.name(), false);
                switch(sgt){
                    case MODE_NONE:
                        if(sqrt(pow(dy, 2) + pow(dx, 2)) > touchSlop) {
                            log("Found movement; swiping not tapping", false);
                            if (startX > touchSlop && startX < getWidth() - edgeSlop
                                    && startY > edgeSlop && startY < getHeight() - edgeSlop) {
                                log("Selected icon choosing mode!", false);
                                sgt = SGTypes.MODE_SELECT_ICON;
                                gestureStartX = touchX;
                                gestureStartY = touchY;
                                cm = ChoiceMode.CHOOSING_FOLDER;
                            }
                        }
                        break;
                    case MODE_SELECT_ICON:
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
                switch(sgt){
                    case MODE_NONE:
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
                            ha.setDockbarState(HomeActivity.DockbarState.STATE_APPS, true);
                            resetState(1, -1);
                        }
                        break;
                    case MODE_SELECT_ICON:
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
        sgt = SGTypes.MODE_NONE;
        cm = ChoiceMode.CHOOSING_FOLDER;

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
