package com.inipage.homelylauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.hardware.display.DisplayManagerCompat;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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

import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.model.Favorite;
import com.inipage.homelylauncher.scroller.FavoriteGridSplayer;
import com.inipage.homelylauncher.utils.Utilities;
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
public class DebugActivity extends Activity implements WeatherController.WeatherPresenter, FavoriteGridSplayer.FavoriteStateCallback, ViewTreeObserver.OnScrollChangedListener {
    public static final String TAG = "DebugActivity";

    private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm", Locale.getDefault());
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());

    @Bind(R.id.pointer_rl)
    PointerInfoRelativeLayout pointerRl;

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

    @Bind(R.id.grid_layout)
    AbsoluteLayout favoritesGrid;

    @Bind(R.id.overlay_layout)
    AbsoluteLayout overlayLayout;
    View attachedOverlay;

    //Header fields
    Timer timeUpdateTimer;

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

    List<ApplicationIcon> cachedApps = new ArrayList<>();
    PatriciaTrie<ApplicationIcon> appsTree = new PatriciaTrie<>();
    List<Favorite> favorites;
    FavoriteGridSplayer favManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        ButterKnife.bind(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        background.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

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
        updateScrims();

        if(scrollView.getViewTreeObserver().isAlive()) {
            scrollView.getViewTreeObserver().addOnScrollChangedListener(this);
        } else {
            Log.d(TAG, "dead1");
        }
    }

    int top = -1;
    int bottom = -1;
    private void updateScrims(){
        if(top != -1){
            updateSpacing();
            return;
        }

        //It appears setting this twice does not work
        ViewCompat.setOnApplyWindowInsetsListener(topScrim,
                new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                        top = insets.getSystemWindowInsetTop();
                        bottom = insets.getSystemWindowInsetBottom();

                        updateSpacing();

                        return insets.consumeSystemWindowInsets();
                    }
                });
    }

    private void updateSpacing(){
        LinearLayout.LayoutParams topParams = (LinearLayout.LayoutParams) topScrim.getLayoutParams();
        topParams.height = top;
        topScrim.setLayoutParams(topParams);

        LinearLayout.LayoutParams bottomParams = (LinearLayout.LayoutParams) bottomScrim.getLayoutParams();
        bottomParams.height = bottom;
        bottomScrim.setLayoutParams(bottomParams);

        //Set "space" to occupy...space [screen height - top scrim - bottom scrim - header size - [cell*space]]
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
        float cellDimension = (screenWidth / 5);

        LinearLayout.LayoutParams spaceParams = (LinearLayout.LayoutParams) space.getLayoutParams();
        //[screen height - top scrim - bottom scrim - header size - [cell*space]]

        int desiredSpace = (int) (screenHeight - top - bottom - headerSize - (cellDimension * 2));
        if(desiredSpace < 0)
            desiredSpace = 0;
        spaceParams.height = desiredSpace;
        space.setLayoutParams(spaceParams);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WeatherController.requestWeather(this, this, false);
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

        new AsyncTask<Object, Void, List<ApplicationIcon>>() {
            @Override
            protected List<ApplicationIcon> doInBackground(Object... params) {
                PackageManager pm = getPackageManager();
                List<ComponentName> hiddenApps = DatabaseEditor.getInstance().getHiddenApps();
                List<ApplicationIcon> applicationIcons = new ArrayList<>();

                //Grab all matching applications
                final Intent allAppsIntent = new Intent(Intent.ACTION_MAIN, null);
                allAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                final List<ResolveInfo> packageList = pm.queryIntentActivities(allAppsIntent, 0);
                for (ResolveInfo ri : packageList) {
                    try {
                        String name = (String) ri.loadLabel(pm);
                        if (!hiddenApps.contains(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)))
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
                return applicationIcons;
            }

            @Override
            protected void onPostExecute(List<ApplicationIcon> apps) {
                cachedApps = apps;
                populateAppList(null);
            }
        }.execute();
    }

    private void loadFavorites(){
        favorites = DatabaseEditor.getInstance().getFavorites();
        favManager = new FavoriteGridSplayer(favoritesGrid, favorites, this, 5);
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
    public void attachOverlay(View toAttach, int centerX, int centerY, int width, int height) {
        toAttach.setPivotY(centerY);
        toAttach.setPivotX(centerX);

        int x = centerX - (width / 2);
        int y = centerY - (height / 2);
        if(x < 0) x = 0;
        if(x > (scrollView.getWidth() - width)) x = scrollView.getWidth() - width;
        if(y < top) y = top;

        AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(width, height, x, y);
        overlayLayout.addView(toAttach, params);
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
}
