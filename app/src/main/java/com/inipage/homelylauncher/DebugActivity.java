package com.inipage.homelylauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.hardware.display.DisplayManagerCompat;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.inipage.homelylauncher.drawer.ApplicationHideAdapter;
import com.inipage.homelylauncher.drawer.ApplicationHiderIcon;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.model.ContextualElement;
import com.inipage.homelylauncher.model.Favorite;
import com.inipage.homelylauncher.scroller.FavoriteGridSplayer;
import com.inipage.homelylauncher.utils.Utilities;
import com.inipage.homelylauncher.views.ContextualView;
import com.inipage.homelylauncher.views.PointerInfoRelativeLayout;
import com.inipage.homelylauncher.weather.WeatherController;
import com.inipage.homelylauncher.weather.model.CleanedUpWeatherModel;
import com.inipage.homelylauncher.weather.model.LTSForecastModel;

import org.apache.commons.collections4.trie.PatriciaTrie;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

/**
 * A staging activity for changes that will eventually be used in the HomeActivity. Oh boy.
 */
public class DebugActivity extends Activity implements WeatherController.WeatherPresenter, FavoriteGridSplayer.FavoriteStateCallback, ViewTreeObserver.OnScrollChangedListener, ContextualView.ContextualViewListener {
    public static final String TAG = "DebugActivity";

