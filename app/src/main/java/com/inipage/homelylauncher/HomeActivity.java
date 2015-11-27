package com.inipage.homelylauncher;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.inipage.homelylauncher.background.SequentialLauncherService;
import com.inipage.homelylauncher.drawer.ApplicationHideAdapter;
import com.inipage.homelylauncher.drawer.ApplicationHiderIcon;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.drawer.ApplicationIconAdapter;
import com.inipage.homelylauncher.drawer.FastScroller;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.icons.IconChooserActivity;
import com.inipage.homelylauncher.swiper.AppEditAdapter;
import com.inipage.homelylauncher.swiper.RowEditAdapter;
import com.inipage.homelylauncher.utils.Utilities;
import com.inipage.homelylauncher.widgets.UpdateItem;
import com.inipage.homelylauncher.widgets.WidgetAddAdapter;
import com.inipage.homelylauncher.widgets.WidgetContainer;
import com.mobeta.android.dslv.DragSortListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

import butterknife.Bind;
import butterknife.ButterKnife;

@SuppressWarnings("unchecked")
public class HomeActivity extends Activity implements ShortcutGestureViewHost {
    public static final String TAG = "HomeActivity";

    public static final int REQUEST_CHOOSE_ICON = 200;
    public static final int REQUEST_CHOOSE_APPLICATION = 201;
    public static final int REQUEST_ALLOCATE_ID = 202;
    private static final int REQUEST_CONFIG_WIDGET = 203;
    private static final int REQUEST_PICK_APP_WIDGET = 204;
    private static final int REQUEST_PERMISSIONS = 205;

    public static final int HOST_ID = 505;
    private static final long ONE_DAY_MILLIS = 1000 * 60 * 60 * 24;

    public enum DockbarState {
        STATE_HOME, STATE_APPS, STATE_DROP
    }

    public enum ScreenSize {
        PHONE, SMALL_TABLET, LARGE_TABLET
    }

    //Dock states
    ScreenSize size;
    DockbarState currentState = DockbarState.STATE_HOME;

    //Apps stuff
    @Bind(R.id.allAppsContainer)
    RelativeLayout allAppsContainer;
    @Bind(R.id.allAppsLayout)
    RecyclerView allAppsScreen;
    @Bind(R.id.scrollerContainer)
    View scrollerBar;
    @Bind(R.id.startLetter)
    TextView startLetter;
    @Bind(R.id.endLetter)
    TextView endLetter;
    @Bind(R.id.popup)
    TextView popup;

    AsyncTask appsSearch = null;
    int cachedHash;

    //Dockbar background
    @Bind(R.id.dockBar)
    View dockBar;

    //Date stuff
    @Bind(R.id.timeDateContainer)
    View timeDateContainer;
    @Bind(R.id.date)
    TextView date;
    @Bind(R.id.alarm)
    TextView alarm;
    @Bind(R.id.hour)
    TextView hour;
    @Bind(R.id.minute)
    TextView minute;
    @Bind(R.id.timeLayout)
    View timeLayout;
    @Bind(R.id.timeColon)
    TextView timeColon;

    SimpleDateFormat minutes;
    SimpleDateFormat hours;
    SimpleDateFormat alarmTime = new SimpleDateFormat("h:mm aa", Locale.getDefault());
    Typeface light;
    Typeface regular;
    Typeface condensed;
    Timer timer;

    //Home dockbar
    @Bind(R.id.dockApps)
    LinearLayout dockbarApps;
    @Bind(R.id.dockApp1)
    ImageView db1;
    @Bind(R.id.dockApp2)
    ImageView db2;
    @Bind(R.id.dockApp3)
    ImageView db3;
    @Bind(R.id.dockApp4)
    ImageView db4;
    @Bind(R.id.dockApp5)
    ImageView db5;
    @Bind(R.id.dockApp6)
    ImageView db6;
    @Bind(R.id.dockApp7)
    ImageView db7;

    //Search/menu
    @Bind(R.id.searchActionBar)
    RelativeLayout searchActionBar;
    @Bind(R.id.backToHome)
    ImageView backToHome;
    @Bind(R.id.moreOptions)
    ImageView allAppsMenu;
    @Bind(R.id.clearSearch)
    ImageView clearSearch;
    @Bind(R.id.searchBox)
    EditText searchBox;

    //Drop layout
    @Bind(R.id.dropLayout)
    LinearLayout dropLayout;
    //App drop layout
    @Bind(R.id.appDropIcons)
    RelativeLayout appDropLayout;
    @Bind(R.id.addToDock)
    View addToDock;
    @Bind(R.id.uninstallApp)
    View uninstallApp;
    @Bind(R.id.appInfo)
    View appInfo;

    //Database saving/loading
    DatabaseHelper dh;
    SQLiteDatabase db;

    //A view which exists just to listen to respond to drop events properly
    @Bind(R.id.dropListener)
    View dragListener;

    //A view for fading behind nav
    @Bind(R.id.statusBarBackdrop)
    View bigTint;

    //A view for catching strange touches
    @Bind(R.id.strayTouchShield)
    View strayTouchCatch;

    //GestureView for opening/closing apps
    @Bind(R.id.sgv)
    ShortcutGestureView sgv;

    //Widget overlay
    @Bind(R.id.widgetOverlay)
    View widgetOverlay;
    @Bind(R.id.widgetOverlayDisplay)
    LinearLayout widgetOverlayContainer;
    @Bind(R.id.widgetOverlayClose)
    View widgetOverlayClose;
    @Bind(R.id.widgetOverlaySettings)
    View widgetOverlaySettings;
    @Bind(R.id.widgetOverlayIcon)
    ImageView widgetOverlayIcon;

    private HashMap<String, Boolean> hasWidgetMap = new HashMap<>();

    //Suggestions
    @Bind(R.id.no_suggestions_right_now)
    TextView noSuggestions;

    @Bind(R.id.suggestionsLayout)
    LinearLayout suggestionsLayout;

    boolean editingWidget = false;

    AppWidgetManager widgetManager;
    AppWidgetHost widgetHost;

    //Settings
    SharedPreferences reader;
    SharedPreferences.Editor writer;

    List<TypeCard> samples;
    List<WidgetContainer> widgets;
    List<Pair<String, String>> hiddenApps;

    View dialogView;
    String packageName;
    String resourceName;
    String cachedPref;

    Handler testHandler;
    //List<AppWidgetHostView> testRef = new ArrayList<>();

    /** Some dialogs have onDismiss(...) listeners that open their 'parent' dialogs. This variable
     * lets us know when we intentionally raise a child dialog of depth two, so as not to trigger
     * the onDismiss(...) showing of a depth 1 dialog from a child dialog of depth 2. Yes,
     * this is a gimmicky hack until we have better dialog management.
     */
    boolean depthThreeDialogRaised = false;

