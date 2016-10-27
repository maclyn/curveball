package com.inipage.homelylauncher;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Application;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
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
import com.inipage.homelylauncher.drawer.StickyImageView;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.icons.IconChooserActivity;
import com.inipage.homelylauncher.swiper.AppEditAdapter;
import com.inipage.homelylauncher.swiper.RowEditAdapter;
import com.inipage.homelylauncher.utils.Utilities;
import com.inipage.homelylauncher.views.DockElement;
import com.inipage.homelylauncher.views.DockView;
import com.inipage.homelylauncher.views.DockViewHost;
import com.inipage.homelylauncher.views.DragToOpenView;
import com.inipage.homelylauncher.views.ShortcutGestureView;
import com.inipage.homelylauncher.views.ShortcutGestureViewHost;
import com.inipage.homelylauncher.widgets.WidgetAddAdapter;
import com.mobeta.android.dslv.DragSortListView;

import org.apache.commons.collections4.trie.PatriciaTrie;

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
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

@SuppressWarnings("unchecked")
public class HomeActivity extends Activity implements ShortcutGestureViewHost {
    //region Constants
    public static final String TAG = "HomeActivity";

    public static final int REQUEST_CHOOSE_ICON = 200;
    public static final int REQUEST_CHOOSE_APPLICATION = 201;
    public static final int REQUEST_ALLOCATE_ID = 202;
    private static final int REQUEST_CONFIG_WIDGET = 203;
    private static final int REQUEST_PICK_APP_WIDGET = 204;
    private static final int REQUEST_PERMISSIONS = 205;

    public static final int HOST_ID = 505;
    private static final long ONE_DAY_MILLIS = 1000 * 60 * 60 * 24;
    private static final long DEFAULT_ANIMATION_DURATION = 300;
    //endregion

    //region Enums
    public enum ScreenSize {
        PHONE, SMALL_TABLET, LARGE_TABLET
    }
    //endregion

    //region Fields
    //Dock states
    ScreenSize size;

    //To make sure we don't repopulate suggestions when just hitting the home button
    long lastPauseTime = -1;

    //Apps stuff
    @Bind(R.id.allAppsContainer)
    RelativeLayout allAppsContainer;
    @Bind(R.id.allAppsLayout)
    RecyclerView allAppsScreen;
    @Bind(R.id.allAppsMessageLayout)
    View allAppsMessageLayout;
    @Bind(R.id.allAppsMessage)
    TextView allAppsMessage;
    @Bind(R.id.allAppsSearchGoogle)
    Button allAppsSearchGoogle;
    @Bind(R.id.scrollerContainer)
    View scrollerBar;
    @Bind(R.id.startLetter)
    TextView startLetter;
    @Bind(R.id.endLetter)
    TextView endLetter;
    @Bind(R.id.popup)
    TextView popup;

    //Dockbar background
    @Bind(R.id.dockBar)
    DragToOpenView dockBar;

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
    RelativeLayout dockbarApps;
    @Bind(R.id.dockView)
    DockView dockView;

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
    FrameLayout dropLayout;

    //App drop layout
    @Bind(R.id.appDropIcons)
    View appDropLayout;
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

    //Widgets
    @Bind(R.id.widget_drawer)
    View widgetDrawer;
    @Bind(R.id.widget_delete)
    View widgetDelete;
    @Bind(R.id.widget_collapse)
    View widgetCollapse;
    @Bind(R.id.widget_container)
    FrameLayout widgetContainer;

    private HashMap<String, Boolean> hasWidgetMap = new HashMap<>();
    private boolean widgetsDefrosted = false;
    List<Pair<Integer, AppWidgetProviderInfo>> widgetIdsWithInfo = new ArrayList<>();
    int currentWidget = -1;
    boolean showingWidget = false;

    //Suggestions
    @Bind(R.id.suggestionsBox)
    View suggestionsRootView;

    @Bind(R.id.loading_suggestions)
    TextView loadingSuggestions;

    @Bind(R.id.no_suggestions_right_now)
    TextView noSuggestions;

    @Bind(R.id.suggestionsScrollView)
    HorizontalScrollView suggestionsSV;

    @Bind(R.id.suggestionsLayout)
    LinearLayout suggestionsLayout;

    AppWidgetManager widgetManager;
    AppWidgetHost widgetHost;

    //Settings
    SharedPreferences reader;
    SharedPreferences.Editor writer;

    Configuration previousConfiguration;

    List<TypeCard> samples;
    List<Pair<String, Integer>> smartApps;
    List<Pair<String, String>> hiddenApps;

    View dialogView;
    String packageName;
    String resourceName;
    String cachedPref;

    /**
     * Some dialogs have onDismiss(...) listeners that open their 'parent' dialogs. This variable
     * lets us know when we intentionally raise a child dialog of depth two, so as not to trigger
     * the onDismiss(...) showing of a depth 1 dialog from a child dialog of depth 2. Yes,
     * this is a gimmicky hack until we have better dialog management.
     */
    boolean depthThreeDialogRaised = false;

    //Tell when things have changed
    BroadcastReceiver packageReceiver;
    BroadcastReceiver storageReceiver;
    //endregion

    //region Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        Utilities.logEvent(Utilities.LogLevel.STATE_CHANGE, "onCreate HomeActivity");

        setContentView(R.layout.activity_home);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        ButterKnife.bind(this);

        if (Utilities.isSmallTablet(this)) {
            size = ScreenSize.SMALL_TABLET;
        } else if (Utilities.isLargeTablet(this)) {
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
            Log.e(TAG, "Error opening DB ", e);
            Toast.makeText(this, "Failed to open database!", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }
        reader = PreferenceManager.getDefaultSharedPreferences(this);
        writer = reader.edit();

        previousConfiguration = getResources().getConfiguration();

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
        smartApps = new ArrayList<>();
        hiddenApps = new ArrayList<>();

        //Allow rotation if requested or a tablet
        if ((Utilities.isSmallTablet(this) || Utilities.isLargeTablet(this)) || reader.getBoolean(Constants.ALLOW_ROTATION_PREF, false)) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }

        //Set up dockbar apps
        dockBar.setOnDragToOpenListener(new DragToOpenView.OnDragToOpenListener() {
            boolean hasHiddenClock = false;

            @Override
            public void onDragStarted() {
                allAppsContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDragChanged(float distance) {
                dockBar.setTranslationY(-distance);
                float translationToSet = (allAppsContainer.getHeight()) - distance;
                if (translationToSet < 0) {
                    allAppsContainer.setTranslationY(0);
                } else {
                    float threshold = sgv.getHeight() / 2;

                    if (!hasHiddenClock && (Math.abs(distance) > threshold)) {
                        hideDateTime();
                        hasHiddenClock = true;
                    }

                    allAppsContainer.setTranslationY(translationToSet);
                }

                if(distance < 0){
                    distance = 0;
                }
                bigTint.setAlpha(distance / (float) allAppsContainer.getHeight());
            }

            @Override
            public void onDragCompleted(boolean dragAccepted, float finalDistance, float finalVelocity) {
                if (dragAccepted) {
                    if (!hasHiddenClock) hideDateTime();
                } else {
                    if (hasHiddenClock) showDateTime();
                }
                toggleAppsContainer(dragAccepted, finalVelocity);
                hasHiddenClock = false;
            }
        });

        List<DockElement> de = new ArrayList<>();
        int elementCount;
        if (size == ScreenSize.PHONE) {
            Log.d(TAG, "Is phone");
            elementCount = 5;
        } else { //Two extra spots for tablets
            Log.d(TAG, "Is tablet");
            elementCount = 7;
        }

        for(int i = 0; i < elementCount; i++){
            String existingData = reader.getString("dockbarTarget_" + i, "null");
            Gson gson = new Gson();
            try {
                ApplicationIcon ai = gson.fromJson(existingData, ApplicationIcon.class);
                ComponentName cn = new ComponentName(ai.getPackageName(), ai.getActivityName());
                String label = getPackageManager().getActivityInfo(cn, 0).loadLabel(getPackageManager()).toString();

                dockbarTargetsMap.put(i, ai);
                de.add(new DockElement(cn, label, i));
            } catch (Exception fail) {
            }
        }

        dockView.init(elementCount, de, new DockViewHost() {
            @Override
            public void onElementRemoved(DockElement oldElement, int index) {
                Log.d(TAG, "Clearing dock element at " + index);
                dockbarTargetsMap.remove(index);
                writer.putString(" dockbarTarget_" + index, "null").apply();

                populateSuggestions();
            }

            @Override
            public void onElementReplaced(DockElement oldElement, DockElement newElement, int index) {
                final ApplicationIcon ai = new ApplicationIcon(newElement.getActivity().getPackageName(), newElement.getTitle(), newElement.getActivity().getClassName());
                Gson gson = new Gson();
                String repl = gson.toJson(ai);
                Log.d(TAG, "Replacing dock element at " + index + " with " + repl);
                writer.putString("dockbarTarget_" + index, repl).apply();
                dockbarTargetsMap.put(index, ai);

                populateSuggestions();
            }
        });

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
                resetState();
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
                            case R.id.manageSuggestions:
                                showManageSuggestionsMenu();
                                break;
                            case R.id.help:
                                showTutorial();
                                break;
                            case R.id.settings:
                                Intent settingsActivity = new Intent(HomeActivity.this, SettingsActivity.class);
                                settingsActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(settingsActivity);
                                break;
                            case R.id.changeWallpaper:
                                toggleAppsContainer(false);
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
                quitSearch();
            }
        });
        allAppsSearchGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = searchBox.getText().toString();
                String uri = "market://search?q=" + text;

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(uri));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException anfe) {
                    Toast.makeText(HomeActivity.this, R.string.store_not_installed, Toast.LENGTH_SHORT).show();
                }

                quitSearch();
            }
        });
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String change = s.toString();
                performAppQuery(change);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                        (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    //Try and open app
                    ((ApplicationIconAdapter) allAppsScreen.getAdapter()).launchTop();
                }
                return false;
            }
        });
        searchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    collapseSuggestions();
                } else {
                    expandSuggestions();
                }
            }
        });

        //Set drag type of dockbar
        appDropLayout.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        if (event.getLocalState() == null || !(event.getLocalState() instanceof ApplicationIcon))
                            return false;

                        double sw = getResources().getDisplayMetrics().widthPixels;
                        double prog = event.getX() / sw;

                        if (prog >= (2d / 3d) && allAppsContainer.getTranslationY() == getAbsoluteBottom()) {
                            showDockApps();
                            return true;
                        }
                        return false;
                    case DragEvent.ACTION_DROP:
                        if (event.getLocalState() == null || !(event.getLocalState() instanceof ApplicationIcon))
                            return false;

                        double screenWidth = getResources().getDisplayMetrics().widthPixels;
                        double progress = event.getX() / screenWidth;

                        if (progress <= (1d / 3d)) {

                            ApplicationIcon ai = (ApplicationIcon) event.getLocalState();
                            try {
                                Uri uri = Uri.parse("package:" + ai.getPackageName());
                                Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                                uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                                v.getContext().startActivity(uninstallIntent);
                            } catch (Exception e) {
                                Toast.makeText(v.getContext(), "The app is unable to be removed.", Toast.LENGTH_LONG).show();
                            }
                            return true;
                        } else if (progress <= (2d / 3d)) {
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
                            return true;
                        } else {
                            return false; //Should've already moved
                        }
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
                            toggleAppsContainer(false);
                            setAndAnimateToDarkBackgroundTint();
                            showDropMenuFast();
                        }
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        resetState();
                        break;
                }
                return true;
            }
        });

        //Move the screen as needed
        toggleAppsContainer(false);
        showDockApps();

        //Check if we've run before
        if (!reader.getBoolean(Constants.HAS_RUN_PREFERENCE, false)) {
            writer.putInt(Constants.VERSION_PREF, Constants.Versions.CURRENT_VERSION).apply();
            sgv.setCards(new ArrayList<TypeCard>());

            showTutorial();
        } else {
            //Do version upgrades
            int lastUsedVersion = reader.getInt(Constants.VERSION_PREF, Constants.Versions.VERSION_0_2_3);
            switch (lastUsedVersion) {
                case Constants.Versions.VERSION_0_2_3:
                    writer.putBoolean(Constants.HOME_WIDGET_PREFERENCE, false).commit();
                    writer.putInt(Constants.HOME_WIDGET_ID_PREFERENCE, -1).commit();
                    break;
            }

            if (lastUsedVersion != Constants.Versions.CURRENT_VERSION) {
                writer.putInt(Constants.VERSION_PREF, Constants.Versions.CURRENT_VERSION).commit();
            }

            loadList();
            loadHiddenApps();
            loadSmartApps();
        }

        //Set up broadcast receivers
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Utilities.logEvent(Utilities.LogLevel.SYS_BG_TASK, "Package removed and/or changed");

                if(searchBox != null) searchBox.setText("");
                loadApps();
                sgv.invalidateCaches();
                IconCache.getInstance().invalidateCaches();

                Utilities.logEvent(Utilities.LogLevel.SYS_BG_TASK, "Accordingly, invalidating caches...");
            }
        };

        storageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Utilities.logEvent(Utilities.LogLevel.SYS_BG_TASK, "A storage medium has been chanegd");

                if(searchBox != null) searchBox.setText("");
                loadApps();
                sgv.invalidateCaches();
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

        //Setup app drawer
        updateRowCount(getResources().getConfiguration().orientation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (packageReceiver != null) {
            unregisterReceiver(packageReceiver);
        }
        if (storageReceiver != null) {
            unregisterReceiver(storageReceiver);
        }
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
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        if(reader.getBoolean(Constants.ALLOW_ROTATION_PREF, false)) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }

        updateDisplay();

        //Update most parts on timer
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                updateDisplay();
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(t, 0, 1000);

        resetState();

        if(System.currentTimeMillis() - lastPauseTime > 1000L){
            updateWidgetAvailabilityInfo();
            sgv.onActivityResumed();
            startSuggestionsPopulation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause");

        if (timer != null) {
            timer.cancel();
        }

        lastPauseTime = System.currentTimeMillis();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { //Back button pressed
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
        for (int result : grantResults) allGranted &= result == PackageManager.PERMISSION_GRANTED;

        if (!allGranted) {
            Toast.makeText(HomeActivity.this, R.string.okay_we_wont_ask, Toast.LENGTH_SHORT).show();
        }
        populateSuggestions();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(previousConfiguration.densityDpi != newConfig.densityDpi)
            IconCache.getInstance().invalidateCaches();

        updateRowCount(newConfig.orientation);
        previousConfiguration = newConfig;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHOOSE_ICON && resultCode == RESULT_OK) {
            String pkg = data.getStringExtra("r_pkg");
            String rName = data.getStringExtra("r_name");
            if (packageName == null || rName == null) {
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
                if (resId == 0) throw new Exception("");
                d = getPackageManager().getResourcesForApplication(packageName).getDrawable(resId);
            } catch (Exception e) {
                d = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }

            if (dialogView != null) {
                ((ImageButton) dialogView.findViewById(R.id.chooseIcon)).setImageDrawable(d);
            }
        } else if (requestCode == REQUEST_CHOOSE_APPLICATION && resultCode == RESULT_OK) {
            ActivityInfo activityInfo = data.resolveActivityInfo(getPackageManager(), 0);
            ApplicationIcon ai = new ApplicationIcon(activityInfo.packageName,
                    (String) activityInfo.loadLabel(getPackageManager()), activityInfo.name);

            //Store data in SharedPreferences
            Gson gson = new Gson();
            writer.putString(cachedPref, gson.toJson(ai)).apply();
        } else if (requestCode == REQUEST_ALLOCATE_ID || requestCode == REQUEST_PICK_APP_WIDGET) {
            if (resultCode == RESULT_OK) {
                handleWidgetConfig(data);
            } else if (data != null) {
                Toast.makeText(this, "You must allow us to manage widgets to use widgets.", Toast.LENGTH_LONG).show();
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    widgetHost.deleteAppWidgetId(appWidgetId);
                }
            }
        } else if (requestCode == REQUEST_CONFIG_WIDGET) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();

                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                AppWidgetProviderInfo appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId);

                saveWidget(appWidgetId, appWidgetInfo);
                replaceDrawerWidget(appWidgetId, appWidgetInfo);
            } else if (resultCode == RESULT_CANCELED && data != null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    widgetHost.deleteAppWidgetId(appWidgetId);
                }
            }
        }
    }
    //endregion

    //region App drawer
    List<ApplicationIcon> cachedApps = new ArrayList<>();
    PatriciaTrie<ApplicationIcon> appsTree = new PatriciaTrie<>();

    private void loadApps(){
        cachedApps.clear();
        appsTree.clear();
        searchBox.setEnabled(false);

        allAppsMessageLayout.setVisibility(View.VISIBLE);
        allAppsMessage.setText(R.string.loading);
        allAppsSearchGoogle.setVisibility(View.GONE);

        new AsyncTask<Object, Void, List<ApplicationIcon>>() {
            @Override
            protected List<ApplicationIcon> doInBackground(Object... params) {
                PackageManager pm = getPackageManager();
                List<ApplicationIcon> applicationIcons = new ArrayList<>();

                //Grab all matching applications
                final Intent allAppsIntent = new Intent(Intent.ACTION_MAIN, null);
                allAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                final List<ResolveInfo> packageList = pm.queryIntentActivities(allAppsIntent, 0);
                for (ResolveInfo ri : packageList) {
                    try {
                        String name = (String) ri.loadLabel(pm);
                        if (!hiddenApps.contains(new Pair<>(ri.activityInfo.packageName,
                                ri.activityInfo.name))) {
                            applicationIcons.add(new ApplicationIcon(ri.activityInfo.packageName,
                                    name, ri.activityInfo.name));
                            ri.loadIcon(pm);
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
                for(ApplicationIcon icon : applicationIcons){
                    appsTree.put(icon.getName().toLowerCase(Locale.getDefault()), icon);
                }
                return applicationIcons;
            }

            @Override
            protected void onPostExecute(List<ApplicationIcon> apps) {
                cachedApps = apps;
                searchBox.setEnabled(true);
                allAppsMessageLayout.setVisibility(View.GONE);
                performAppQuery(null);
            }
        }.execute();
    }

    public void performAppQuery(final String query) {
        int visibility = query == null || query.isEmpty() ? View.GONE : View.VISIBLE;

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

        clearSearch.setVisibility(visibility);
        if(visibility == View.VISIBLE && result.isEmpty()) {
            allAppsMessageLayout.setVisibility(View.VISIBLE);
            allAppsMessage.setText(getString(R.string.no_apps_matching_installed, query));
            allAppsSearchGoogle.setVisibility(View.VISIBLE);
        } else {
            allAppsMessageLayout.setVisibility(View.GONE);
        }

        ApplicationIconAdapter adapter = new ApplicationIconAdapter(result, this);
        adapter.setHasStableIds(true);
        allAppsScreen.setAdapter(adapter);

        FastScroller scroller = new FastScroller(allAppsScreen, scrollerBar, startLetter, endLetter, popup);
        scroller.setupList((List) result);
        scroller.setupScrollbar();
    }

    public void toggleAppsContainer(boolean visible) {
        toggleAppsContainer(visible, -1);
    }

    public void toggleAppsContainer(boolean visible, float animationVelocity) {
        //Do the popup for usage requests if needed
        if (visible && !reader.getBoolean(Constants.HAS_REQUESTED_USAGE_PERMISSION_PREF, false)) {
            try {
                @SuppressWarnings("ResourceType")
                final UsageStatsManager usm = (UsageStatsManager) HomeActivity.this.getSystemService("usagestats");

                long end = 0L;
                long start = System.currentTimeMillis();

                UsageEvents use = usm.queryEvents(start, end);
                UsageEvents.Event event = new UsageEvents.Event();
                if (use.hasNextEvent() && use.getNextEvent(event)) {
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
        if (visible) {
            long duration;
            if (animationVelocity > 0) {
                float distanceToGo = allAppsContainer.getTranslationY();
                duration = (long) (distanceToGo / animationVelocity);
                if (duration > 600) duration = 600;
            } else {
                duration = 500;
            }


            ObjectAnimator oa = ObjectAnimator.ofFloat(allAppsContainer, "translationY", getAbsoluteTop());
            oa.setDuration(duration);
            oa.start();

            ObjectAnimator oa2 = ObjectAnimator.ofFloat(dockBar, "translationY", -getAbsoluteBottom());
            oa2.setDuration(duration);
            oa2.start();

            ObjectAnimator.ofFloat(bigTint, "alpha", 1)
                    .setDuration(duration)
                    .start();

            strayTouchCatch.setVisibility(View.VISIBLE);
        } else {
            ObjectAnimator oa = ObjectAnimator.ofFloat(allAppsContainer, "translationY", getAbsoluteBottom());
            oa.setDuration(500);
            oa.start();

            ObjectAnimator oa2 = ObjectAnimator.ofFloat(dockBar, "translationY", 0);
            oa2.setDuration(500);
            oa2.start();

            strayTouchCatch.setVisibility(View.GONE);
        }
    }

    public void updateRowCount(int orientation) {
        int columnCount;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Utilities.isSmallTablet(this)) {
                size = ScreenSize.SMALL_TABLET;
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                columnCount = 7;
            } else if (Utilities.isLargeTablet(this)) {
                size = ScreenSize.LARGE_TABLET;
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                columnCount = 8;
            } else {
                size = ScreenSize.PHONE;
                columnCount = 4;
            }
        } else {
            if (Utilities.isSmallTablet(this)) {
                size = ScreenSize.SMALL_TABLET;
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                columnCount = 5;
            } else if (Utilities.isLargeTablet(this)) {
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
        loadApps();
    }

    private void quitSearch() {
        searchBox.setText("");
        searchBox.clearFocus();
        clearSearch.setVisibility(View.GONE);
        hideKeyboard();
    }

    public void hideKeyboard() {
        //Hide keyboard
        InputMethodManager imm = (InputMethodManager)
                HomeActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.bigTint.getWindowToken(), 0);
    }
    //endregion

    //region Suggested apps
    private void startSuggestionsPopulation() {
        if (!reader.getBoolean(Constants.HAS_RUN_PREFERENCE, false)) return;

        Log.d(TAG, "Starting suggestions population...");

        //We always show a prompt UI to explain if we need to ask for permissions.
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALENDAR};
        List<String> neededPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (!Utilities.checkPermission(permission, this)) neededPermissions.add(permission);
        }

        if (neededPermissions.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !reader.getBoolean(Constants.HAS_REQUESTED_PERMISSIONS_PREF, false)) {
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

    private ApplicationIcon addSmartbarAppFromPref(String prefName) {
        String existingData = reader.getString(prefName, "null");
        if (!existingData.equals("null")) {
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

    private void addSuggestionsApp(final String packageName) {
        View v = LayoutInflater.from(HomeActivity.this).inflate(R.layout.popular_app_icon, suggestionsLayout, false);

        final Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null) return;

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

        //Set drag option
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String appName = packageName;
                try {
                    appName = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0)).toString();
                } catch (Exception ignored) {}

                new MaterialDialog.Builder(v.getContext())
                        .title(R.string.suppress_suggestion_title)
                        .content(R.string.suppress_suggestion_message, appName)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                launchableCachedMap.clear();
                                smartApps.add(new Pair<>(packageName, DatabaseHelper.SMARTAPP_SHOW_NEVER));
                                persistSmartApps();
                                populateSuggestions();
                            }
                        }).show();
                return true;
            }
        });

        int size = (int) Utilities.convertDpToPixel(48f, this);
        final StickyImageView popularAppIcon = (StickyImageView) v.findViewById(R.id.popularAppIcon);
        popularAppIcon.setImageBitmap(IconCache.getInstance().getAppIcon(launchIntent.getPackage(), launchIntent.getComponent().getClassName(), IconCache.IconFetchPriority.DOCK_ICONS, size, new IconCache.ItemRetrievalInterface() {
            @Override
            public void onRetrievalComplete(Bitmap result) {
                popularAppIcon.setImageBitmap(result);
            }
        }));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);

        suggestionsLayout.addView(v, params);
    }

    private class SuggestionApp {
        private String packageName;
        private String className;
        private long timeUsed;

        public SuggestionApp(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
            this.timeUsed = Long.MAX_VALUE;
        }

        public SuggestionApp(String packageName, String className, long timeUsed) {
            this.packageName = packageName;
            this.className = className;
            this.timeUsed = timeUsed;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getClassName() {
            return className;
        }

        public long getTimeUsed() {
            return timeUsed;
        }
    }

    List<SuggestionApp> suggestions = new ArrayList<>();
    private void populateSuggestions() {
        //Clean up
        suggestionsLayout.removeAllViews();

        suggestionsSV.setVisibility(View.GONE);
        noSuggestions.setVisibility(View.GONE);
        loadingSuggestions.setVisibility(View.VISIBLE);
        suggestions.clear();

        //(1) Check if we're in a phone call
        boolean hasPhonePermission = Utilities.checkPermission(Manifest.permission.READ_PHONE_STATE, this);

        if (hasPhonePermission) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            int state = tm.getCallState();
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) { //In call
                ApplicationIcon ai = addSmartbarAppFromPref(Constants.PHONE_APP_PREFERENCE);
                if (ai != null) {
                    suggestions.add(new SuggestionApp(ai.getPackageName(), ai.getActivityName()));
                }
            }
        }

        //(2) Check if we're in a calendar event
        boolean hasCalendarPermission = Utilities.checkPermission(Manifest.permission.READ_CALENDAR, this);

        if (hasCalendarPermission) {
            ContentResolver cr = getContentResolver();
            String[] columns = new String[]{CalendarContract.Instances.DTSTART, CalendarContract.Instances.DTEND,
                    CalendarContract.Instances.TITLE, CalendarContract.Instances.ALL_DAY};
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, System.currentTimeMillis());
            ContentUris.appendId(builder, System.currentTimeMillis() + (1000l * 60l * 60l * 24l));
            Cursor c = cr.query(builder.build(), columns, null, null, CalendarContract.Instances.DTSTART + " asc");
            calendarBlock:
            {
                if (c != null && c.moveToFirst()) {
                    Log.d(TAG, "Calendar block move to first!");

                    int allDayCol = c.getColumnIndex(CalendarContract.Instances.ALL_DAY);
                    int startCol = c.getColumnIndex(CalendarContract.Instances.DTSTART);
                    int endCol = c.getColumnIndex(CalendarContract.Instances.DTEND);
                    int titleCol = c.getColumnIndex(CalendarContract.Instances.TITLE);

                    if (startCol == -1 || endCol == -1 || titleCol == -1 || allDayCol == -1 ||
                            c.getCount() == 0) {
                        Log.d(TAG, "Count/columns 0!");
                        c.close();
                        break calendarBlock;
                    }

                    while(!c.isAfterLast()) {
                        String title = c.getString(titleCol);
                        boolean allDay = c.getInt(allDayCol) == 1;
                        long startTime = c.getLong(startCol);
                        long endTime = c.getLong(endCol);
                        long duration = c.getLong(endCol) - c.getLong(startCol);

                        Log.d(TAG, "Curr: " + System.currentTimeMillis() + ", start: " + startTime + ", end: " + endTime + ", duration: " + duration);

                        if (System.currentTimeMillis() > startTime && System.currentTimeMillis() < endTime
                                && !allDay) {
                            Log.d(TAG, "In event!");

                            ApplicationIcon ai = addSmartbarAppFromPref(Constants.CALENDAR_APP_PREFERENCE);
                            if (ai != null) {
                                suggestions.add(new SuggestionApp(ai.getPackageName(), ai.getActivityName()));
                            }
                            break;
                        }

                        c.moveToNext();
                    }

                    c.close();
                }
            }
        }

        //(3) Check if we're almost out of power
        if (hasPhonePermission) {
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
                        suggestions.add(new SuggestionApp(ai.getPackageName(), ai.getActivityName()));
                    }
                } else if (isCharging) {
                    ApplicationIcon ai = addSmartbarAppFromPref(Constants.CHARGING_APP_PREFERENCE);
                    if (ai != null) {
                        suggestions.add(new SuggestionApp(ai.getPackageName(), ai.getActivityName()));
                    }
                }
            }
        }

        //(4) Check condition rules (WEEKDAY apps, WEEKEND apps, WORK apps, HOME apps)
        //TODO: Condition rules

        //(5) Use remaining space to show popular apps
        // Get data from system usage events and integrate into our own database
        new AsyncTask<Void, Void, List<SuggestionApp>>() {
            @Override
            protected List<SuggestionApp> doInBackground(Void... params) {
                try {
                    @SuppressWarnings("ResourceType")
                    final UsageStatsManager usm = (UsageStatsManager) HomeActivity.this.getSystemService("usagestats");
                    UsageEvents.Event e = new UsageEvents.Event();

                    //(1) Look at the past 4 hours
                    Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                    cal.roll(Calendar.HOUR_OF_DAY, -4);
                    long end = System.currentTimeMillis();
                    long start = cal.getTimeInMillis();
                    UsageEvents use = usm.queryEvents(start, end);
                    Map<String, Long> startUseMap = new HashMap<>();
                    Map<String, Long> useTimeMap = new HashMap<>();
                    while (use.getNextEvent(e)) {
                        if(!isComponentValidSuggestion(e.getPackageName())) continue;

                        String packageName = e.getPackageName();
                        if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            startUseMap.put(packageName, e.getTimeStamp());
                        } else if (e.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND){
                            if (startUseMap.containsKey(packageName)) {
                                long startTime = startUseMap.get(packageName);
                                //16-hours of phone use a day (about) -- 4 * 4 = 16;
                                //generally we want the expected daily use amount, but since this
                                //is recent we weight it twice as heavily
                                long timeUsed = (e.getTimeStamp() - startTime) * 8;
                                if(useTimeMap.containsKey(packageName)){
                                    useTimeMap.put(packageName, useTimeMap.get(packageName) + timeUsed);
                                } else {
                                    useTimeMap.put(packageName, timeUsed);
                                }
                            }
                        }
                    }

                    //(2) Look at past day
                    cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                    cal.roll(Calendar.DAY_OF_WEEK, -1);
                    start = cal.getTimeInMillis();
                    end = System.currentTimeMillis();
                    List<UsageStats> granularOptions = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
                    for(UsageStats entry : granularOptions) {
                        if (!isComponentValidSuggestion(entry.getPackageName())) continue;

                        long timeUsed = (long) (entry.getTotalTimeInForeground() * 1.5); //Weighed a little more heavily than week
                        if(useTimeMap.containsKey(entry.getPackageName())){
                            useTimeMap.put(entry.getPackageName(), useTimeMap.get(entry.getPackageName()) + timeUsed);
                        } else {
                            useTimeMap.put(entry.getPackageName(), timeUsed);
                        }
                    }

                    //(3) If needed, look at past week
                    if(useTimeMap.size() < 10){
                        cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                        cal.roll(Calendar.WEEK_OF_YEAR, -1);
                        start = cal.getTimeInMillis();
                        end = System.currentTimeMillis();
                        List<UsageStats> leastGranularOptions = usm.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, start, end);
                        for(UsageStats entry : leastGranularOptions) {
                            if (!isComponentValidSuggestion(entry.getPackageName())) continue;

                            long timeUsed = (long) (entry.getTotalTimeInForeground() / 7); //Weighed at base value
                            if(useTimeMap.containsKey(entry.getPackageName())){
                                useTimeMap.put(entry.getPackageName(), useTimeMap.get(entry.getPackageName()) + timeUsed);
                            } else {
                                useTimeMap.put(entry.getPackageName(), timeUsed);
                            }
                        }
                    }

                    List<String> packagesAdded = new ArrayList<>();
                    for(SuggestionApp s : suggestions){
                        packagesAdded.add(s.getPackageName());
                    }
                    List<SuggestionApp> strippedList = new ArrayList<>();
                    for (Map.Entry<String, Long> entry : useTimeMap.entrySet()) {
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(entry.getKey());
                        if(launchIntent != null && !packagesAdded.contains(entry.getKey())) {
                            strippedList.add(new SuggestionApp(launchIntent.getPackage(), launchIntent.getComponent().getClassName(), entry.getValue()));
                        }
                    }

                    return strippedList;
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<SuggestionApp> result) {
                loadingSuggestions.setVisibility(View.GONE);
                suggestions.addAll(result);

                Collections.sort(suggestions, new Comparator<SuggestionApp>() {
                    @Override
                    public int compare(SuggestionApp lhs, SuggestionApp rhs) {
                        return (int) (rhs.getTimeUsed() - lhs.getTimeUsed());
                    }
                });

                //Strip out excessive apps
                if(suggestions.size() > 10){
                    suggestions = suggestions.subList(0, 10);
                }

                //"De-dup" the result
                //Yes, it looks like a nasty O(n^2) algorithm -- it is, actually, but the commonly accepted
                //way of dedup-ing in Java (new List<>(new HashSet<>(yourItems)) does the same under the
                //hood (with more overhead!)
                for(int i = 0; i < suggestions.size(); i++){
                    SuggestionApp a1 = suggestions.get(i);
                    addSuggestionsApp(a1.getPackageName());
                }

                //Needed check -- addSuggestionsApp might not always add if a package is un-launchable
                if(suggestionsLayout.getChildCount() == 0){
                    noSuggestions.setVisibility(View.VISIBLE);
                    suggestionsSV.setVisibility(View.GONE);
                } else {
                    suggestionsSV.setVisibility(View.VISIBLE);
                    suggestionsSV.scrollTo(0, 0);
                }
            }
        }.execute();
    }

    Map<Integer, ApplicationIcon> dockbarTargetsMap = new HashMap<>();
    private Map<String, Boolean> launchableCachedMap = new HashMap<>();

    public boolean isComponentValidSuggestion(String packageName) {
        if(packageName == null) return false;

        if(!launchableCachedMap.containsKey(packageName)) {
            boolean isValid = getPackageManager().getLaunchIntentForPackage(packageName) != null;
            for(Map.Entry<Integer, ApplicationIcon> entry : dockbarTargetsMap.entrySet()){
                if(entry.getValue().getPackageName().equals(packageName)){
                    isValid = false;
                    break;
                }
            }

            for(Pair<String, Integer> packageToUse : smartApps){
                if(packageToUse.first.equals(packageName) && packageToUse.second == DatabaseHelper.SMARTAPP_SHOW_NEVER){
                    isValid = false;
                    break;
                }
            }

            launchableCachedMap.put(packageName, isValid);
        }

        return launchableCachedMap.get(packageName);
    }

    private void expandSuggestions() {
        /*
        Animation anim = new ScaleAnimation(
                1f, 1f, // Start and end values for the X axis scaling
                0.0f, 1.0f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 1f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        suggestionsRootView.startAnimation(anim);
        */

        /*
        ObjectAnimator oa = ObjectAnimator.ofFloat(suggestionsRootView, "scaleY", 1.0f);
        oa.setDuration(400L);
        oa.start();
        */
    }

    private void collapseSuggestions() {
        /*
        Animation anim = new ScaleAnimation(
                1f, 1f, // Start and end values for the X axis scaling
                1.0f, 0.0f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 1f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        suggestionsRootView.startAnimation(anim);
        */

        /*
        ObjectAnimator oa = ObjectAnimator.ofFloat(suggestionsRootView, "scaleY", 0);
        oa.setDuration(400L);
        oa.start();
        */
    }
    //endregion

    //region Manage ShortcutGestureView apps
    private void loadList() {
        Log.d(TAG, "Loading list...");

        loadRows:
        {
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
                    if (data.startsWith("<!--APPTYPE-->")) { //This is just one app, not a full row
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

    /**
     * Saves cards to the database.
     *
     * @param cards Cards to save.
     */
    public void persistList(List<TypeCard> cards) {
        Log.d(TAG, "Persisting at most " + cards.size() + " cards");
        db.delete(DatabaseHelper.TABLE_ROWS, null, null);
        for (int i = 0; i < cards.size(); i++) {
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

    public void showCreateFolderDialog(final ApplicationIcon ai) {
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
                        if (name == null || name.length() == 0) {
                            name = "Folder";
                        }
                        if (packageName.equals("null")) {
                            packageName = this.getClass().getPackage().getName();
                            resourceName = "ic_folder_white_48dp";
                        }
                        List<Pair<String, String>> apps = new ArrayList<Pair<String, String>>();
                        apps.add(new Pair<>(ai.getPackageName(), ai.getActivityName()));
                        samples.add(new TypeCard(name, packageName, resourceName, apps));
                        persistList(samples);
                        sgv.invalidate();
                        dialog.dismiss();
                    }
                }).show();
    }

    public void showEditFolderDialog(final int row) {
        if (row >= 0 && row < samples.size()) {
            packageName = samples.get(row).getDrawablePackage();
            resourceName = samples.get(row).getDrawableName();

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
                if (resId == 0) throw new Exception("");
                d = getPackageManager().getResourcesForApplication(packageName).getDrawable(resId);
            } catch (Exception e) {
                d = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }

            ((ImageButton) dialogView.findViewById(R.id.chooseIcon)).setImageDrawable(d);
            final EditText et = (EditText) dialogView.findViewById(R.id.chooseName);
            et.setText(samples.get(row).getTitle());
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
                            samples.remove(row);
                            persistList(samples);
                            sgv.notifyShortcutsChanged();
                        }

                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            String name = et.getText().toString();
                            if (name == null || name.length() == 0) {
                                name = "Folder";
                            }

                            if (packageName.equals("null")) {
                                packageName = this.getClass().getPackage().getName();
                                resourceName = "ic_folder_white_48dp";
                            }
                            samples.get(row).setTitle(name);
                            samples.get(row).setDrawable(resourceName, packageName);

                            persistList(samples);
                            sgv.notifyShortcutsChanged();
                        }
                    }).show();
        }
    }

    public void showReorderDialog(final int row) {
        depthThreeDialogRaised = false;

        DragSortListView dsiv = (DragSortListView) LayoutInflater.from(this).inflate(R.layout.sort_list, null);
        dsiv.setAdapter(new AppEditAdapter(this,
                R.layout.app_edit_row, samples.get(row).getPackages()));
        dsiv.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        new MaterialDialog.Builder(this)
                .customView(dsiv, false)
                .title(R.string.edit_items)
                .positiveText(R.string.save)
                .neutralText(R.string.help)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!depthThreeDialogRaised) showEditFolderDialog(row);
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

    public void batchOpen(int row) {
        if (row >= 0 && row < samples.size()) {
            Intent sequentialLauncherService = new Intent(this, SequentialLauncherService.class);
            sequentialLauncherService.putExtra("row_position", row);
            this.startService(sequentialLauncherService);
        }
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

    public void invalidateGestureView(){
        sgv.invalidate();
    }
    //endregion

    //region Manage hidden apps
    private void loadHiddenApps() {
        Log.d(TAG, "Loading hidden apps...");
        hiddenApps.clear();

        loadRows:
        {
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
        new AsyncTask<Void, Void, List<ApplicationHiderIcon>>() {
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
                } catch (RuntimeException packageManagerDiedException) {
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<ApplicationHiderIcon> apps) {
                final ApplicationHideAdapter adapter = new ApplicationHideAdapter(HomeActivity.this, apps);
                new MaterialDialog.Builder(HomeActivity.this)
                        .adapter(adapter, new LinearLayoutManager(HomeActivity.this))
                        .title(R.string.hideApps)
                        .positiveText(R.string.done)
                        .negativeText(R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                persistHidden(adapter.getApps());
                                loadHiddenApps();
                                loadApps();
                            }
                        }).show();
            }
        }.execute();
    }

    private void showManageSuggestionsMenu() {
        new AsyncTask<Void, Void, List<ApplicationHiderIcon>>() {
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
                            if (smartApps.contains(new Pair<>(ri.activityInfo.packageName, DatabaseHelper.SMARTAPP_SHOW_NEVER))) {
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

                    Collections.sort(applicationIcons, new Comparator<ApplicationHiderIcon>() {
                        @Override
                        public int compare(ApplicationHiderIcon lhs, ApplicationHiderIcon rhs) {
                            if(lhs.getIsHidden() && rhs.getIsHidden()) return lhs.getName().compareToIgnoreCase(rhs.getName());
                            if(lhs.getIsHidden()) return -1;
                            if(rhs.getIsHidden()) return 1;
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
                final ApplicationHideAdapter adapter = new ApplicationHideAdapter(HomeActivity.this, apps);
                new MaterialDialog.Builder(HomeActivity.this)
                        .adapter(adapter, new LinearLayoutManager(HomeActivity.this))
                        .title(R.string.manage_suggestions)
                        .positiveText(R.string.done)
                        .negativeText(R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                launchableCachedMap.clear();
                                smartApps.clear();
                                for(ApplicationHiderIcon icon : adapter.getApps()){
                                    if(icon.getIsHidden()) smartApps.add(new Pair<>(icon.getPackageName(), DatabaseHelper.SMARTAPP_SHOW_NEVER));
                                }
                                persistSmartApps();
                                populateSuggestions();
                            }
                        }).show();
            }
        }.execute();
    }

    private void persistHidden(List<ApplicationHiderIcon> apps) {
        Log.d(TAG, "Persisting at most " + hiddenApps.size() + " hidden apps");
        db.delete(DatabaseHelper.TABLE_HIDDEN_APPS, null, null);
        for (int i = 0; i < apps.size(); i++) {
            if (!apps.get(i).getIsHidden()) continue;

            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COLUMN_ACTIVITY_NAME, apps.get(i).getActivityName());
            cv.put(DatabaseHelper.COLUMN_PACKAGE, apps.get(i).getPackageName());

            Log.d(TAG, "Trying to insert: " + cv);

            long result = db.insert(DatabaseHelper.TABLE_HIDDEN_APPS, null, cv);

            Log.d(TAG, result == -1 ? "Error inserting: " + i :
                    "Inserted hidden: " + i + " at " + result);
        }
    }

    private void loadSmartApps() {
        Log.d(TAG, "Loading smart apps...");
        smartApps.clear();

        loadRows:
        {
            Cursor loadItems = db.query(DatabaseHelper.TABLE_SMARTAPPS, null, null, null, null, null, null);
            if (loadItems.moveToFirst()) {
                Log.d(TAG, "Found: " + loadItems.getCount());

                int packageColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_PACKAGE);
                int whenColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_WHEN_TO_SHOW);
                if (packageColumn == -1 || whenColumn == -1) {
                    loadItems.close();
                    break loadRows;
                }

                while (!loadItems.isAfterLast()) {
                    String packageName = loadItems.getString(packageColumn);
                    int when = loadItems.getInt(whenColumn);

                    Log.d(TAG, "Loading '" + packageName + "'...");
                    smartApps.add(new Pair<>(packageName, when));

                    loadItems.moveToNext();
                }
            } else {
                Log.d(TAG, "No smart app prefs found!");
            }
            loadItems.close();
        }
    }

    private void persistSmartApps() {
        Log.d(TAG, "Persisting at most " + smartApps.size() + " smartapp hide prefs apps");

        db.delete(DatabaseHelper.TABLE_SMARTAPPS, null, null);
        for (int i = 0; i < smartApps.size(); i++) {

            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COLUMN_PACKAGE, smartApps.get(i).first);
            cv.put(DatabaseHelper.COLUMN_WHEN_TO_SHOW, smartApps.get(i).second);

            long result = db.insert(DatabaseHelper.TABLE_SMARTAPPS, null, cv);

            Log.d(TAG, result == -1 ? "Error inserting: " + i : "Inserted smartapp: " + i + " at " + result);
        }
    }
    //endregion

    //region ShortcutGestureViewHost helpers
    @Override
    public Pair<Float, Float> getBoundsWhenNotFullscreen() {
        float top = timeDateContainer.getTop() + timeDateContainer.getHeight();
        float bottom = dockBar.getTop();

        return new Pair<>(top, bottom);
    }

    @Override
    public float getTopMargin() {
        return timeDateContainer.getY() + timeDateContainer.getHeight();
    }

    @Override
    public float getBottomMargin() {
        return dockBar.getHeight();
    }
    //endregion

    //region Widgets
    @OnClick(R.id.widget_collapse)
    public void setWidgetCollapse(){
        collapseWidgetDrawer();
        showTopElements();
        showBottomElements();
        clearBackgroundTint();
        sgv.resetState();
    }

    @OnClick(R.id.widget_delete)
    public void deleteWidget(){
        deleteWidget(currentWidget);
        setWidgetCollapse();
    }

    private void defrostWidgets() {
        if(widgetsDefrosted)
            return;

        Log.d(TAG, "Defrosting widgets...");

        int foundCount = 0;
        List<Integer> widgetIds = new ArrayList<>();
        loadRows:
        {
            Cursor loadItems =
                    db.query(DatabaseHelper.TABLE_WIDGETS, null, null, null, null, null, null);
            if (loadItems.moveToFirst()) {
                foundCount = loadItems.getCount();
                Log.d(TAG, "Found: " + foundCount);

                int idColumn = loadItems.getColumnIndex(DatabaseHelper.COLUMN_WIDGET_ID);
                if (idColumn == -1) {
                    loadItems.close();
                    break loadRows;
                }

                while (!loadItems.isAfterLast()) {
                    widgetIds.add(loadItems.getInt(idColumn));
                    loadItems.moveToNext();
                }
            } else {
                Log.d(TAG, "No widgets found!");
            }
            loadItems.close();
        }

        widgetIdsWithInfo.clear();
        for(Integer j : widgetIds){
            try {
                getPackageManager().getResourcesForApplication(widgetManager.getAppWidgetInfo(j).provider.getPackageName());
            } catch (Exception e) {
                Log.e(TAG, "App widget for " + j + " isn't installed...", e);
                continue;
            }

            AppWidgetProviderInfo awpi = widgetManager.getAppWidgetInfo(j);
            if (awpi == null) {
                Log.e(TAG, "App widget for " + j + " isn't installed...");
                continue;
            }

            widgetIdsWithInfo.add(new Pair<>(j, awpi));
        }

        if (foundCount != widgetIds.size()) {
            Log.w(TAG, "WARNING: Lost " + (foundCount - widgetIds.size()) + " widgets");
            persistWidgets(); //Update our list -- don't want to keep around mistakes!
        } else {
            Log.d(TAG, "All widgets loaded OK!");
        }

        widgetsDefrosted = true;
    }

    /**
     * Save widgets to the database.
     *
     * @param @widgets Widgets to save.
     */
    public void persistWidgets() {
        Log.d(TAG, "Persisting at most " + widgetIdsWithInfo.size() + " widgets");
        db.delete(DatabaseHelper.TABLE_WIDGETS, null, null);
        for (int i = 0; i < widgetIdsWithInfo.size(); i++) {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COLUMN_WIDGET_ID, widgetIdsWithInfo.get(i).first);
            Log.d(TAG, "Trying to insert: " + cv);
            long result = db.insert(DatabaseHelper.TABLE_WIDGETS, null, cv);
            Log.d(TAG, result == -1 ? "Error inserting: " + i : "Inserted widget: " + i + " at " + result);
        }
    }

    @Override
    public void showWidget(final String packageName) {
        Log.d(TAG, "Looking to show or add widget for " + packageName);
        defrostWidgets();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Show the widget overlay
                //(1) Do we have a match?
                Pair<Integer, AppWidgetProviderInfo> match = null;
                for (int i = 0; i < widgetIdsWithInfo.size(); i++) {
                    if (packageName.equalsIgnoreCase(widgetIdsWithInfo.get(i).second.provider.getPackageName())) {
                        match = widgetIdsWithInfo.get(i);
                        break;
                    }
                }

                //(2) Yes -- add widget
                if (match != null) {
                    Log.d(TAG, "Found widget match!");
                    if(replaceDrawerWidget(match.first, match.second)){
                        Log.d(TAG, "Managed to replace!");
                        expandWidgetDrawer();
                        setAndAnimateToDarkBackgroundTint();
                        hideTopElements();
                        hideBottomElements();
                        showingWidget = true;
                    } else {
                        Log.d(TAG, "Failed to replace!");
                        Toast.makeText(HomeActivity.this, R.string.widget_no_longer_valid, Toast.LENGTH_SHORT).show();
                    }
                } else { //(2) No -- show add menu
                    clearBackgroundTint();
                    showTopElements();
                    showBottomElements();

                    showAddWidgetMenu(packageName);
                }
            }
        });
    }

    @Override
    public void collapseWidgetDrawer() {
        ObjectAnimator oa = ObjectAnimator.ofFloat(widgetDrawer, "translationY", widgetDrawer.getHeight());
        oa.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                widgetDrawer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                widgetDrawer.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        oa.start();

        showingWidget = false;
    }

    private void expandWidgetDrawer(){
        Log.d(TAG, "Expanding widget drawer...");

        ObjectAnimator oa = ObjectAnimator.ofFloat(widgetDrawer, "translationY", widgetDrawer.getHeight(), 0);
        oa.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                widgetDrawer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        oa.start();
    }

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

                if(matchingProviders.size() == 1){
                    AppWidgetProviderInfo awpi = matchingProviders.get(0);
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
                } else {
                    final WidgetAddAdapter adapter = new WidgetAddAdapter(matchingProviders, HomeActivity.this);
                    final MaterialDialog md = new MaterialDialog.Builder(HomeActivity.this)
                            .adapter(adapter, new LinearLayoutManager(HomeActivity.this))
                            .title("Select a " + matchingProviders.get(0).label + " Widget")
                            .negativeText(R.string.cancel)
                            .build();
                    adapter.setOnClickListener(new WidgetAddAdapter.OnWidgetClickListener() {
                        @Override
                        public void onClick(AppWidgetProviderInfo awpi) {
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
            }
        });
    }

    private void updateWidgetAvailabilityInfo() {
        hasWidgetMap.clear();
        List<AppWidgetProviderInfo> installedProviders = widgetManager.getInstalledProviders();
        for (AppWidgetProviderInfo awpi : installedProviders) {
            if ((awpi.widgetCategory & AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
                    == AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) {
                hasWidgetMap.put(awpi.provider.getPackageName(), true);
            }
        }
    }

    @Override
    public boolean hasWidget(String packageName) {
        Boolean widgetExists = hasWidgetMap.get(packageName);
        return widgetExists == null ? false : widgetExists;
    }

    public void handleWidgetConfig(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId);
        handleWidgetConfig(appWidgetId, appWidgetInfo);
    }

    public void handleWidgetConfig(int appWidgetId, AppWidgetProviderInfo awpi) { //Only occurs on first add
        AppWidgetProviderInfo appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId);
        if (awpi.configure != null) { //Must configure the widget
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CONFIG_WIDGET);
        } else {
            saveWidget(appWidgetId, awpi);
            replaceDrawerWidget(appWidgetId, awpi);
            expandWidgetDrawer();
            setAndAnimateToDarkBackgroundTint();
            hideTopElements();
            hideBottomElements();
        }
    }

    private void saveWidget(int appWidgetId, AppWidgetProviderInfo awpi){
        widgetIdsWithInfo.add(new Pair<>(appWidgetId, awpi));
        persistWidgets();

        Log.d(TAG, "Saved widget for package " + awpi.provider.getPackageName());
    }

    /**
     * Replace the widget in the widget drawer with the given widget.
     *
     * @param appWidgetId   The ID for the widget allocated by the system.
     * @param awpi          The AppWidgetProviderInfo passed to us by the system.
     */
    public boolean replaceDrawerWidget(final int appWidgetId, final AppWidgetProviderInfo awpi) {
        try {
            Log.d(TAG, "Replacing drawer widget with: " + awpi.provider.getPackageName() + "; defaultHeight=" + awpi.minHeight + "; resizeMode(3=BOTH,VERTICAL=2)=" + awpi.resizeMode + "; minResizeHeight=" + awpi.minResizeHeight);

            widgetContainer.removeAllViews();

            final AppWidgetHostView hostView = widgetHost.createView(this, appWidgetId, awpi);

            int desiredHeight = (int) getResources().getDimension(R.dimen.desired_width_height);
            int width = widgetContainer.getWidth();

            //This one-liner tells us how to size the widget vertically (in pixels)
            int height = (int) awpi.minHeight; //The docs *claim* minHeight is given in DPs -- this is not true; it's actually in px
            if(height > desiredHeight) height = desiredHeight; //Force to 180dp if too big
            if(awpi.resizeMode == AppWidgetProviderInfo.RESIZE_BOTH || awpi.resizeMode == AppWidgetProviderInfo.RESIZE_VERTICAL){
                int minResizeHeight = (int) awpi.minResizeHeight; //Same deal as minHeight -- seriously guys?
                if(minResizeHeight < height){ //We can bound it to the max height
                    height = desiredHeight;
                }
            }

            hostView.updateAppWidgetSize(null, (int) Utilities.convertPixelsToDp(width, this), (int) Utilities.convertPixelsToDp(height, this), (int) Utilities.convertPixelsToDp(width, this), (int) Utilities.convertPixelsToDp(height, this));
            hostView.setLayoutParams(new FrameLayout.LayoutParams(width, height));

            widgetContainer.addView(hostView);
            currentWidget = appWidgetId;

            return true;
        } catch (Exception e){
            Log.e(TAG, "Error adding widget with ID " + appWidgetId, e);

            collapseWidgetDrawer();
            deleteWidget(appWidgetId);

            return false;
        }
    }

    private void deleteWidget(final int appWidgetId){
        //(1) Remove from list
        //please save us java 1.8
        Pair<Integer, AppWidgetProviderInfo> toRemove = null;
        for(Pair<Integer, AppWidgetProviderInfo> p : widgetIdsWithInfo){
            if(p.first == appWidgetId){
                toRemove = p;
                break;
            }
        }
        if(toRemove != null) widgetIdsWithInfo.remove(toRemove);

        //(2) Update database
        persistWidgets();
    }
    //endregion

    //region Help/getting started
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

    private void showUsageMessage() {
        writer.putBoolean(Constants.HAS_REQUESTED_USAGE_PERMISSION_PREF, true).commit();
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
    //endregion

    //region Adjust visibility/position of UI elements
    @Override
    public void showTopElements() {
        showDateTime();
    }

    @Override
    public void hideTopElements() {
        hideDateTime();
    }

    @Override
    public void showBottomElements() {
        showDock();
    }

    @Override
    public void hideBottomElements() {
        hideDock();
    }

    private void showDateTime() {
        ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(timeDateContainer, "translationY", 0);
        leavingAnim.setDuration(DEFAULT_ANIMATION_DURATION);
        leavingAnim.start();
    }

    private void hideDateTime() {
        //TODO: The +100 is a little sloppy
        ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(timeDateContainer, "translationY", -(timeDateContainer.getHeight() + 100));
        leavingAnim.setDuration(DEFAULT_ANIMATION_DURATION);
        leavingAnim.start();
    }

    public void clearBackgroundTint() {
        ObjectAnimator.ofFloat(bigTint, "alpha", 0)
                .setDuration(DEFAULT_ANIMATION_DURATION)
                .start();
    }

    public void setAndAnimateToDarkBackgroundTint() {
        bigTint.setBackgroundColor(Color.parseColor("#a0000000"));
        ObjectAnimator.ofFloat(bigTint, "alpha", 1)
                .setDuration(DEFAULT_ANIMATION_DURATION)
                .start();
    }

    public void showDock() {
        ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(dockBar, "translationY",
                dockBar.getHeight(), 0);
        leavingAnim.setDuration(DEFAULT_ANIMATION_DURATION);
        leavingAnim.start();
    }

    private void hideDock() {
        ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(dockBar, "translationY",
                0, dockBar.getHeight());
        leavingAnim.setDuration(DEFAULT_ANIMATION_DURATION);
        leavingAnim.start();
    }

    private void showDropMenuFast() {
        Log.d(TAG, "Showing drop menu!");

        DisplayMetrics dm = getResources().getDisplayMetrics();

        dockbarApps.setTranslationX(-dm.widthPixels);
        dropLayout.setTranslationX(0);
    }

    private void showDockApps() {
        DisplayMetrics dm = getResources().getDisplayMetrics();

        ObjectAnimator enteringAnim = ObjectAnimator.ofFloat(dockbarApps, "translationX",
                0);
        enteringAnim.setDuration(DEFAULT_ANIMATION_DURATION);
        enteringAnim.start();

        ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(dropLayout, "translationX",
                getResources().getDisplayMetrics().widthPixels);
        leavingAnim.setDuration(DEFAULT_ANIMATION_DURATION);
        leavingAnim.start();
    }

    /**
     * @return The bottom of the interactable UI.
     */
    private int getNaturalBottom(){
        return sgv.getTop() + sgv.getHeight();
    }

    /**
     * The absolute bottom of the UI.
     */
    private int getAbsoluteBottom(){
        return getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * The top of the interactable UI (i.e. just below status bar).
     */
    private int getNaturalTop(){
        return sgv.getTop(); //TODO: Is this right? In theory, this should be ~80
    }

    /**
     * The absolute top of the UI (i.e. behind the status bar).
     */
    private int getAbsoluteTop(){
        return 0;
    }

    private void toggleDropMenu(boolean visible) {
        final DisplayMetrics dm = getResources().getDisplayMetrics();

        if (visible) { //Show drop menu
            ObjectAnimator enteringAnim = ObjectAnimator.ofFloat(dropLayout, "translationX", 0);
            enteringAnim.setDuration(400);
            enteringAnim.start();

            ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(dockbarApps, "translationX",
                    dm.widthPixels);
            leavingAnim.setDuration(400);
            leavingAnim.start();
        } else { //Show dock
            ObjectAnimator enteringAnim = ObjectAnimator.ofFloat(dockbarApps, "translationX", 0);
            enteringAnim.setDuration(400);
            enteringAnim.start();

            ObjectAnimator leavingAnim = ObjectAnimator.ofFloat(dropLayout, "translationX", dm.widthPixels);
            leavingAnim.setDuration(400);
            leavingAnim.start();
        }
    }
    //endregion

    //region Clock
    void updateDisplay() {
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
                            date.setText(getString(R.string.charging_format,
                                    status.getIntExtra(BatteryManager.EXTRA_LEVEL, 50),
                                    sdfDay.format(cal.getTime()),
                                    sdfMonth.format(cal.getTime()),
                                    sdfDayOfMonth.format(cal.getTime())));
                        } else {
                            date.setText(sdfDay.format(cal.getTime())
                                    + ", " + sdfMonth.format(cal.getTime())
                                    + " " + sdfDayOfMonth.format(cal.getTime()));
                        }
                    }

                    //Check if there's an alarm set
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    final AlarmManager.AlarmClockInfo aci = am.getNextAlarmClock();

                    if (aci != null && (aci.getTriggerTime() - System.currentTimeMillis() < ONE_DAY_MILLIS)) {
                        alarm.setVisibility(View.VISIBLE);
                        alarm.setText(getString(R.string.next_alarm_at, alarmTime.format(new Date(aci.getTriggerTime()))));
                        alarm.setOnClickListener(new View.OnClickListener() {
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
        };

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            this.runOnUiThread(runnable);
        }
    }

    public void handleDedicatedAppButton(String pref, boolean slideTransition) {
        cachedPref = pref;
        String existingData = reader.getString(pref, "null");
        if (!existingData.equals("null")) {
            Log.d(TAG, "Existing data: " + existingData);
            Gson gson = new Gson();
            try {
                ApplicationIcon ai = gson.fromJson(existingData, ApplicationIcon.class);
                try {
                    Intent appLaunch = new Intent();
                    appLaunch.setClassName(ai.getPackageName(), ai.getActivityName());
                    appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(appLaunch, ActivityOptions.makeCustomAnimation(this, R.anim.search_slide_anim, R.anim.slide_out_right_anim).toBundle());
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
    //endregion

    public void resetState() {
        Log.d(TAG, "resetState");

        final boolean wasShowingWidget = showingWidget;

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Clear search
                    if(wasShowingWidget){
                        collapseWidgetDrawer();
                        showTopElements();
                        showBottomElements();
                        clearBackgroundTint();
                        sgv.resetState();
                    } else {
                        quitSearch();
                        collapseWidgetDrawer();
                        toggleAppsContainer(false);
                        showDockApps();
                        clearBackgroundTint();
                        showDateTime();
                    }
                } catch (Exception e) { //May fail at boot due to uncreated UI
                    Log.d(TAG, "Exception: " + e);
                    Log.d(TAG, "Message: " + e.getMessage());
                }
            }
        });
    }
}