    private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm", Locale.getDefault());
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());

    @Bind(R.id.pointer_rl)
    PointerInfoRelativeLayout pointerRl;

    @Bind(R.id.context_view)
    ContextualView contextView;

    @Bind(R.id.background)
    View background;

    @Bind(R.id.scrollContainer)
    ScrollView scrollView;

    @Bind(R.id.top_scrim)
    View topScrim;

    @Bind(R.id.headerView)
    View headerView;

    @Bind(R.id.bottom_scrim)
    View bottomScrim;

    @Bind(R.id.space)
    View space;

    @Bind(R.id.app_list)
    LinearLayout appList;

    @Bind(R.id.wallpaper_button)
    View wallpaperButton;

    @Bind(R.id.settings_button)
    View settingsButton;

    @Bind(R.id.unhide)
    View unhideButton;

    @Bind(R.id.fold_rows)
    View foldRowsButton;

    @Bind(R.id.column_count)
    View columnCountButton;

    @Bind(R.id.fold_num)
    TextView foldRowsNumTextView;

    @Bind(R.id.column_num)
    TextView columnCountNumTextView;

    @Bind(R.id.grid_layout)
    AbsoluteLayout favoritesGrid;

    @Bind(R.id.overlay_layout)
    AbsoluteLayout overlayLayout;
    View attachedOverlay;

    //Header fields
    Timer timeUpdateTimer;
    Timer animationUpdateTimer;

    @Bind(R.id.time)
    TextView timeTv;
    @Bind(R.id.date)
    TextView dateTv;
    @Bind(R.id.temp)
    TextView tempTv;
    @Bind(R.id.highLow)
    TextView highLowTv;
    @Bind(R.id.weatherIcon)
    ImageView conditionIv;

    List<ComponentName> hiddenApps = new ArrayList<>();
    List<ApplicationIcon> cachedApps = new ArrayList<>();
    PatriciaTrie<ApplicationIcon> appsTree = new PatriciaTrie<>();
    List<Favorite> favorites;
    FavoriteGridSplayer favManager;
    BroadcastReceiver packageReceiver;
    BroadcastReceiver storageReceiver;
    SharedPreferences reader;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        ButterKnife.bind(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        background.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        reader = PreferenceManager.getDefaultSharedPreferences(this);
        editor = reader.edit();

        loadApps();
        loadFavorites();
        timeUpdateTimer = new Timer();
        TimerTask clockUpdateTask = new TimerTask(){
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Calendar cal = GregorianCalendar.getInstance();
                        //Set time
                        timeTv.setText(TIME_FORMAT.format(cal.getTime()));
                        //Set date
                        dateTv.setText(DATE_FORMAT.format(cal.getTime()));
                    }
                });
            }
        };
        timeUpdateTimer.schedule(clockUpdateTask, new Date(), 1000L);
        animationUpdateTimer = new Timer();
        TimerTask animationUpdateTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        favManager.animate();
                    }
                });
            }
        };
        animationUpdateTimer.schedule(animationUpdateTask, new Date(), 1000L / 60L);
        updateScrims();

        if(scrollView.getViewTreeObserver().isAlive()) {
            scrollView.getViewTreeObserver().addOnScrollChangedListener(this);
        }

        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Utilities.logEvent(Utilities.LogLevel.SYS_BG_TASK, "Package removed and/or changed");

                loadApps();
                IconCache.getInstance().invalidateCaches();

                Utilities.logEvent(Utilities.LogLevel.SYS_BG_TASK, "Accordingly, invalidating caches...");
            }
        };

        storageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Utilities.logEvent(Utilities.LogLevel.SYS_BG_TASK, "A storage medium has been chanegd");

                loadApps();
                IconCache.getInstance().invalidateCaches();

                Utilities.logEvent(Utilities.LogLevel.SYS_BG_TASK, "Accordingly, invalidating caches...");
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);

        IntentFilter storageFilter = new IntentFilter();
        storageFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        storageFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        storageFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(storageReceiver, storageFilter);

        scrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);

        //Add settings buttons
        wallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(pickWallpaper, getString(R.string.set_wallpaper)));
            }
        });
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsActivity = new Intent(DebugActivity.this, SettingsActivity.class);
                settingsActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsActivity);
            }
        });
        unhideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHiddenAppsMenu();
            }
        });
        foldRowsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Set above rows
                if(rowsAboveFold > 5)
                    rowsAboveFold = 0;
                else
                    rowsAboveFold++;
                editor.putInt(Constants.ROWS_ABOVE_FOLD_PREFERENCE, rowsAboveFold).commit();
                updateSpacing();
            }
        });
        columnCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(columnCount > 6)
                    columnCount = 3;
                else
                    columnCount++;
                editor.putInt(Constants.COLUMN_COUNT_PREFERENCE, columnCount).commit();
                loadFavorites();
                updateSpacing();
            }
        });
        rowsAboveFold = reader.getInt(Constants.ROWS_ABOVE_FOLD_PREFERENCE, 2);
        pointerRl.attachScrollView(scrollView, contextView);
    }

    int topInset = -1;
    int bottomInset = -1;
    private void updateScrims(){
        if(topInset != -1){
            updateSpacing();
            return;
        }

        //It appears setting this twice does not work
        ViewCompat.setOnApplyWindowInsetsListener(topScrim,
                new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                        topInset = insets.getSystemWindowInsetTop();
                        bottomInset = insets.getSystemWindowInsetBottom();

                        updateSpacing();

                        return insets.consumeSystemWindowInsets();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (packageReceiver != null)
            unregisterReceiver(packageReceiver);
        if (storageReceiver != null)
            unregisterReceiver(storageReceiver);
    }

    int rowsAboveFold = -1;
    int columnCount = -1;
    private void updateSpacing(){
        LinearLayout.LayoutParams topParams = (LinearLayout.LayoutParams) topScrim.getLayoutParams();
        topParams.height = topInset;
        topScrim.setLayoutParams(topParams);

        LinearLayout.LayoutParams bottomParams = (LinearLayout.LayoutParams) bottomScrim.getLayoutParams();
        bottomParams.height = bottomInset;
        bottomScrim.setLayoutParams(bottomParams);

        //Set "space" to occupy...space [screen height - topInset scrim - bottomInset scrim - header size - [cell*space]]
        DisplayManagerCompat dmc = DisplayManagerCompat.getInstance(DebugActivity.this);
        int screenHeight = -1;
        int screenWidth = -1;
        for(Display d : dmc.getDisplays()){
            DisplayMetrics metrics = new DisplayMetrics();
            d.getRealMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        }
        int headerSize = headerView.getHeight();
        float cellDimension = (screenWidth / columnCount);

        LinearLayout.LayoutParams spaceParams = (LinearLayout.LayoutParams) space.getLayoutParams();
        //[screen height - topInset scrim - bottomInset scrim - header size - [cell*space]]

        int desiredSpace = (int) (screenHeight - topInset - bottomInset - headerSize - (cellDimension * rowsAboveFold));
        if(desiredSpace < 0)
            desiredSpace = 0;
        spaceParams.height = desiredSpace;
        space.setLayoutParams(spaceParams);

        foldRowsNumTextView.setText(getString(R.string.fold_row_num, rowsAboveFold));
    }

    @Override
    protected void onResume() {
        super.onResume();
        WeatherController.requestWeather(this, this, false);
        loadContextualData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_MAIN)) {
            scrollView.smoothScrollTo(0, 0);
        }
    }

    @OnClick(R.id.weatherIcon)
    public void onWeatherClick(){
        //TODO
    }

    @OnLongClick(R.id.weatherIcon)
    public boolean onWeatherLongClick(){
        WeatherController.requestWeather(this, this, true);
        return true;
    }

    /**
     * Reload the app list. Unoptimized way of checking package changes and the like.
     */
    private void loadApps(){
        cachedApps.clear();
        appsTree.clear();

        new AsyncTask<Object, Void, Pair<List<ApplicationIcon>, List<ComponentName>>>() {
            @Override
            protected Pair<List<ApplicationIcon>, List<ComponentName>> doInBackground(Object... params) {
                PackageManager pm = getPackageManager();
                List<ApplicationIcon> applicationIcons = new ArrayList<>();
                List<ComponentName> hideApps = DatabaseEditor.getInstance().getHiddenApps();

                //Grab all matching applications
                final Intent allAppsIntent = new Intent(Intent.ACTION_MAIN, null);
                allAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                final List<ResolveInfo> packageList = pm.queryIntentActivities(allAppsIntent, 0);
                for (ResolveInfo ri : packageList) {
                    try {
                        String name = (String) ri.loadLabel(pm);
                        if (!hideApps.contains(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)))
                            applicationIcons.add(new ApplicationIcon(ri.activityInfo.packageName,
                                    name, ri.activityInfo.name));
                    } catch (Exception ignored) {}
                }
                Collections.sort(applicationIcons, new Comparator<ApplicationIcon>() {
                    @Override
                    public int compare(ApplicationIcon lhs, ApplicationIcon rhs) {
                        return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }
                });
                for(ApplicationIcon icon : applicationIcons){
                    appsTree.put(icon.getName().toLowerCase(Locale.getDefault()), icon);
                }
                return new Pair<>(applicationIcons, hideApps);
            }

            @Override
            protected void onPostExecute(Pair<List<ApplicationIcon>, List<ComponentName>> apps) {
                cachedApps = apps.first;
                hiddenApps = apps.second;

                populateAppList(null);
            }
        }.execute();
    }

    private void loadContextualData(){
        //TODO: Find things we might care about quickly getting to
        //These would be:
        //  Events within 2 hours -> Calendar app
        //  Alarms (!) -> Alarm app
        //  Low battery/charging battery -> Settings
        //  Ongoing call -> Phone app
        //  App/web search -> Internal modal
        //  Search app (always there) -> Google/smtg
    }

    private void loadFavorites(){
        favorites = DatabaseEditor.getInstance().getFavorites();
        columnCount = reader.getInt(Constants.COLUMN_COUNT_PREFERENCE, 5);
        columnCountNumTextView.setText(getString(R.string.column_count_desc, columnCount));
        favManager = new FavoriteGridSplayer(favoritesGrid, favorites, this, columnCount);
    }

    private void populateAppList(String query){
        appList.removeAllViews(); //I'm a bad person. I should feel bad.
        float iconSize = Utilities.convertDpToPixel(48, this);

        List<ApplicationIcon> result;
        if(query != null) {
            Set<Map.Entry<String, ApplicationIcon>> set = appsTree.prefixMap(query.toLowerCase(Locale.getDefault())).entrySet();
            result = new ArrayList<>(set.size());
            for(Map.Entry<String, ApplicationIcon> entry : set){
                result.add(entry.getValue());
            }
        } else {
            result = cachedApps;
        }

        for(final ApplicationIcon ai : result){
            //Future potential employers: I know. This is bad.
            final View layout = LayoutInflater.from(this).inflate(R.layout.item_scr_app, appList, false);
            ((TextView) layout.findViewById(R.id.appIconTitle)).setText(ai.getName());
            final ImageView iconView  = (ImageView) layout.findViewById(R.id.appIconImage);
            iconView.setImageBitmap(IconCache.getInstance().getAppIcon(
                    ai.getPackageName(),
                    ai.getActivityName(),
                    IconCache.IconFetchPriority.APP_DRAWER_ICONS,
                    (int) iconSize,
                    new IconCache.ItemRetrievalInterface() {
                        @Override
                        public void onRetrievalComplete(Bitmap result) {
                        iconView.setImageBitmap(result);
                        }
                    }));
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startApp(ai, DebugActivity.this);
                }
            });
            layout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    MenuBuilder mb = new MenuBuilder(v.getContext());
                    new SupportMenuInflater(v.getContext()).inflate(R.menu.app_icon_menu, mb);
                    MenuPopupHelper mph = new MenuPopupHelper(v.getContext(), mb, v);
                    mph.setForceShowIcon(true);
                    mph.setGravity(getPointerPosition().x < scrollView.getWidth() / 2 ? Gravity.LEFT : Gravity.RIGHT);
                    mb.setCallback(new MenuBuilder.Callback() {
                        @Override
                        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.favorite:
                                    favManager.addApp(ai.getPackageName(), ai.getActivityName());
                                    return true;
                                case R.id.uninstall:
                                    try {
                                        Uri uri = Uri.parse("package:" + ai.getPackageName());
                                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                                        uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                                        DebugActivity.this.startActivity(uninstallIntent);
                                    } catch (Exception e) {
                                        Toast.makeText(DebugActivity.this, R.string.cannot_uninstall, Toast.LENGTH_LONG).show();
                                    }
                                    return true;
                                case R.id.app_info:
                                    try {
                                        Uri uri = Uri.parse("package:" + ai.getPackageName());
                                        Intent uninstallIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                                        uninstallIntent.setData(uri);
                                        DebugActivity.this.startActivity(uninstallIntent);
                                    } catch (Exception e) {
                                        Toast.makeText(DebugActivity.this, R.string.app_no_info, Toast.LENGTH_LONG).show();
                                    }
                                    return true;
                                case R.id.hide:
                                    DatabaseEditor.getInstance().markAppHidden(ai.getActivityName(), ai.getPackageName());
                                    appList.removeView(layout);
                                    Toast.makeText(DebugActivity.this, R.string.app_hidden, Toast.LENGTH_SHORT).show();
                                    return true;
                                default:
                                    return false;
                            }
                        }

                        @Override
                        public void onMenuModeChange(MenuBuilder menu) {
                        }
                    });
                    mph.show();
                    return true;
                }
            });
            appList.addView(layout);
        }
    }

    private void startApp(ApplicationIcon ai, Context context){
        startAppImpl(context, new ComponentName(ai.getPackageName(), ai.getActivityName()));
    }

    public void requestLaunch(ComponentName cn){
        startAppImpl(this, cn);
    }

    @Override
    public void attachOverlay(final View toAttach, int centerX, int centerY, int width, int height) {
        int x = centerX - (width / 2);
        int y = centerY - (height / 2);

        int pivotX =  width / 2;
        int pivotY = height / 2;

        if(x < 0){
            pivotX += x; //move it left
            x = 0;
        }
        if(x > (scrollView.getWidth() - width)){
            pivotX += (x - (scrollView.getWidth() - width)); //move it right
            x = scrollView.getWidth() - width;
        }
        if(y < topInset){
            pivotY += (y - topInset);
            y = topInset;
        }

        //pivot relateive to center; to grow from center pivotX = width / 2; pivotY = height /2

        AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(width, height, x, y);
        overlayLayout.addView(toAttach, params);

        toAttach.setPivotX(pivotX);
        toAttach.setPivotY(pivotY);

        Utilities.animateScaleChange(toAttach, new Utilities.ScaleAnimation() {
            @Override
            public void onComplete() {
            }
        }, 500L, 0.0f, 1.0f);

        overlayLayout.setClickable(true);
        overlayLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick!");
                closeOverlay();
            }
        });
        attachedOverlay = toAttach;
    }

    @Override
    public void closeOverlay() {
        overlayLayout.setClickable(false);
        Utilities.animateScaleChange(attachedOverlay, new Utilities.ScaleAnimation() {
            @Override
            public void onComplete() {
                overlayLayout.removeView(attachedOverlay);
                overlayLayout.setOnClickListener(null);
                overlayLayout.setClickable(false);
            }
        }, 500L, 1.0f, 0.0f);
    }

    @Override
    public Activity getActivityContext() {
        return this;
    }

    @Override
    public PointF getPointerPosition() {
        return pointerRl.getPointLocation();
    }

    private void startAppImpl(Context context, ComponentName cn){
        try {
            Intent appLaunch = new Intent();
            appLaunch.setClassName(cn.getPackageName(), cn.getClassName());
            appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appLaunch);
        } catch (Exception e) {
            Toast.makeText(context, "Couldn't start this app!", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void showHiddenAppsMenu() {
        new AsyncTask<Void, Void, List<ApplicationHiderIcon>>() {
            @Override
            protected List<ApplicationHiderIcon> doInBackground(Void... params) {
                List<ApplicationHiderIcon> applicationIcons = new ArrayList<>();


                //Grab all hidden applications
                try {
                    final Intent allAppsIntent = new Intent(Intent.ACTION_MAIN, null);
                    allAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    final List<ResolveInfo> packageList = getPackageManager().queryIntentActivities(allAppsIntent, 0);
                    for (ResolveInfo ri : packageList) {
                        try {
                            String name = (String) ri.loadLabel(getPackageManager());
                            if (hiddenApps.contains(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name))) {
                                applicationIcons.add(new ApplicationHiderIcon(ri.activityInfo.packageName,
                                        name, ri.activityInfo.name, true));
                            }
                        } catch (Exception e) {
                            //Failed to add one.
                        }
                    }

                    Collections.sort(applicationIcons, new Comparator<ApplicationIcon>() {
                        @Override
                        public int compare(ApplicationIcon lhs, ApplicationIcon rhs) {
                            return lhs.getName().compareToIgnoreCase(rhs.getName());
                        }
                    });
                    return applicationIcons;
                } catch (RuntimeException packageManagerDiedException) {
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<ApplicationHiderIcon> apps) {
                final ApplicationHideAdapter adapter = new ApplicationHideAdapter(DebugActivity.this, apps);
                new MaterialDialog.Builder(DebugActivity.this)
                        .adapter(adapter, new LinearLayoutManager(DebugActivity.this))
                        .title(R.string.hideApps)
                        .positiveText(R.string.done)
                        .negativeText(R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                for(ApplicationHiderIcon ahi : adapter.getApps()){
                                    if(!ahi.getIsHidden())
                                        DatabaseEditor.getInstance().unmarkAppHidden(ahi.getActivityName(), ahi.getPackageName());
                                }
                                loadApps();
                            }
                        }).show();
            }
        }.execute();
    }

    @Override
    public void requestLocationPermission() {
        //TODO
    }

    @Override
    public void onWeatherFound(LTSForecastModel weather) {
        CleanedUpWeatherModel cModel = CleanedUpWeatherModel.parseFromLTSForceastModel(weather, this);
        conditionIv.setVisibility(cModel.getResourceId() != -1 ? View.VISIBLE : View.GONE);
        if (cModel.getResourceId() != -1)
            conditionIv.setImageResource(cModel.getResourceId());
        tempTv.setVisibility(cModel.getTemp() != null ? View.VISIBLE : View.GONE);
        if (cModel.getTemp() != null)
            tempTv.setText(cModel.getTemp());
        highLowTv.setVisibility(cModel.getHigh() != null ? View.VISIBLE : View.GONE);
        if (cModel.getHigh() != null) {
            highLowTv.setText("↑" + cModel.getHigh() + "/" + "↓" + cModel.getLow());
        }
    }

    @Override
    public void onFetchFailure() {
        Toast.makeText(this, R.string.error_getting_weather, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFavoritesChanged() {
        DatabaseEditor.getInstance().saveFavorites(favorites);
    }

    @Override
    public void onRendered() {
        Log.d(TAG, "onRendered");
        updateScrims();
    }

    @Override
    public void onScrollChanged() {
        int scrollY = scrollView.getScrollY();
        if(scrollY < space.getHeight()){
            float difference = space.getHeight() - scrollY;
            float ratio = 1 - (difference / space.getHeight());
            background.setAlpha(ratio);
        }
    }

    @Override
    public void openElement(ContextualElement element) {

    }
}