    //Tell when things have changed
    BroadcastReceiver packageReceiver;
    BroadcastReceiver storageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        testHandler = new Handler();
        testHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                spillWidgets();
            }
        }, 5000);

        if(Utilities.isSmallTablet(this)){
            size = ScreenSize.SMALL_TABLET;
        } else if (Utilities.isLargeTablet(this)){
            size = ScreenSize.LARGE_TABLET;
        } else {
            size = ScreenSize.PHONE;
        }

        widgetManager = AppWidgetManager.getInstance(this);
        widgetHost = new AppWidgetHost(this, HOST_ID);

        //Open databases
        dh = new DatabaseHelper(this);
        try {
            db = dh.getWritableDatabase();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open database!", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }
        reader = PreferenceManager.getDefaultSharedPreferences(this);
        writer = reader.edit();

        minutes = new SimpleDateFormat("mm", Locale.US);
        hours = new SimpleDateFormat("h", Locale.US);
        light = Typeface.createFromAsset(this.getAssets(), "Roboto-Thin.ttf");
        regular = Typeface.createFromAsset(this.getAssets(), "Roboto-Regular.ttf");
        condensed = Typeface.createFromAsset(this.getAssets(), "Roboto-Condensed.ttf");

        hour.setTypeface(light);
        minute.setTypeface(light);
        timeColon.setTypeface(light);
        timeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDedicatedAppButton(Constants.CLOCK_APP_PREFERENCE, false);
            }
        });
        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDedicatedAppButton(Constants.CALENDAR_APP_PREFERENCE, false);
            }
        });

        samples = new ArrayList<>();
        widgets = new ArrayList<>();
        hiddenApps = new ArrayList<>();

        //Set up all apps button
        cachedHash = -1;

        //Allow tablet to rotate
        if(Utilities.isSmallTablet(this) || Utilities.isLargeTablet(this)){
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }

        //Set up dockbar apps

        setupDockbarTarget(db1, 1);
        setupDockbarTarget(db2, 2);
        setupDockbarTarget(db3, 3);
        setupDockbarTarget(db4, 4);
        setupDockbarTarget(db5, 5);

        if(size == ScreenSize.PHONE) {
            Log.d(TAG, "Is phone");

            dockbarApps.findViewById(R.id.fiveToSix).setVisibility(View.GONE);
            dockbarApps.findViewById(R.id.sixToSeven).setVisibility(View.GONE);
            db6.setVisibility(View.GONE);
            db7.setVisibility(View.GONE);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) db5.getLayoutParams();
            layoutParams.setMargins(0, 0,
                    ((LinearLayout.LayoutParams)db1.getLayoutParams()).leftMargin, 0);
            db5.setLayoutParams(layoutParams);

            dockbarApps.requestLayout();
        } else { //Two extra spots for tablets
            Log.d(TAG, "Is tablet");

            setupDockbarTarget(db6, 6);
            setupDockbarTarget(db7, 7);

            dockbarApps.requestLayout();
        }

        sgv.setActivity(this);

        //Search action bar
        strayTouchCatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do nothing
            }
        });
        backToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAppsContainer(false);
                hideKeyboard();
                setDockbarState(DockbarState.STATE_HOME, true);
            }
        });
        allAppsMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pm = new PopupMenu(v.getContext(), v);
                pm.inflate(R.menu.popup_menu);
                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.hideApps:
                                showHideAppsMenu();
                                break;
                            case R.id.help:
                                showTutorial();
                                break;
                            case R.id.settings:
                                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                                break;
                            case R.id.changeWallpaper:
                                toggleAppsContainer(false);
                                setDockbarState(DockbarState.STATE_HOME, true);
                                final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
                                startActivity(Intent.createChooser(pickWallpaper, "Set Wallpaper"));
                                break;
                            case R.id.reorderRows:
                                showReorderRowsDialog();
                                break;
                        }
                        return true;
                    }
                });
                pm.show();
            }
        });

        DrawableCompat.setTint(searchBox.getBackground(), getResources().getColor(R.color.black));
        clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBox.setText("");
                clearSearch.setVisibility(View.GONE);
                hideKeyboard();
            }
        });
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String change = s.toString();
                resetAppsList(change, false);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    //Try and open app
                    ((ApplicationIconAdapter) allAppsScreen.getAdapter()).launchTop();
                }
                return handled;
            }
        });

        //Set drag type of dockbar
        uninstallApp.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                //We only can operate an "apps"
                if (event.getLocalState() == null ||
                        !(event.getLocalState() instanceof ApplicationIcon)) return false;

                ApplicationIcon ai  = (ApplicationIcon) event.getLocalState();
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    try {
                        Uri uri = Uri.parse("package:" + ai.getPackageName());
                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                        uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                        v.getContext().startActivity(uninstallIntent);
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(),
                                "The app is unable to be removed.", Toast.LENGTH_LONG).show();
                    }
                } else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED){
                    //Tint red
                    Log.d(TAG, "Tinting red");
                    tintDropLayout(TintColor.RED);
                } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                    Log.d(TAG, "CLearing tint");
                    tintDropLayout(TintColor.CLEAR);
                }
                return true;
            }
        });
        appInfo.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    ApplicationIcon ai = (ApplicationIcon) event.getLocalState();
                    try {
                        Uri uri = Uri.parse("package:" + ai.getPackageName());
                        Intent uninstallIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                        uninstallIntent.setData(uri);
                        v.getContext().startActivity(uninstallIntent);
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(),
                                "The app does not have info.", Toast.LENGTH_LONG).show();
                    }
                } else if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED){
                    tintDropLayout(TintColor.BLUE);
                } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                    tintDropLayout(TintColor.CLEAR);
                }
                return true;
            }
        });
        addToDock.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                    tintDropLayout(TintColor.GREEN);
                    setDockbarState(DockbarState.STATE_HOME, true);
                } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                    tintDropLayout(TintColor.CLEAR);
                }
                return true;
            }
        });

        //Listener for drags on the homescreen
        dragListener.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                int action = event.getAction();
                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        if (event.getLocalState() instanceof ApplicationIcon) { //Moving apps around
                            appDropLayout.setVisibility(View.VISIBLE);
                            toggleAppsContainer(false);
                            setDockbarState(DockbarState.STATE_DROP, true);
                        }
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        setDockbarState(DockbarState.STATE_HOME, true);
                        break;
                }
                return true;
            }
        });

        //Move the screen as needed
        toggleAppsContainer(false);
        setDockbarState(DockbarState.STATE_HOME, false);

        //Check if we've run before
        if (!reader.getBoolean(Constants.HAS_RUN_PREFERENCE, false)) {
            writer.putInt(Constants.VERSION_PREF, Constants.Versions.CURRENT_VERSION).apply();
            sgv.setCards(new ArrayList<TypeCard>());

            showTutorial();
        } else {
            //Do version upgrades
            int lastUsedVersion = reader.getInt(Constants.VERSION_PREF, Constants.Versions.VERSION_0_2_3);
            switch(lastUsedVersion){
                case Constants.Versions.VERSION_0_2_3:
                    writer.putBoolean(Constants.HOME_WIDGET_PREFERENCE, false).commit();
                    writer.putInt(Constants.HOME_WIDGET_ID_PREFERENCE, -1).commit();
                    break;
            }

            if(lastUsedVersion != Constants.Versions.CURRENT_VERSION){
                writer.putInt(Constants.VERSION_PREF, Constants.Versions.CURRENT_VERSION).commit();
            }

            loadList(samples);
            loadWidgets(widgets);
            loadHiddenApps(hiddenApps);
        }

        //Set up broadcast receivers
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(searchBox != null) {
                    searchBox.setText(""); //The TextWatcher resets the app list in this case
                } else {
                    resetAppsList("", false);
                }
                sgv.invalidateCaches();
                IconCache.getInstance().invalidateCaches();
                verifyWidgets();
            }
        };

        storageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null) {
                    if (searchBox != null){
                        searchBox.setText(""); //The TextWatcher resets the app list in this case
                    } else {
                        resetAppsList("", false);
                    }
                    verifyWidgets();
                }
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
        //storageFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(storageReceiver, storageFilter);

        //Setup app drawer
        updateRowCount(getResources().getConfiguration().orientation);
    }


    private enum TintColor {
        RED, BLUE, GREEN, CLEAR
    }

    private void tintDropLayout(TintColor color){
        Drawable newBg = new ColorDrawable(Color.TRANSPARENT);
        switch(color){
            case RED:
                newBg = getResources().getDrawable(R.drawable.light_red_to_transparent_bg);
                break;
            case BLUE:
                newBg = getResources().getDrawable(R.drawable.light_blue_to_transparent_bg);
                break;
            case GREEN:
                newBg = getResources().getDrawable(R.drawable.light_green_to_transparent_bg);
                break;
            case CLEAR:
                break;
        }

        dropLayout.setBackgroundDrawable(newBg);
    }

    private void spillWidgets() {
        /*
        Log.d(TAG, "Spilling widgets...");
        String line = "";
        for(AppWidgetHostView refs : testRef) {
            line += getTextData(refs);
        }
        Log.d(TAG, "Line: " + line);

        testHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                spillWidgets();
            }
        }, 5000);
        */
    }

    private String getTextData(ViewGroup layout){
        if(layout == null) return "";

        String toReturn = "";
        for(int i = 0; i < layout.getChildCount(); i++){
            View child = layout.getChildAt(0);
            if(child instanceof ViewGroup){
                ViewGroup childGroup = (ViewGroup) child;
                toReturn += getTextData(childGroup);
            } else if (child instanceof Button){
                Button childButton = (Button) child;
                toReturn += childButton.getText() + ":::";
            } else if (child instanceof TextView) {
                TextView childTextView = (TextView) child;
                toReturn += childTextView.getText() + ":::";
            }
        }

        return toReturn;
    }

    private void loadHiddenApps(List<Pair<String, String>> hiddenApps) {
        Log.d(TAG, "Loading hidden apps...");
        hiddenApps.clear();

        loadRows: {
            Cursor loadItems =
                    db.query(DatabaseHelper.TABLE_HIDDEN_APPS, null, null, null, null, null, null);
            if (loadItems.moveToFirst()) {
                Log.d(TAG, "Found: " + loadItems.getCount());

                int packageColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_PACKAGE);
                int activityColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_ACTIVITY_NAME);
                if (packageColumn == -1 || activityColumn == -1) {
                    loadItems.close();
                    break loadRows;
                }

                while (!loadItems.isAfterLast()) {
                    String packageName = loadItems.getString(packageColumn);
                    String activityName = loadItems.getString(activityColumn);

                    Log.d(TAG, "Loading '" + packageName + "'...");
                    hiddenApps.add(new Pair<String, String>(packageName, activityName));

                    loadItems.moveToNext();
                }
            } else {
                Log.d(TAG, "No hidden apps found!");
            }
            loadItems.close();
        }
    }

    private void showHideAppsMenu() {
        new AsyncTask<Void, Void, List<ApplicationHiderIcon>>(){
            @Override
            protected List<ApplicationHiderIcon> doInBackground(Void... params) {
                List<ApplicationHiderIcon> applicationIcons = new ArrayList<>();

                //Grab all matching applications
                try {
                    final Intent allAppsIntent = new Intent(Intent.ACTION_MAIN, null);
                    allAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    final List<ResolveInfo> packageList = getPackageManager().queryIntentActivities(allAppsIntent, 0);
                    for (ResolveInfo ri : packageList) {
                        try {
                            String name = (String) ri.loadLabel(getPackageManager());
                            if (hiddenApps.contains(new Pair<>(ri.activityInfo.packageName,
                                    ri.activityInfo.name))) {
                                applicationIcons.add(new ApplicationHiderIcon(ri.activityInfo.packageName,
                                        name, ri.activityInfo.name, true));
                            } else {
                                applicationIcons.add(new ApplicationHiderIcon(ri.activityInfo.packageName,
                                        name, ri.activityInfo.name, false));
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
                } catch (RuntimeException packageManagerDiedException){
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<ApplicationHiderIcon> apps){
                final ApplicationHideAdapter adapter = new ApplicationHideAdapter(HomeActivity.this,
                        R.layout.application_icon_hidden, apps);

                new MaterialDialog.Builder(HomeActivity.this)
                        .adapter(adapter, new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                //Do nothing
                            }
                        })
                        .title(R.string.hideApps)
                        .positiveText(R.string.done)
                        .negativeText(R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                persistHidden(adapter.getApps());
                                loadHiddenApps(hiddenApps);
                                resetAppsList("", false);
                            }
                        }).show();
            }
        }.execute();
    }

    private void persistHidden(List<ApplicationHiderIcon> apps) {
        Log.d(TAG, "Persisting at most " + widgets.size() + " hidden apps");
        db.delete(DatabaseHelper.TABLE_HIDDEN_APPS, null, null);
        for(int i = 0; i < apps.size(); i++){
            if(!apps.get(i).getIsHidden()) continue;

            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COLUMN_ACTIVITY_NAME, apps.get(i).getActivityName());
            cv.put(DatabaseHelper.COLUMN_PACKAGE, apps.get(i).getPackageName());

            Log.d(TAG, "Trying to insert: " + cv);

            long result = db.insert(DatabaseHelper.TABLE_HIDDEN_APPS, null, cv);

            Log.d(TAG, result == -1 ? "Error inserting: " + i :
                    "Inserted hidden: " + i + " at " + result);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updateRowCount(newConfig.orientation);
    }

    public void updateRowCount(int orientation){
        int columnCount;

        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            if(Utilities.isSmallTablet(this)){
                size = ScreenSize.SMALL_TABLET;
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                columnCount = 7;
            } else if (Utilities.isLargeTablet(this)){
                size = ScreenSize.LARGE_TABLET;
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                columnCount = 8;
            } else {
                size = ScreenSize.PHONE;
                columnCount = 4;
            }
        } else {
            if(Utilities.isSmallTablet(this)){
                size = ScreenSize.SMALL_TABLET;
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                columnCount = 5;
            } else if (Utilities.isLargeTablet(this)){
                size = ScreenSize.LARGE_TABLET;
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                columnCount = 7;
            } else {
                size = ScreenSize.PHONE;
                columnCount = 4;
            }
        }

        GridLayoutManager glm = new GridLayoutManager(this, columnCount);
        allAppsScreen.setLayoutManager(glm);
        resetAppsList("", true);
    }

    private void showTutorial() {
        new MaterialDialog.Builder(this)
                .title(R.string.welcome_title)
                .content(R.string.welcome_message)
                .positiveText(R.string.lets_go)
                .neutralText(R.string.tutorial_please)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        Uri urlUri = Uri.parse("http://youtu.be/IX-c5GJ3FZE");
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_VIEW);
                        i.setData(urlUri);
                        dialog.getContext().startActivity(i);
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.d(TAG, "Saving has run preferences...");
                        writer.putBoolean(Constants.HAS_RUN_PREFERENCE, true).apply();
                        startSuggestionsPopulation();
                    }
                }).show();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showUsageMessage(){
        writer.putBoolean(Constants.HAS_REQUESTED_USAGE_PERMISSION_PREF, true).commit();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Show usage settings dialog
            new MaterialDialog.Builder(this)
                .title(R.string.enable_usage_viewing)
                .content(R.string.enable_usage_viewing_message)
                .positiveText(R.string.enable)
                .negativeText(R.string.nope)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        Toast.makeText(HomeActivity.this, R.string.aww, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Intent showIntent = new Intent();
                        showIntent.setAction(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(showIntent);
                        HomeActivity.this.finish();
                    }
                }).show();
        }
    }

    public void batchOpen(int row) {
        if(row >= 0 && row < sgv.data.size()){
            Intent sequentialLauncherService = new Intent(this, SequentialLauncherService.class);
            sequentialLauncherService.putExtra("row_position", row);
            this.startService(sequentialLauncherService);
        }
    }

    @Override
    public boolean hasWidget(String packageName) {
        Boolean widgetExists = hasWidgetMap.get(packageName);
        return widgetExists == null ? false : widgetExists;
    }

    public WidgetContainer findContainer(String packageName){
        for (int i = 0; i < widgets.size(); i++) {
            if (packageName.equalsIgnoreCase(widgets.get(i).getWidgetPackage())) {
                return widgets.get(i);
            }
        }
        return null;
    }

    @Override
    public void showWidget(final String packageName) {
        Log.d(TAG, "Showing widget for: " + packageName);

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Show the widget overlay
                //(1) Do we have a match?
                int matchIndex = -1;
                for (int i = 0; i < widgets.size(); i++) {
                    if (packageName.equalsIgnoreCase(widgets.get(i).getWidgetPackage())) {
                        matchIndex = i;
                        break;
                    }
                }

                //(2) Yes -- show overlay and make widget visible
                if (matchIndex != -1) {
                    widgetOverlayContainer.getChildAt(matchIndex).setVisibility(View.VISIBLE);
                    //Wow that's a lot in one line.
                    try {
                        Drawable icon = getPackageManager().getActivityIcon(getPackageManager().getLaunchIntentForPackage(packageName));
                        widgetOverlayIcon.setImageDrawable(icon);
                    } catch (Exception ignored) {
                    }
                    widgetOverlayClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideWidgetOverlay();
                        }
                    });
                    widgetOverlaySettings.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PopupMenu pm = new PopupMenu(v.getContext(), widgetOverlaySettings);
                            pm.inflate(R.menu.widget_context_menu);
                            pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    WidgetContainer wc = findContainer(packageName);
                                    int indexOfWidget = widgets.indexOf(wc);

                                    if(wc == null){
                                        hideWidgetOverlay();
                                        return false; //Should we just crash? This should never happen
                                    }

                                    switch (item.getItemId()) {
                                        case R.id.resizeWidget:
                                            showResizeDialog(wc.getWidgetId(),
                                                    (AppWidgetHostView) widgetOverlayContainer.getChildAt(indexOfWidget));
                                            break;
                                        case R.id.swapWidget:
                                            widgetOverlayContainer.removeViewAt(indexOfWidget);
                                            widgetHost.deleteAppWidgetId(wc.getWidgetId());
                                            widgets.remove(wc);
                                            showAddWidgetMenu(packageName);
                                            break;
                                    }
                                    return true;
                                }
                            });
                            pm.show();
                        }
                    });
                    widgetOverlay.setVisibility(View.VISIBLE);
                } else { //(2) No -- show add menu
                    showAddWidgetMenu(packageName);
                }
            }
        });
    }

    @Override
    public void hideWidgetOverlay() {
        for (int i = 0; i < widgetOverlayContainer.getChildCount(); i++) {
            widgetOverlayContainer.getChildAt(i).setVisibility(View.GONE);
        }
        widgetOverlay.setVisibility(View.GONE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showAddWidgetMenu(final String packageName) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<AppWidgetProviderInfo> installedProviders = widgetManager.getInstalledProviders();
                List<AppWidgetProviderInfo> matchingProviders = new ArrayList<>();
                for (AppWidgetProviderInfo awpi : installedProviders) {
                    if (((awpi.widgetCategory & AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) == AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
                            && awpi.provider.getPackageName().equalsIgnoreCase(packageName)) {
                        matchingProviders.add(awpi);
                    }
                }

                final WidgetAddAdapter adapter = new WidgetAddAdapter(HomeActivity.this, R.layout.widget_preview, matchingProviders);

                final MaterialDialog md = new MaterialDialog.Builder(HomeActivity.this)
                        .adapter(adapter, new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                            }
                        })
                        .title("Select a " + matchingProviders.get(0).label + " Widget")
                        .negativeText(R.string.cancel)
                        .build();

                ListView lv = md.getListView();
                if (lv != null)
                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            AppWidgetProviderInfo awpi = adapter.getItem(position);
                            int appWidgetId = widgetHost.allocateAppWidgetId();
                            if (widgetManager.bindAppWidgetIdIfAllowed(appWidgetId, awpi.provider)) {
                                //Carry on with configuring
                                handleWidgetConfig(appWidgetId, awpi);
                            } else {
                                //Ask for permission
                                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, awpi.provider);
                                startActivityForResult(intent, REQUEST_ALLOCATE_ID);
                            }
                            md.hide();
                        }
                    });

                md.show();
            }
        });
    }

    private void showReorderRowsDialog() {
        DragSortListView dsiv = (DragSortListView) LayoutInflater.from(this).inflate(R.layout.sort_row_list, null);
        dsiv.setAdapter(new RowEditAdapter(this,
                R.layout.row_edit_row, samples));
        dsiv.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        new MaterialDialog.Builder(this)
                .customView(dsiv, false)
                .title("Re-order")
                .positiveText("Done")
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        persistList(samples);
                    }
                })
                .show();
    }

    private void verifyWidgets() {
        Log.d(TAG, "Verifying widgets...");
        final List<WidgetContainer> toRemove = new ArrayList<>();
        for(WidgetContainer wc : widgets){
            if(widgetManager.getAppWidgetInfo(wc.getWidgetId()) == null){
                Log.d(TAG, wc.getWidgetId() + " @ height " + wc.getWidgetHeight() + " is null...");
                toRemove.add(wc);
            }

            try {
                getPackageManager().getResourcesForApplication(widgetManager.getAppWidgetInfo(wc.getWidgetId()).provider.getPackageName());
            } catch (Exception e){
                Log.d(TAG, wc.getWidgetId() + " @ height " + wc.getWidgetHeight() + " isn't installed...");
                toRemove.add(wc);
            }
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (WidgetContainer wc : toRemove) {
                    Log.d(TAG, "Removing " + wc.getWidgetId());
                    int removalIndex = widgets.indexOf(wc);
                    if (removalIndex != -1) {
                        widgets.remove(removalIndex);
                        if (removalIndex < widgetOverlayContainer.getChildCount())
                            widgetOverlayContainer.removeViewAt(removalIndex);
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if(packageReceiver != null){
            unregisterReceiver(packageReceiver);
        }
        if(storageReceiver != null){
            unregisterReceiver(storageReceiver);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        updateDisplay();

        //Update most parts on timer
        TimerTask t = new TimerTask(){
            @Override
            public void run() {
                updateDisplay();
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(t, 0, 1000);

        //Reset widgets
        hideWidgetOverlay();
        updateWidgetInfo();

        sgv.onActivityResumed();
        startSuggestionsPopulation();
    }

    private void updateWidgetInfo() {
        hasWidgetMap.clear();
        List<AppWidgetProviderInfo> installedProviders = widgetManager.getInstalledProviders();
        for (AppWidgetProviderInfo awpi : installedProviders) {
            if ((awpi.widgetCategory & AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
                    == AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) {
                hasWidgetMap.put(awpi.provider.getPackageName(), true);
            }
        }
    }

    private void startSuggestionsPopulation(){
        if(!reader.getBoolean(Constants.HAS_RUN_PREFERENCE, false)) return;

        Log.d(TAG, "Starting suggestions population...");

        //We always show a prompt UI to explain if we need to ask for permissions.
        String[] permissions = new String[] { Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALENDAR };
        List<String> neededPermissions = new ArrayList<>();

        for(String permission : permissions){
            if(!Utilities.checkPermission(permission, this)) neededPermissions.add(permission);
        }

        if(neededPermissions.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !reader.getBoolean(Constants.HAS_REQUESTED_PERMISSIONS_PREF, false)){
            Log.d(TAG, "Requesting permissions for the first time; showing message...");
            final String[] requested = neededPermissions.toArray(new String[0]);

            new MaterialDialog.Builder(HomeActivity.this)
                    .title(R.string.permissions_needed_for_smartbar)
                    .content(R.string.smartbar_needs_permissions)
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            requestPermissions(requested, REQUEST_PERMISSIONS);
                        }
                    })
                    .positiveText(R.string.got_it)
                    .show();
        } else {
            populateSuggestions();
        }
    }

    private ApplicationIcon addSmartbarAppFromPref(String prefName){
        String existingData = reader.getString(prefName, "null");
        if(!existingData.equals("null")){
            Log.d(TAG, "Existing data: " + existingData);
            Gson gson = new Gson();
            try {
                ApplicationIcon ai = gson.fromJson(existingData, ApplicationIcon.class);
                return ai;
            } catch (Exception ignored) {
                return null;
            }
        } else {
            return null;
        }
    }

    private void addSuggestionsApp(String packageName){
        View v = LayoutInflater.from(HomeActivity.this).inflate(R.layout.popular_app_icon, suggestionsLayout, false);

        final Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if(launchIntent == null) return;

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    v.getContext().startActivity(launchIntent);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), R.string.cant_start, Toast.LENGTH_SHORT).show();
                }
            }
        });

        IconCache.getInstance().setSuggestionsIcon(packageName, (ImageView) v);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity |= Gravity.CENTER_VERTICAL;

        int eightDp = (int) Utilities.convertDpToPixel(8f, this);

        params.setMargins(eightDp, eightDp, eightDp, eightDp);
        suggestionsLayout.addView(v, params);
    }

    private void addSuggestionsApp(String packageName, String className){
        final Intent appLaunch = new Intent();
        appLaunch.setClassName(packageName, className);
        appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        View v = LayoutInflater.from(HomeActivity.this).inflate(R.layout.popular_app_icon, null);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    v.getContext().startActivity(appLaunch);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Couldn't start this app!", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        IconCache.getInstance().setDockIcon(packageName, className, ((ImageView) v));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity |= Gravity.CENTER_VERTICAL;

        int eightDp = (int) Utilities.convertDpToPixel(8f, this);

        params.setMargins(eightDp, eightDp, eightDp, eightDp);
        suggestionsLayout.addView(v, params);
    }

    @SuppressLint("NewApi")
    private void populateSuggestions() {
        //Clean up
        suggestionsLayout.removeAllViews();
        noSuggestions.setVisibility(View.VISIBLE);

        //(Prep)
        //Figure out max number of smartBar apps
        final int mostApps = reader.getInt(Constants.SMART_BAR_NUM_PREFERENCE, 10);
        int appsAdded = 0;

        //(1) Check if we're in a phone call
        boolean hasPhonePermission = Utilities.checkPermission(Manifest.permission.READ_PHONE_STATE, this);

        if(hasPhonePermission) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            int state = tm.getCallState();
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) { //In call
                ApplicationIcon ai = addSmartbarAppFromPref(Constants.PHONE_APP_PREFERENCE);
                if (ai != null) {
                    addSuggestionsApp(ai.getPackageName(), ai.getActivityName());
                    appsAdded++;
                    noSuggestions.setVisibility(View.GONE);
                }
            }
        }

        //(2) Check if we're in a calendar event
        boolean hasCalendarPermission = Utilities.checkPermission(Manifest.permission.READ_CALENDAR, this);

        if(hasCalendarPermission) {
            ContentResolver cr = getContentResolver();
            String[] columns = new String[]{CalendarContract.Instances.DTSTART, CalendarContract.Instances.DTEND,
                    CalendarContract.Instances.TITLE};
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, System.currentTimeMillis());
            ContentUris.appendId(builder, System.currentTimeMillis() + (1000l * 60l * 60l * 24l));
            Cursor c = cr.query(builder.build(), columns, null, null, CalendarContract.Instances.DTSTART + " asc");
            calendarBlock:
            {
                if (c != null && c.moveToFirst()) {
                    Log.d(TAG, "Calendar block move to first!");

                    int startCol = c.getColumnIndex(CalendarContract.Instances.DTSTART);
                    int endCol = c.getColumnIndex(CalendarContract.Instances.DTEND);
                    int titleCol = c.getColumnIndex(CalendarContract.Instances.TITLE);

                    if (startCol == -1 || endCol == -1 || titleCol == -1 || c.getCount() == 0) {
                        Log.d(TAG, "Count/columns 0!");
                        c.close();
                        break calendarBlock;
                    }


                    long startTime = c.getLong(startCol);
                    long endTime = c.getLong(endCol);
                    long duration = c.getLong(endCol) - c.getLong(startCol);

                    Log.d(TAG, "Curr: " + System.currentTimeMillis() + ", start: " + startTime + ", end: " + endTime + ", duration: " + duration);

                    if (System.currentTimeMillis() > startTime && System.currentTimeMillis() < endTime) {
                        Log.d(TAG, "In event!");

                        ApplicationIcon ai = addSmartbarAppFromPref(Constants.CALENDAR_APP_PREFERENCE);
                        if (ai != null) {
                            addSuggestionsApp(ai.getPackageName(), ai.getActivityName());
                            appsAdded++;
                            noSuggestions.setVisibility(View.GONE);
                        }
                    }

                    c.close();
                }
            }
        }

        //(3) Check if we're almost out of power
        if(hasPhonePermission) {
            IntentFilter batteryReading = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, batteryReading);

            //Get percent
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPercent = level / (float) scale;

                //Get isCharging
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

                if (batteryPercent < 0.2 && !isCharging) {
                    ApplicationIcon ai = addSmartbarAppFromPref(Constants.LOW_POWER_APP_PREFERENCE);
                    if (ai != null) {
                        addSuggestionsApp(ai.getPackageName(), ai.getActivityName());
                        appsAdded++;
                        noSuggestions.setVisibility(View.GONE);
                    }
                } else if (isCharging) {
                    ApplicationIcon ai = addSmartbarAppFromPref(Constants.CHARGING_APP_PREFERENCE);
                    if (ai != null) {
                        addSuggestionsApp(ai.getPackageName(), ai.getActivityName());
                        appsAdded++;
                        noSuggestions.setVisibility(View.GONE);
                    }
                }
            }
        }

        //(4) Check condition rules (WEEKDAY apps, WEEKEND apps, WORK apps, HOME apps)
        //TODO: Condition rules

        //(5) Use remaining space to show popular apps
        // Get data from system usage events and integrate into our own database
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final int appsAddedCount = appsAdded;

            //TODO: Make this more useful

            new AsyncTask<Void, Void, List<String>>() {
                @Override
                protected List<String> doInBackground(Void... params) {
                    try {
                        //Before we begin, query and generate a list of (a) all events in the query period
                        // (b) all events in a 4 hour window of this weekday (c) all events in the past week
                        // (d) calendar-specific events
                        @SuppressWarnings("ResourceType")
                        final UsageStatsManager usm = (UsageStatsManager) HomeActivity.this.getSystemService("usagestats");

                        long end = System.currentTimeMillis();
                        long start = end - (7 * 24 * 60 * 60 * 1000);

                        //Use results map to intelligently insert new data gotten from the system in
                        //the same timespan
                        HashMap<String, Integer> count = new HashMap<>();
                        UsageEvents use = usm.queryEvents(start, end);
                        UsageEvents.Event e = new UsageEvents.Event();

                        while(use.getNextEvent(e)) {
                            int type = e.getEventType();

                            //Log.d(TAG, "Found event with type: " + type + " and pkg " + e.getPackageName());

                            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                                long time = e.getTimeStamp();
                                String packageName = e.getPackageName();
                                String className = e.getClassName();

                                if(canLaunch(packageName, className)) {
                                    if (count.containsKey(packageName)) {
                                        count.put(packageName, count.get(packageName) + 1);
                                    } else {
                                        count.put(packageName, 1);
                                    }
                                }
                            }
                        }

                        List<Pair<String, Integer>> toSort = new ArrayList<>();
                        for(Map.Entry<String, Integer> entry : count.entrySet()){
                            toSort.add(new Pair<>(entry.getKey(), entry.getValue()));
                        }

                        Collections.sort(toSort, new Comparator<Pair<String, Integer>>() {
                            @Override
                            public int compare(Pair<String, Integer> lhs, Pair<String, Integer> rhs) {
                                return rhs.second.compareTo(lhs.second);
                            }
                        });

                        List<String> strippedList = new ArrayList<>();
                        for(Pair<String, Integer> pair : toSort){
                            strippedList.add(pair.first);
                        }

                        return strippedList;
                    } catch (Exception e) {
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(List<String> result) {
                    if (result != null) { //Success; samples has been updated; we should reset the adapter
                        Log.d(TAG, "Success!");

                        int appsAddedInner = appsAddedCount;
                        for(final String packageName : result){
                            if(appsAddedInner > mostApps)
                                return;
                            addSuggestionsApp(packageName);
                            appsAddedInner++;
                            noSuggestions.setVisibility(View.GONE);
                        }
                    } else { //Failure; log it
                        Log.d(TAG, "Failed to generate meaningful usage cards");
                    }
                }
            }.execute();
        } else { //Get running tasks as a fallback for usage stats
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> runningApps = am.getRunningTasks(10);
            List<String> handledPackages = new ArrayList<>(10);

            if(runningApps.isEmpty()) return;

            for(ActivityManager.RunningTaskInfo rti : runningApps){
                if(appsAdded > mostApps)
                    break;
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(rti.baseActivity.getPackageName());
                if(launchIntent != null && !handledPackages.contains(launchIntent.getPackage())) {
                    String packageName = launchIntent.getComponent().getPackageName();
                    String className = launchIntent.getComponent().getClassName();

                    handledPackages.add(launchIntent.getPackage());
                    addSuggestionsApp(packageName, className);
                    appsAdded++;
                    noSuggestions.setVisibility(View.GONE);
                }
            }
        }
    }

    public boolean canLaunch(String packageName, String className) {
        Intent intent = new Intent();
        intent.setClassName(packageName, className);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return !list.isEmpty();
    }

    @Override
    protected void onStart() {
        super.onStart();
        widgetHost.startListening();
    }
    @Override
    protected void onStop() {
        super.onStop();
        widgetHost.stopListening();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { //Back button pressed
            fadeDateTime(1, 300);
            resetState();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        writer.putBoolean(Constants.HAS_REQUESTED_PERMISSIONS_PREF, true).commit();
        boolean allGranted = true;
        for(int result : grantResults) allGranted &= result == PackageManager.PERMISSION_GRANTED;

        if (!allGranted) {
            Toast.makeText(HomeActivity.this, R.string.okay_we_wont_ask, Toast.LENGTH_SHORT).show();
        }
        populateSuggestions();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(Intent.ACTION_MAIN)) { //Home button press; reset state
            fadeDateTime(1, 300);
            resetState();
        }
    }

    public void resetState() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Clear search
                    if (!searchBox.getText().toString().equals("")) searchBox.setText("");

                    hideKeyboard();

                    setDockbarState(DockbarState.STATE_HOME, true);

                    toggleAppsContainer(false);
                } catch (Exception e) { //May fail at boot due to uncreated UI
                    Log.d(TAG, "Exception: " + e);
                    Log.d(TAG, "Message: " + e.getMessage());
                }
            }
        });
    }

    public void hideKeyboard(){
        //Hide keyboard
        InputMethodManager imm = (InputMethodManager)
                HomeActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.bigTint.getWindowToken(), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CHOOSE_ICON && resultCode == RESULT_OK){
            String pkg = data.getStringExtra("r_pkg");
            String rName = data.getStringExtra("r_name");
            if(packageName == null || rName == null){
                packageName = this.getClass().getPackage().getName();
                resourceName = "ic_folder_black_48dp";
            } else {
                packageName = pkg;
                resourceName = rName;
            }

            Drawable d;
            try {
                int resId = getPackageManager().getResourcesForApplication(packageName)
                        .getIdentifier(rName, "drawable", packageName);
                if(resId == 0) throw new Exception("");
                d = getPackageManager().getResourcesForApplication(packageName).getDrawable(resId);
            } catch (Exception e) {
                d = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }

            if(dialogView != null) {
                ((ImageButton) dialogView.findViewById(R.id.chooseIcon)).setImageDrawable(d);
            }
        } else if (requestCode == REQUEST_CHOOSE_APPLICATION && resultCode == RESULT_OK){
            ActivityInfo activityInfo = data.resolveActivityInfo(getPackageManager(), 0);
            ApplicationIcon ai = new ApplicationIcon(activityInfo.packageName,
                    (String) activityInfo.loadLabel(getPackageManager()), activityInfo.name);

            //Store data in SharedPreferences
            Gson gson = new Gson();
            writer.putString(cachedPref, gson.toJson(ai)).apply();
        } else if (requestCode == REQUEST_ALLOCATE_ID || requestCode == REQUEST_PICK_APP_WIDGET) {
            if(resultCode == RESULT_OK) {
                handleWidgetConfig(data);
            } else if (data != null) {
                Toast.makeText(this, "You must allow us to manage widgets to use widgets.", Toast.LENGTH_LONG).show();
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    widgetHost.deleteAppWidgetId(appWidgetId);
                }
            }
        } else if (requestCode == REQUEST_CONFIG_WIDGET) {
            if(resultCode == RESULT_OK) {
                addWidget(data);
                persistWidgets(widgets);
            } else if (resultCode == RESULT_CANCELED && data != null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    widgetHost.deleteAppWidgetId(appWidgetId);
                }
            }
        }
    }

    public void handleWidgetConfig(Intent data){
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId);
        handleWidgetConfig(appWidgetId, appWidgetInfo);
    }

    public void handleWidgetConfig(int appWidgetId, AppWidgetProviderInfo awpi){ //Only occurs on first add
        AppWidgetProviderInfo appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId);
        if (awpi.configure != null) { //Must configure the widget
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CONFIG_WIDGET);
        } else {
            addWidget(appWidgetId, awpi);
            persistWidgets(widgets);
        }
    }

    public void addWidget(Intent data){
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId);
        addWidget(appWidgetId, appWidgetInfo);
    }

    public void addWidget(int appWidgetId, int height){
        try {
            addWidget(appWidgetId, widgetManager.getAppWidgetInfo(appWidgetId), height);
        } catch (Exception e) {
            Log.d(TAG, "Failed to add widget; removing from database...");

            WidgetContainer wc = findContainer(appWidgetId);
            if (wc != null) widgets.remove(wc);
        }
    }

    public void addWidget(int appWidgetId, AppWidgetProviderInfo awpi){
        addWidget(appWidgetId, awpi, -1);
    }

    /**
     * Add a widget to the widget container. Also handles adding it to the list used to
     * keep track of widgets.
     *
     * @param appWidgetId The ID for the widget allocated by the system.
     * @param awpi The AppWidgetProviderInfo passed to us by the system.
     * @param defaultHeight The height we've stored for the widget. If -1, we're adding a widget for the first time.
     */
    public void addWidget(final int appWidgetId, final AppWidgetProviderInfo awpi, int defaultHeight){
        final AppWidgetHostView hostView = widgetHost.createView(this, appWidgetId, awpi);
        hostView.setAppWidget(appWidgetId, awpi);

        //Set to minHeight
        int resultDp;
        int width = (int) Utilities.convertPixelsToDp(widgetOverlayContainer.getWidth(), this);

        if(defaultHeight == -1){
            resultDp = awpi.minHeight;
        } else { //Use
            resultDp = defaultHeight;
        }
        int resultPx = (int) Utilities.convertDpToPixel(resultDp, this);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            hostView.updateAppWidgetSize(null, width, resultDp, width, resultDp);
        }

        hostView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, resultPx));

        widgetOverlayContainer.addView(hostView); //Add the HostView directly to the linear layout

        //Add it to the widget list
        widgets.add(new WidgetContainer(awpi.provider.getPackageName(), appWidgetId, resultDp));
        if(defaultHeight == -1){ //Show if a "new" widget
            showWidget(awpi.provider.getPackageName());
        }
    }

    private WidgetContainer findContainer(int widgetId){
        for(WidgetContainer wc : widgets){
            if(wc.getWidgetId() == widgetId) return wc;
        }
        return null;
    }

    private void showResizeDialog(final int appWidgetId, final AppWidgetHostView awhv){
        AppWidgetProviderInfo awpi = widgetManager.getAppWidgetInfo(appWidgetId);

        int widgetIndex = -1;
        for(int i = 0; i < widgets.size(); i++){
            if(widgets.get(i).getWidgetId() == appWidgetId) widgetIndex = i;
        }

        try {
            int shouldNotFail = awpi.minResizeHeight;
        } catch (Exception e) { //Error condition; find widget and remove
            if(widgetIndex != -1){
                widgets.remove(widgetIndex);
                widgetOverlayContainer.removeViewAt(widgetIndex);
                persistWidgets(widgets);
            }

            Toast.makeText(this, R.string.widget_no_longer_installed, Toast.LENGTH_SHORT).show();
            return;
        }

        //We know we can resize vertically; get bounds
        int tempMinHeight = awpi.minHeight;
        if(awpi.resizeMode == AppWidgetProviderInfo.RESIZE_VERTICAL){
            tempMinHeight = awpi.minResizeHeight < awpi.minHeight ? awpi.minResizeHeight : awpi.minHeight;
        }
        final int minHeightDp = tempMinHeight;

        final int maxHeightDp = (int) Utilities.convertPixelsToDp((float) (bigTint.getHeight() * 0.7), this);
        final int widthDp = (int) Utilities.convertPixelsToDp(widgetOverlayContainer.getWidth(), this);

        View v = getLayoutInflater().inflate(R.layout.widget_resize, null);
        final SeekBar sb = (SeekBar) v.findViewById(R.id.heightBar);

        //Set SeekBar to the current size
        int currentSize = awhv.getHeight();

        float minInPixels = Utilities.convertDpToPixel(minHeightDp, this);
        float maxInPixels = Utilities.convertDpToPixel(maxHeightDp, this);
        float slideDistance = maxInPixels - minInPixels;

        if(slideDistance == 0) slideDistance = 1; // /0 = bad; shouldn't happen, buuuut...

        float percent = ((currentSize - minInPixels) / slideDistance);
        sb.setProgress((int) (sb.getMax() * percent));

        MaterialDialog md = new MaterialDialog.Builder(this)
                .title("Resize " + awhv.getAppWidgetInfo().label)
                .positiveText("Resize")
                .customView(v, false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        int position = sb.getProgress();
                        int max = sb.getMax();

                        Log.d(TAG, "Position/Max: " + position + " and " + max);
                        if (max == 0) max = 1;

                        float percentSlid = (float) position / (float) max;
                        Log.d(TAG, "Percent slid: " + percentSlid);

                        float range = maxHeightDp - minHeightDp;
                        int result = (int) (percentSlid * range);
                        result += minHeightDp;
                        Log.d(TAG, "Range: " + range + " and result: " + result);

                        int resultPx = (int) Utilities.convertDpToPixel((float) result, dialog.getContext());
                        Log.d(TAG, "Result pixels: " + resultPx);

                        awhv.updateAppWidgetSize(null, widthDp, result, widthDp, result);
                        awhv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, resultPx));

                        //Update widget container
                        WidgetContainer wc = findContainer(appWidgetId);
                        if (wc != null) {
                            wc.setWidgetHeight(result);
                        }

                        persistWidgets(widgets);
                    }
                }).build();
        md.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(timer != null){
            timer.cancel();
        }
    }

    public void fadeDateTime(float to, long duration){
        //Log.d(TAG, "Fade date time called with: " + to + " and " + duration);
        if(duration < 0l){ //Apply immediately
            timeDateContainer.setAlpha(to);
            dockbarApps.setAlpha(to);
            bigTint.setAlpha(to);
        } else {
            ObjectAnimator.ofFloat(timeDateContainer, "alpha", to)
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(dockbarApps, "alpha", to)
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(bigTint, "alpha", 1 - to)
                    .setDuration(duration)
                    .start();
        }
    }

    public void handleDedicatedAppButton(String pref, boolean slideTransition){
        cachedPref = pref;
        String existingData = reader.getString(pref, "null");
        if(!existingData.equals("null")){
            Log.d(TAG, "Existing data: " + existingData);
            Gson gson = new Gson();
            try {
                ApplicationIcon ai = gson.fromJson(existingData, ApplicationIcon.class);
                try {
                    Intent appLaunch = new Intent();
                    appLaunch.setClassName(ai.getPackageName(), ai.getActivityName());
                    appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if(slideTransition && Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                        startActivity(appLaunch, ActivityOptions.makeCustomAnimation(this,
                                R.anim.search_slide_anim, R.anim.slide_out_right_anim).toBundle());
                    } else {
                        startActivity(appLaunch);
                    }
                } catch (Exception e) { //Show launcher!
                    Utilities.grabActivity(this, HomeActivity.REQUEST_CHOOSE_APPLICATION);
                }
            } catch (Exception ignored) {
                Utilities.grabActivity(this, HomeActivity.REQUEST_CHOOSE_APPLICATION);
            }
        } else {
            Toast.makeText(this,
                    "Choose an application to launch when you perform this action.", Toast.LENGTH_SHORT).show();
            Utilities.grabActivity(this, HomeActivity.REQUEST_CHOOSE_APPLICATION);
        }
    }

    public void toggleAppsContainer(boolean visible){
        //Do the popup for usage requests if needed
        if(visible && !reader.getBoolean(Constants.HAS_REQUESTED_USAGE_PERMISSION_PREF, false) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                @SuppressWarnings("ResourceType")
                final UsageStatsManager usm = (UsageStatsManager) HomeActivity.this.getSystemService("usagestats");

                long end = 0l;
                long start = System.currentTimeMillis() ;

                UsageEvents use = usm.queryEvents(start, end);
                UsageEvents.Event event = new UsageEvents.Event();
                if(use.hasNextEvent() && use.getNextEvent(event)) {
                    Log.d(TAG, "Found usage events...");
                } else {
                    showUsageMessage(); //We assume this means a problem..?
                }
            } catch (Exception notAllowed) {
                showUsageMessage();
            }
        }

        //Show all apps RecyclerView
        allAppsContainer.setVisibility(View.VISIBLE);

        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        if(visible){
            ObjectAnimator oa = ObjectAnimator.ofFloat(allAppsContainer, "translationY", 0);
            oa.setDuration(500);
            oa.start();

            strayTouchCatch.setVisibility(View.VISIBLE);
        } else {
            if(allAppsContainer.getTranslationX() != 0) return; //Might be animating; let it do its thing
            ObjectAnimator oa = ObjectAnimator.ofFloat(allAppsContainer, "translationY", dm.heightPixels);
            oa.setDuration(500);
            oa.start();

            strayTouchCatch.setVisibility(View.GONE);
        }
    }

    public void resetAppsList(final String query, final boolean preCacheIcons){
        IconCache.getInstance().cancelPendingIconTasks();

        if(appsSearch != null && !appsSearch.isCancelled() && appsSearch.getStatus() == AsyncTask.Status.FINISHED){
            try {
                appsSearch.cancel(true);
            } catch (Exception ignored) {}
        }

        appsSearch = new AsyncTask<Object, Void, List<ApplicationIcon>>(){
            @Override
            protected List<ApplicationIcon> doInBackground(Object... params) {
                PackageManager pm = (PackageManager) params[0];
                List<ApplicationIcon> applicationIcons = new ArrayList<ApplicationIcon>();

                //Grab all matching applications
                final Intent allAppsIntent = new Intent(Intent.ACTION_MAIN, null);
                allAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                final List<ResolveInfo> packageList = pm.queryIntentActivities(allAppsIntent, 0);
                String lowerQuery = query.toLowerCase(Locale.getDefault());
                for(ResolveInfo ri : packageList){
                    try {
                        String name = (String) ri.loadLabel(pm);
                        if(lowerQuery.equals("") || name.toLowerCase(Locale.getDefault())
                            .contains(lowerQuery)) {
                            if (!hiddenApps.contains(new Pair<>(ri.activityInfo.packageName,
                                    ri.activityInfo.name))) {
                                applicationIcons.add(new ApplicationIcon(ri.activityInfo.packageName,
                                        name, ri.activityInfo.name));
                            }
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
            }

            @Override
            protected void onPostExecute(List<ApplicationIcon> apps){
                if(apps != null) {
                    //Compare old "apps" with cached apps -- if the same, don't reset
                    int newHash = apps.hashCode();
                    if(newHash != cachedHash) {
                        ApplicationIconAdapter adapter = new ApplicationIconAdapter(apps, HomeActivity.this);
                        adapter.setHasStableIds(true);
                        allAppsScreen.setAdapter(adapter);

                        if(preCacheIcons && reader.getBoolean(Constants.AGGRESIVE_CACHING_PREF, true)){
                            for(ApplicationIcon ai : apps){
                                IconCache.getInstance().setIcon(ai.getPackageName(), ai.getActivityName(), null);
                            }
                        }

                        FastScroller scroller = new FastScroller(allAppsScreen, scrollerBar, startLetter,
                                endLetter, popup);

                        scroller.setupList((List)apps);
                        scroller.setupScrollbar();

                        cachedHash = newHash;
                    }
                }
            }
        };

        try {
            //Default SERIAL_EXECUTOR will hang, but sometimes we have to use it
            appsSearch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.getPackageManager());
        } catch (RejectedExecutionException tooManyOnThreadPool) {
            appsSearch.execute(this.getPackageManager());
        }

        clearSearch.setVisibility(query == null || query.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadList(List<TypeCard> samples){
        Log.d(TAG, "Loading list...");

        loadRows: {
            Cursor loadItems =
                    db.query(DatabaseHelper.TABLE_ROWS, null, null, null, null, null, null);
            if (loadItems.moveToFirst()) {
                Log.d(TAG, "Found: " + loadItems.getCount());

                int dataColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_DATA);
                int graphicColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_GRAPHIC);
                int graphicPackageColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_GRAPHIC_PACKAGE);
                int titleColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_TITLE);
                if (dataColumn == -1 || graphicColumn == -1 || graphicPackageColumn == -1 || titleColumn == -1) {
                    loadItems.close();
                    break loadRows;
                }

                while (!loadItems.isAfterLast()) {
                    String data = loadItems.getString(dataColumn);
                    String graphicPackage = loadItems.getString(graphicPackageColumn);
                    String title = loadItems.getString(titleColumn);
                    String graphic = loadItems.getString(graphicColumn);

                    Log.d(TAG, "Loading '" + data + "'...");

                    //Generate package/activity pairs by splitting data
                    if(data.startsWith("<!--APPTYPE-->")){ //This is just one app, not a full row
                        String appName = data.replace("<!--APPTYPE-->", "");
                        String[] split = appName.split("\\|");
                        TypeCard tc = new TypeCard(new Pair<>(split[0], split[1]));
                        samples.add(tc);
                    } else { //A full row type (this is the default)
                        List<Pair<String, String>> paPairs = new ArrayList<>();
                        String[] pairs = data.split(",");
                        Log.d(TAG, pairs.length + " elements in this");
                        for (int i = 0; i < pairs.length; i++) {
                            String[] packAndAct = pairs[i].split("\\|");
                            paPairs.add(new Pair<>(packAndAct[0], packAndAct[1]));
                        }
                        TypeCard tc = new TypeCard(title, graphicPackage, graphic, paPairs);
                        samples.add(tc);
                    }
                    loadItems.moveToNext();
                }
            } else {
                Log.d(TAG, "No rows found!");
            }
            loadItems.close();
        }

        sgv.setCards(samples);
    }

    private void loadWidgets(List<WidgetContainer> widgets) {
        Log.d(TAG, "Loading widgets...");

        loadRows: {
            Cursor loadItems =
                    db.query(DatabaseHelper.TABLE_WIDGETS, null, null, null, null, null,
                            DatabaseHelper.COLUMN_POSITION + " asc");
            if (loadItems.moveToFirst()) {
                Log.d(TAG, "Found: " + loadItems.getCount());

                int idColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_WIDGET_ID);
                int sizeColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_SIZE_DP);
                if (idColumn == -1 || sizeColumn == -1) {
                    loadItems.close();
                    break loadRows;
                }

                while (!loadItems.isAfterLast()) {
                    int widgetId = loadItems.getInt(idColumn);
                    int size = loadItems.getInt(sizeColumn);

                    Log.d(TAG, "Loading '" + widgetId + "'...");
                    addWidget(widgetId, size);

                    loadItems.moveToNext();
                }
            } else {
                Log.d(TAG, "No widgets found!");
            }
            loadItems.close();
        }
    }

    /**
     * Saves cards to the database.
     * @param cards Cards to save.
     */
    public void persistList(List<TypeCard> cards){
        Log.d(TAG, "Persisting at most " + cards.size() + " cards");
        db.delete(DatabaseHelper.TABLE_ROWS, null, null);
        for(int i = 0; i < cards.size(); i++){
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COLUMN_GRAPHIC, cards.get(i).getDrawableName());
            cv.put(DatabaseHelper.COLUMN_GRAPHIC_PACKAGE, cards.get(i).getDrawablePackage());
            cv.put(DatabaseHelper.COLUMN_TITLE, cards.get(i).getTitle());
            cv.put(DatabaseHelper.COLUMN_ORDER, i);
            String data = "";
            List<Pair<String, String>> pkgList = cards.get(i).getPackages();
            for (Pair<String, String> pkgs : pkgList) {
                data += pkgs.first + "|" + pkgs.second + ",";
            }
            data = data.substring(0, data.length() - 1);
            cv.put(DatabaseHelper.COLUMN_DATA, data);

            Log.d(TAG, "Trying to insert: " + cv);

            long result = db.insert(DatabaseHelper.TABLE_ROWS, null, cv);

            Log.d(TAG, result == -1 ? "Error inserting: " + i :
                    "Inserted row: " + i + " at " + result);
        }
    }

    /** Save widgets to the database.
     *
     * @param widgets Widgets to save.
     */
    public void persistWidgets(List<WidgetContainer> widgets){
        Log.d(TAG, "Persisting at most " + widgets.size() + " widgets");
        db.delete(DatabaseHelper.TABLE_WIDGETS, null, null);
        for(int i = 0; i < widgets.size(); i++){
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COLUMN_WIDGET_ID, widgets.get(i).getWidgetId());
            cv.put(DatabaseHelper.COLUMN_SIZE_DP, widgets.get(i).getWidgetHeight());
            cv.put(DatabaseHelper.COLUMN_POSITION, i);

            Log.d(TAG, "Trying to insert: " + cv);

            long result = db.insert(DatabaseHelper.TABLE_WIDGETS, null, cv);

            Log.d(TAG, result == -1 ? "Error inserting: " + i :
                    "Inserted widget: " + i + " at " + result);
        }
    }

    public void setDockbarState(final DockbarState states, boolean animate){
        dockbarApps.setVisibility(View.VISIBLE);
        dropLayout.setVisibility(View.VISIBLE);
        searchActionBar.setVisibility(View.VISIBLE);

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        if(!animate) {
            finalizeSwitch(dm, states);
        } else {
            View leavingView = null;
            View enteringView = null;
            int deltaX = 0;
            if(currentState == DockbarState.STATE_HOME){
                leavingView = dockbarApps;
                if(states == DockbarState.STATE_DROP){
                    dropLayout.setTranslationX(-dm.widthPixels);
                    deltaX = dm.widthPixels;
                    enteringView = dropLayout;
                } else if (states == DockbarState.STATE_APPS){
                    searchActionBar.setTranslationX(dm.widthPixels);
                    deltaX = -dm.widthPixels;
                    enteringView = searchActionBar;
                }
            } else if (currentState == DockbarState.STATE_DROP){
                //Can only go to home now
                leavingView = dropLayout;
                enteringView = dockbarApps;
                deltaX = -dm.widthPixels;
                dockbarApps.setTranslationX(dm.widthPixels);
            } else if (currentState == DockbarState.STATE_APPS){
                leavingView = searchActionBar;
                if(states == DockbarState.STATE_DROP){
                    deltaX = -dm.widthPixels;
                    dropLayout.setTranslationX(dm.widthPixels);
                    enteringView = dropLayout;
                } else if (states == DockbarState.STATE_HOME){
                    deltaX = dm.widthPixels;
                    dockbarApps.setTranslationX(-dm.widthPixels);
                    enteringView = dockbarApps;
                }
            }

            if(enteringView == null || leavingView == null) return;

            float start = enteringView.getTranslationX();
            ObjectAnimator enteringAnim = ObjectAnimator.ofFloat(enteringView, "translationX",
                    start, 0);
            enteringAnim.setDuration(400);
            enteringAnim.start();

            ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(leavingView, "translationX",
                    deltaX);
            leavingAnim.setDuration(400);
            leavingAnim.start();

            if(states == DockbarState.STATE_HOME){
                fadeDateTime(1, 400);
            } else {
                fadeDateTime(0, 400);
            }
        }
        currentState = states;
    }

    private void finalizeSwitch(DisplayMetrics dm, DockbarState states){
        switch (states) {
            case STATE_APPS:
                dockbarApps.setTranslationX(dm.widthPixels);
                dropLayout.setTranslationX(dm.widthPixels);
                searchActionBar.setTranslationX(0);
                break;
            case STATE_DROP:
                dockbarApps.setTranslationX(dm.widthPixels);
                dropLayout.setTranslationX(0);
                searchActionBar.setTranslationX(dm.widthPixels);
                break;
            case STATE_HOME:
                dockbarApps.setTranslationX(0);
                dropLayout.setTranslationX(dm.widthPixels);
                searchActionBar.setTranslationX(dm.widthPixels);
                break;
        }
    }

    public void setupDockbarTarget(final ImageView dockbarTarget, final int place){
        /*
        //Size the target properly
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dockbarTarget.getLayoutParams();
        int properSize = (int) Utilities.convertDpToPixel(56, this);
        if(size == ScreenSize.SMALL_TABLET){
            properSize = (int) Utilities.convertDpToPixel(64, this);
        } else if (size == ScreenSize.LARGE_TABLET){
            properSize = (int) Utilities.convertDpToPixel(72, this);
        }
        params.width = properSize;
        params.height = properSize;
        dockbarTarget.setLayoutParams(params);
        */

        dockbarTarget.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                if(event.getAction() == DragEvent.ACTION_DROP){
                    Log.d(TAG, "Drop occured!");

                    final ApplicationIcon ai = (ApplicationIcon) event.getLocalState();
                    initDockbar(ai, dockbarTarget, place);
                    //Store data in SharedPreferences
                    Gson gson = new Gson();
                    writer.putString("dockbarTarget_" + place, gson.toJson(ai)).apply();
                } else if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                    String existingData = reader.getString("dockbarTarget_" + place, "null");
                    if(existingData.equals("null")){ //Show (+) icon on target
                        dockbarTarget.setImageResource(R.drawable.ic_add_circle_outline_white_48dp);
                    }
                } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    //If we still have no data, set null
                    String existingData = reader.getString("dockbarTarget_" + place, "null");
                    if(existingData.equals("null")){ //Show (+) icon on target
                        dockbarTarget.setImageDrawable(null);
                    }
                }
                return true;
            }
        });

        String existingData = reader.getString("dockbarTarget_" + place, "null");
        if(!existingData.equals("null")){
            Gson gson = new Gson();
            try {
                ApplicationIcon ai = gson.fromJson(existingData, ApplicationIcon.class);
                initDockbar(ai, dockbarTarget, place);
            } catch (Exception e) {
            }
        }
    }

    public void showCreateFolderDialog(final ApplicationIcon ai){
        packageName = "null";
        resourceName = "null";

        dialogView = getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
        dialogView.findViewById(R.id.chooseIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), IconChooserActivity.class);
                startActivityForResult(i, REQUEST_CHOOSE_ICON);
            }
        });
        final EditText et = (EditText) dialogView.findViewById(R.id.chooseName);
        new MaterialDialog.Builder(this)
                .customView(dialogView, true)
                .title("Add Folder")
                .positiveText("Save")
                .negativeText("Cancel")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                    }

                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        String name = et.getText().toString();
                        if(name == null || name.length() == 0){
                            name = "Folder";
                        }
                        if(packageName.equals("null")){
                            packageName = this.getClass().getPackage().getName();
                            resourceName = "ic_folder_white_48dp";
                        }
                        List<Pair<String, String>> apps = new ArrayList<Pair<String, String>>();
                        apps.add(new Pair<>(ai.getPackageName(), ai.getActivityName()));
                        sgv.data.add(new TypeCard(name, packageName, resourceName, apps));
                        persistList(samples);
                        dialog.dismiss();
                    }
                }).show();
    }

    public void showEditFolderDialog(final int row){
        if(row >= 0 && row < sgv.data.size()){
            packageName = sgv.data.get(row).getDrawablePackage();
            resourceName = sgv.data.get(row).getDrawableName();

            dialogView = getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
            dialogView.findViewById(R.id.chooseIcon).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(v.getContext(), IconChooserActivity.class);
                    startActivityForResult(i, REQUEST_CHOOSE_ICON);
                }
            });

            Drawable d;
            try {
                int resId = getPackageManager().getResourcesForApplication(packageName)
                        .getIdentifier(resourceName, "drawable", packageName);
                if(resId == 0) throw new Exception("");
                d = getPackageManager().getResourcesForApplication(packageName).getDrawable(resId);
            } catch (Exception e) {
                d = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }

            ((ImageButton) dialogView.findViewById(R.id.chooseIcon)).setImageDrawable(d);
            final EditText et = (EditText) dialogView.findViewById(R.id.chooseName);
            et.setText(sgv.data.get(row).getTitle());
            new MaterialDialog.Builder(this)
                    .customView(dialogView, false)
                    .title(R.string.edit_folder)
                    .positiveText(R.string.save)
                    .neutralText(R.string.edit_items)
                    .negativeText(R.string.delete)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onNeutral(MaterialDialog dialog) {
                            showReorderDialog(row);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            sgv.data.remove(row);
                            persistList(samples);
                            sgv.notifyShortcutsChanged();
                        }

                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            String name = et.getText().toString();
                            if(name == null || name.length() == 0){
                                name = "Folder";
                            }

                            if(packageName.equals("null")){
                                packageName = this.getClass().getPackage().getName();
                                resourceName = "ic_folder_white_48dp";
                            }
                            sgv.data.get(row).setTitle(name);
                            sgv.data.get(row).setDrawable(resourceName, packageName);

                            persistList(samples);
                            sgv.notifyShortcutsChanged();
                        }
                    }).show();
        }
    }

    public void showReorderDialog(final int row){
        depthThreeDialogRaised = false;

        DragSortListView dsiv = (DragSortListView) LayoutInflater.from(this).inflate(R.layout.sort_list, null);
        dsiv.setAdapter(new AppEditAdapter(this,
                R.layout.app_edit_row, sgv.data.get(row).getPackages()));
        dsiv.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        new MaterialDialog.Builder(this)
                .customView(dsiv, false)
                .title(R.string.edit_items)
                .positiveText(R.string.save)
                .neutralText(R.string.help)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(!depthThreeDialogRaised) showEditFolderDialog(row);
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        persistList(samples);
                        sgv.notifyShortcutsChanged();
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        depthThreeDialogRaised = true;
                        new MaterialDialog.Builder(HomeActivity.this)
                                .title(R.string.help)
                                .positiveText(R.string.got_it)
                                .dismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        showReorderDialog(row); //The help dialog dismisses the main dialog, weirdly enough
                                    }
                                })
                                .content(R.string.edit_items_help)
                                .show();
                    }
                })
                .show();
    }

    public void initDockbar(final ApplicationIcon ai, final ImageView dockbarTarget, final int location){
        dockbarTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent appLaunch = new Intent();
                    appLaunch.setClassName(ai.getPackageName(), ai.getActivityName());
                    appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    v.getContext().startActivity(appLaunch);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Couldn't start this app!", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        //Long-click to clear
        dockbarTarget.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new MaterialDialog.Builder(v.getContext())
                        .title(R.string.remove_dock_item)
                        .content(R.string.remove_dock_item_message)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                dockbarTarget.setOnClickListener(null);
                                dockbarTarget.setOnLongClickListener(null);
                                dockbarTarget.setImageDrawable(null);

                                //Remove stored data
                                writer.putString("dockbarTarget_" + location, "null").apply();
                            }
                        }).show();
                return true;
            }
        });

        try {
            Drawable icon = getPackageManager()
                    .getActivityIcon(new ComponentName(ai.getPackageName(), ai.getActivityName()));
            dockbarTarget.setImageDrawable(icon);
        } catch (Exception e) {
            dockbarTarget.setImageDrawable(getResources().getDrawable(android.R.drawable.sym_def_app_icon));
        }
    }

    //Update clock
    void updateDisplay(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Calendar cal = GregorianCalendar.getInstance();

                hour.setText(hours.format(cal.getTime()));
                minute.setText(minutes.format(cal.getTime()));

                //Set date
                SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", Locale.US);
                SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM", Locale.US);
                SimpleDateFormat sdfDayOfMonth = new SimpleDateFormat("d", Locale.US);
                //Check battery -- if charging, then add percent to left of clock

                boolean hasPhonePermission = Utilities.checkPermission(Manifest.permission.READ_PHONE_STATE, HomeActivity.this);
                if (hasPhonePermission) {
                    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent status = registerReceiver(null, filter);
                    if (status != null) {
                        if (status.getIntExtra(BatteryManager.EXTRA_STATUS,
                                BatteryManager.BATTERY_STATUS_CHARGING) == BatteryManager.BATTERY_STATUS_CHARGING) {
                            date.setText("Charging " + status.getIntExtra(BatteryManager.EXTRA_LEVEL, 50) + "%\n"
                                    + sdfDay.format(cal.getTime()) + ", " + sdfMonth.format(cal.getTime())
                                    + " " + sdfDayOfMonth.format(cal.getTime()));
                        } else {
                            date.setText(sdfDay.format(cal.getTime())
                                    + ", " + sdfMonth.format(cal.getTime())
                                    + " " + sdfDayOfMonth.format(cal.getTime()));
                        }
                    }

                    //Check if there's an alarm set
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                        final AlarmManager.AlarmClockInfo aci = am.getNextAlarmClock();

                        if (aci != null && (aci.getTriggerTime() - System.currentTimeMillis() < ONE_DAY_MILLIS)) {
                            alarm.setVisibility(View.VISIBLE);
                            alarm.setText(getString(R.string.next_alarm_at) + " " + alarmTime.format(new Date(aci.getTriggerTime())));
                            alarm.setOnClickListener(new View.OnClickListener() {
                                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                                @Override
                                public void onClick(View v) {
                                    try {
                                        aci.getShowIntent().send();
                                    } catch (Exception ignored) {
                                    }
                                }
                            });
                        } else {
                            alarm.setVisibility(View.GONE);
                        }
                    }
                }
            }
        };

        if(Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            this.runOnUiThread(runnable);
        }
    }
}
