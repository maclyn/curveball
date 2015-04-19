package smartercardhome.inipage.com.usagehome;

import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
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
import com.mobeta.android.dslv.DragSortListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("unchecked")
public class HomeActivity extends ActionBarActivity {
    public static final String TAG = "HomeActivity";
    public static final int REQUEST_CHOOSE_ICON = 200;
    public static final int REQUEST_CHOOSE_APPLICATION = 201;
    public static final int REQUEST_ALLOCATE_ID = 202;
    private static final int REQUEST_CONFIG_WIDGET = 203;
    private static final int REQUEST_PICK_APP_WIDGET = 204;

    public static final int HOST_ID = 505;

    public enum DockbarState {
        STATE_HOME, STATE_APPS, STATE_DROP
    }

    DockbarState currentState = DockbarState.STATE_HOME;

    //Apps stuff
    RelativeLayout allAppsContainer;
    RecyclerView allAppsScreen;
    Map<ApplicationIcon, Drawable> drawableMap;
    int cachedHash;

    //Dockbar background
    View dockBar;

    //Dates
    View timeDateContainer;
    TextView date;
    TextView hour;
    TextView minute;
    View timeLayout;
    SimpleDateFormat minutes;
    SimpleDateFormat hours;
    Typeface light;
    Typeface regular;
    Typeface condensed;
    Timer timer;

    //Main widget host
    FrameLayout homeWidget;
    boolean addingHomescreenWidget = false;

    //Home dockbar
    LinearLayout dockbarApps;
    ImageView db1;
    ImageView db2;
    ImageView db3;
    ImageView db4;
    ImageView db5;

    //Search/menu
    RelativeLayout searchActionBar;
    ImageView backToHome;
    ImageView allAppsMenu;
    ImageView clearSearch;
    EditText searchBox;

    //Drop layout
    LinearLayout dropLayout;
    //App drop layout
    RelativeLayout appDropLayout;
    View addToDock;
    View uninstallApp;
    View appInfo;

    //Database saving/loading
    DatabaseHelper dh;
    SQLiteDatabase db;

    //A view which exists just to listen to respond to drop events properly
    View dragListener;

    //A view for fading behind nav
    View bigTint;

    //A view for catching strange touches
    View strayTouchCatch;

    //GestureView for opening/closing apps
    ShortcutGestureView sgv;

    //Snacklet stuff
    View snackletBar;
    TextView noSnackletView;
    LinearLayout snackletContainer;
    LinearLayout snackletHint;
    TextView clearSnacklets;

    BroadcastReceiver snackletReceiver;
    List<UpdateItem> updates;
    List<String> handledPackages;

    //Widget stuff
    View widgetBar;
    Toolbar widgetToolbar;
    LinearLayout widgetContainer;

    boolean editingWidget = false;

    AppWidgetManager widgetManager;
    AppWidgetHost widgetHost;

    //Settings
    SharedPreferences reader;
    SharedPreferences.Editor writer;

    List<TypeCard> samples;
    List<WidgetContainer> widgets;

    View v;
    String packageName;
    String resourceName;
    String cachedPref;

    //Tell when things have changed
    BroadcastReceiver packageReceiver;
    BroadcastReceiver storageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        timeLayout = this.findViewById(R.id.timeLayout);
        date = (TextView) this.findViewById(R.id.date);
        date.setTypeface(condensed);
        hour = (TextView) this.findViewById(R.id.hour);
        minute = (TextView) this.findViewById(R.id.minute);
        ((TextView) this.findViewById(R.id.timeColon)).setTypeface(light);
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
        timeDateContainer = this.findViewById(R.id.timeDateContainer);
        homeWidget = (FrameLayout) this.findViewById(R.id.homeWidget);

        samples = new ArrayList<>();
        widgets = new ArrayList<>();

        dockBar = this.findViewById(R.id.dockBar);
        bigTint = this.findViewById(R.id.statusBarBackdrop);

        //Set up all apps button
        allAppsContainer = (RelativeLayout) this.findViewById(R.id.allAppsContainer);
        allAppsScreen = (RecyclerView) this.findViewById(R.id.allAppsLayout);
        drawableMap = new HashMap<>();
        cachedHash = -1;
        GridLayoutManager glm = new GridLayoutManager(this, 4);
        allAppsScreen.setLayoutManager(glm);
        resetAppsList("");

        //Set up dockbar apps
        dockbarApps = (LinearLayout) this.findViewById(R.id.dockApps);
        db1 = (ImageView) dockbarApps.findViewById(R.id.dockApp1);
        db2 = (ImageView) dockbarApps.findViewById(R.id.dockApp2);
        db3 = (ImageView) dockbarApps.findViewById(R.id.dockApp3);
        db4 = (ImageView) dockbarApps.findViewById(R.id.dockApp4);
        db5 = (ImageView) dockbarApps.findViewById(R.id.dockApp5);
        setupDockbarTarget(db1, 1);
        setupDockbarTarget(db2, 2);
        setupDockbarTarget(db3, 3);
        setupDockbarTarget(db4, 4);
        setupDockbarTarget(db5, 5);

        sgv = (ShortcutGestureView) this.findViewById(R.id.sgv);
        sgv.setActivity(this);

        //Search action bar
        strayTouchCatch = this.findViewById(R.id.strayTouchShield);
        strayTouchCatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do nothing
            }
        });
        searchActionBar = (RelativeLayout) this.findViewById(R.id.searchActionBar);
        backToHome = (ImageView) searchActionBar.findViewById(R.id.backToHome);
        backToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAppsContainer(false);
                hideKeyboard();
                setDockbarState(DockbarState.STATE_HOME, true);
            }
        });
        allAppsMenu = (ImageView) searchActionBar.findViewById(R.id.moreOptions);
        allAppsMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pm = new PopupMenu(v.getContext(), v);
                pm.inflate(R.menu.popup_menu);
                if(!reader.getBoolean(Constants.HOME_WIDGET_PREFERENCE, false))
                    pm.getMenu().findItem(R.id.changeHomeWidget).setVisible(false);
                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.help:
                                Toast.makeText(HomeActivity.this, "Um.", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.settings:
                                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                                break;
                            case R.id.changeHomeWidget:
                                addingHomescreenWidget = true;
                                showAddWidgetMenu();
                                break;
                            case R.id.changeWallpaper:
                                toggleAppsContainer(false);
                                setDockbarState(DockbarState.STATE_HOME, true);
                                final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
                                startActivity(Intent.createChooser(pickWallpaper, "Set Wallpaper"));
                                break;
                            case R.id.editRows:
                                editRows();
                                break;
                        }
                        return true;
                    }
                });
                pm.show();
            }
        });
        clearSearch = (ImageView) searchActionBar.findViewById(R.id.clearSearch);
        clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBox.setText("");
                hideKeyboard();
            }
        });
        searchBox = (EditText) searchActionBar.findViewById(R.id.searchBox);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String change = s.toString();
                resetAppsList(change);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if(actionId == EditorInfo.IME_ACTION_GO){
                    //Try and open app
                    ((ApplicationIconAdapter)allAppsScreen.getAdapter()).launchTop();
                }
                return handled;
            }
        });

        //Set drag type of dockbar
        dropLayout = (LinearLayout) this.findViewById(R.id.dropLayout);
        appDropLayout = (RelativeLayout) dropLayout.findViewById(R.id.appDropIcons);
        addToDock = appDropLayout.findViewById(R.id.addToDock);
        uninstallApp = appDropLayout.findViewById(R.id.uninstallApp);
        appInfo = appDropLayout.findViewById(R.id.appInfo);

        uninstallApp.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    ApplicationIcon ai = (ApplicationIcon) event.getLocalState();
                    try {
                        Uri uri = Uri.parse("package:" + ai.getPackageName());
                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                        v.getContext().startActivity(uninstallIntent);
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(),
                                "The app is unable to be removed.", Toast.LENGTH_LONG).show();
                    }
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
                        uninstallIntent.setData(uri);
                        v.getContext().startActivity(uninstallIntent);
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(),
                                "The app does not have info.", Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            }
        });
        addToDock.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                    setDockbarState(DockbarState.STATE_HOME, true);
                }
                if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                }
                return true;
            }
        });

        //Listener for drags on the homescreen
        dragListener = this.findViewById(R.id.dropListener);
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

        //Set up snacklets
        handledPackages = new ArrayList<>();
        updates = new ArrayList<>();
        snackletBar = findViewById(R.id.snackletsBar);
        noSnackletView = (TextView) findViewById(R.id.noSnackletsFound);
        snackletContainer = (LinearLayout) findViewById(R.id.snackletContainer);
        snackletHint = (LinearLayout) findViewById(R.id.snackletsHint);
        clearSnacklets = (TextView) findViewById(R.id.clearSnacklets);
        clearSnacklets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAllSnacklets();
            }
        });

        //Set up receiver for getting data back
        snackletReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Found some new data!");
                try {
                    UpdateItem ui = new UpdateItem(context, intent);
                    snackletAdded(ui, -1);
                } catch (Exception e) {
                    boolean isDebugging = reader.getBoolean(Constants.DEBUG_MODULES, false);
                    Log.d(TAG, "Error! Message: " + e.getMessage());
                    if(intent.hasExtra("sender_package")){
                        try {
                            String error = "Error from package: " + intent.getStringExtra("sender_package");
                            if(isDebugging){
                                new MaterialDialog.Builder(context)
                                        .title(R.string.module_error)
                                        .content(error)
                                        .show();
                            } else {
                                Log.d(TAG, error);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.FOUND_DATA_INTENT);

        //Register when we get new data
        registerReceiver(snackletReceiver, intentFilter);

        //Set up widgets
        widgetBar = findViewById(R.id.widgetBar);
        widgetToolbar = (Toolbar) this.findViewById(R.id.widgetToolbar);
        widgetContainer = (LinearLayout) this.findViewById(R.id.widgetContainer);
        widgetToolbar.inflateMenu(R.menu.widget_menu);
        widgetToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.editWidgets:
                        toggleWidgetEditing();
                        break;
                    case R.id.addWidget:
                        showAddWidgetMenu();
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
            writer.putBoolean(Constants.HAS_RUN_PREFERENCE, true).apply();
            sgv.setCards(new ArrayList<TypeCard>());
        } else {
            loadList(samples);
            loadWidgets(widgets);
        }

        //Set up broadcast receivers
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(searchBox != null) searchBox.setText("");
                resetAppsList("");
                verifyWidgets();
            }
        };

        storageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(searchBox != null) searchBox.setText("");
                resetAppsList("");
                verifyWidgets();
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(storageReceiver, filter);
    }

    private void showAddWidgetMenu() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            List<AppWidgetProviderInfo> installedProviders = widgetManager.getInstalledProviders();
            List<AppWidgetProviderInfo> matchingProviders = new ArrayList<>();
            for (AppWidgetProviderInfo awpi : installedProviders) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if ((awpi.widgetCategory & AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
                            == AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) {
                        matchingProviders.add(awpi);
                    }
                } else {
                    matchingProviders.add(awpi);
                }
            }

            //Sort by label
            Collections.sort(matchingProviders, new Comparator<AppWidgetProviderInfo>() {
                @Override
                public int compare(AppWidgetProviderInfo lhs, AppWidgetProviderInfo rhs) {
                    return lhs.label.compareToIgnoreCase(rhs.label);
                }
            });

            final WidgetAddAdapter adapter = new
                    WidgetAddAdapter(HomeActivity.this, R.layout.widget_preview, matchingProviders);

            final MaterialDialog md = new MaterialDialog.Builder(HomeActivity.this)
                    .adapter(adapter)
                    .title("Choose a Widget")
                    .negativeText("Cancel")
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
        } else {
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            int appWidgetId = widgetHost.allocateAppWidgetId();
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<>();
            pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
            ArrayList<Bundle> customExtras = new ArrayList<>();
            pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
            startActivityForResult(pickIntent, REQUEST_PICK_APP_WIDGET);
        }
    }

    private synchronized void removeAllSnacklets() {
        updates.clear();
        handledPackages.clear();
        snackletHint.removeAllViews();
        snackletContainer.removeAllViews();

        clearSnacklets.setVisibility(View.GONE);
        noSnackletView.setVisibility(View.VISIBLE);
    }

    private synchronized void snackletAdded(UpdateItem snacklet, int position){
        noSnackletView.setVisibility(View.GONE);
        clearSnacklets.setVisibility(View.VISIBLE);

        String dataTag = snacklet.getSenderPackage() + snacklet.getDataType();
        if(handledPackages.contains(dataTag)){
            //Code to replace the existing View and snacklet in updates
            int indexOfSnacklet = -1;
            for(int i = 0; i < updates.size(); i++){
                if((updates.get(i).getSenderPackage() + updates.get(i).getDataType()).equals(dataTag)){
                    indexOfSnacklet = i;
                    break;
                }
            }

            if(indexOfSnacklet != -1){
                updates.remove(indexOfSnacklet);
                snackletContainer.removeViewAt(indexOfSnacklet);
                snackletHint.removeViewAt(indexOfSnacklet);
                handledPackages.remove(dataTag);
                snackletAdded(snacklet, indexOfSnacklet);
            } else { //Error case
                handledPackages.remove(dataTag);
                snackletAdded(snacklet, -1);
            }
        } else {
            //Brand new; we can handle this naively
            handledPackages.add(dataTag);
            View snackletView = generateSnackletView(snacklet);
            setSnackletView(snackletView, snacklet, snacklet.selectedPage);
            View snackletHintRow = generateSnackletHint(snacklet);

            //Add to updates, hint, and screen

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) Utilities.convertDpToPixel(24, this),
                    (int) Utilities.convertDpToPixel(24, this));
            lp.setMargins(0, (int) Utilities.convertDpToPixel(8, this), 0, (int) Utilities.convertDpToPixel(8, this));
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp2.setMargins(0, (int) Utilities.convertDpToPixel(12, this), 0, (int) Utilities.convertDpToPixel(12, this));
            if(position == -1) { //Tack it on the end
                updates.add(snacklet);
                snackletContainer.addView(snackletView, lp2);
                snackletHint.addView(snackletHintRow, lp);
            } else { //We have a position to handle this with
                updates.add(position, snacklet);
                snackletContainer.addView(snackletView, position, lp2);
                snackletHint.addView(snackletHintRow, position, lp);
            }
        }

        Log.d(TAG, "New size of updates: " + updates.size());
    }

    private View generateSnackletView(final UpdateItem snacklet){
        View v = View.inflate(this, R.layout.snacklet_full_layout, null);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snacklet.selectedPage++;
                if(snacklet.selectedPage == snacklet.getPages().size()){
                    snacklet.selectedPage = 0;
                }
                setSnackletView(v, snacklet, snacklet.selectedPage);
            }
        });
        v.findViewById(R.id.openSnacklet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open what's indicated by snacklet
                openSnacklet(snacklet);
            }
        });
        if(snacklet.getPages().size() == 1){
            v.findViewById(R.id.snackletGreaterThanOne).setVisibility(View.GONE);
        }
        return v;
    }

    private View generateSnackletHint(UpdateItem snacklet){
        ImageView v = (ImageView) LayoutInflater.from(this).inflate(R.layout.snacklet_hint_row,
                snackletHint, false);
        v.setImageDrawable(snacklet.getHintIcon());
        return v;
    }

    private void openSnacklet(UpdateItem s){
        String data = s.getPages().get(s.selectedPage).getUrl();
        try {
            if (data.contains("url:")) {
                String url = data.replace("url:", "");
                Uri urlUri = Uri.parse(url);
                Intent i = new Intent();
                i.setAction(Intent.ACTION_VIEW);
                i.setData(urlUri);
                this.startActivity(i);
            } else if (data.contains("app:")) {
                String trimmed = data.replace("app:", "");
                String appPackage = trimmed.substring(0, trimmed.indexOf(";"));
                String appClass = trimmed.substring(trimmed.indexOf(";") + 1);
                Intent i = new Intent();
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setComponent(new ComponentName(appPackage, appClass));
                this.startActivity(i);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e);
            Toast.makeText(this, R.string.invalid_launch_specified, Toast.LENGTH_SHORT).show();
        }
    }

    private void setSnackletView(View view, UpdateItem snacklet, int position){
        //Set icon
        ((ImageView)view.findViewById(R.id.snackletIcon)).setImageDrawable(snacklet.getPages().get(position).getIcon());

        //Set text
        ((TextView)view.findViewById(R.id.snackletText)).setText(snacklet.getPages().get(position).getTitle());

        //Set description
        ((TextView)view.findViewById(R.id.snackletDesc)).setText(snacklet.getPages().get(position).getDescription());

        //Set open button
        if(snacklet.getPages().get(position).getUrl().contains("url:")){
            ((ImageView)view.findViewById(R.id.openSnacklet)).setImageResource(R.drawable.ic_open_in_browser_white_48dp);
        } else {
            ((ImageView)view.findViewById(R.id.openSnacklet)).setImageResource(R.drawable.ic_open_in_new_white_48dp);
        }
    }

    private void editRows() {
        Log.d(TAG, "Editing rows...");

        if(samples.size() == 0){
            Toast.makeText(this, R.string.no_rows, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[samples.size()];
        for(int i = 0; i < samples.size(); i++){
            items[i] = samples.get(i).getTitle();
        }

        new MaterialDialog.Builder(this)
                .title(R.string.edit_rows)
                .items(items)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog,
                                            View view, int i, CharSequence charSequence) {
                        showEditFolderDialog(i);
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        //Show re-order row dialog
                        showReorderRowsDialog();
                    }
                })
                .neutralText(R.string.reorder_rows)
                .negativeText(R.string.cancel)
                .show();
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
                getPackageManager()
                        .getResourcesForApplication(
                                widgetManager.getAppWidgetInfo(wc.getWidgetId())
                                        .provider.getPackageName());
            } catch (Exception e){
                Log.d(TAG, wc.getWidgetId() + " @ height " + wc.getWidgetHeight() +
                        " isn't installed...");
                toRemove.add(wc);
            }
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(WidgetContainer wc : toRemove){
                    Log.d(TAG, "Removing " + wc.getWidgetId());
                    int removalIndex = widgets.indexOf(wc);
                    if(removalIndex != -1){
                        widgets.remove(removalIndex);
                        if(removalIndex < widgetContainer.getChildCount())
                            widgetContainer.removeViewAt(removalIndex);
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
        if(snackletReceiver != null) {
            this.unregisterReceiver(snackletReceiver);
        }
    }

    private void toggleWidgetEditing() {
        editingWidget = !editingWidget;
        for (int i = 0; i < widgetContainer.getChildCount(); i++) {
            try {
                widgetContainer.getChildAt(i).findViewById(R.id.editView)
                        .setVisibility(editingWidget ? View.VISIBLE : View.GONE);

                if(editingWidget) {
                    AppWidgetProviderInfo awpi = widgetManager.getAppWidgetInfo(widgets.get(i).getWidgetId());
                    if ((awpi.resizeMode == AppWidgetProviderInfo.RESIZE_NONE ||
                            awpi.resizeMode == AppWidgetProviderInfo.RESIZE_HORIZONTAL) &&
                            !reader.getBoolean(Constants.RESIZE_ALL_PREFERENCE, false)) {
                        widgetContainer.getChildAt(i).findViewById(R.id.resizeWidget).setVisibility(View.GONE); //Disable because we can't resize
                    } else {
                        widgetContainer.getChildAt(i).findViewById(R.id.resizeWidget).setVisibility(View.VISIBLE);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if(editingWidget){
            widgetToolbar.getMenu().findItem(R.id.editWidgets).setIcon(R.drawable.ic_check_white_48dp);
        } else {
            widgetToolbar.getMenu().findItem(R.id.editWidgets).setIcon(R.drawable.ic_create_white_48dp);
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        //Ask for snacklet data
        Intent getData = new Intent();
        getData.setAction(Constants.WAKEUP_INTENT);
        sendBroadcast(getData);

        //Update most parts on timer
        TimerTask t = new TimerTask(){
            @Override
            public void run() {
                updateDisplay();
            }
        };

        //Set the homescreen widget if it's valid
        if(reader.getBoolean(Constants.HOME_WIDGET_PREFERENCE, false)) {
            int newWidgetId = reader.getInt(Constants.HOME_WIDGET_ID_PREFERENCE, -1);
            if (newWidgetId != -1 && homeWidget.getChildCount() < 1)
                addWidget(newWidgetId, homeWidget.getHeight(), true);
        } else {
            //Ensure there aren't any "attached" remnants
            writer.putInt(Constants.HOME_WIDGET_ID_PREFERENCE, -1).commit();
            homeWidget.removeAllViews();
            timeDateContainer.setVisibility(View.VISIBLE);
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(t, 0, 1000);
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

                    //Hide widgets
                    if (widgetBarIsVisible()) collapseWidgetBar(true, false, 300);

                    //Hide snacklets
                    if (snackletBarIsVisible()) collapseSnackletBar(true, false, 300);

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

            ((ImageButton)v.findViewById(R.id.chooseIcon)).setImageDrawable(d);
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

    public void addWidget(int appWidgetId, int height, boolean toHome){
        try {
            addWidget(appWidgetId, widgetManager.getAppWidgetInfo(appWidgetId), height, toHome);
        } catch (Exception e) {
            Log.d(TAG, "Failed to add widget; removing from DB");
            if(toHome) {
                writer.putBoolean(Constants.HOME_WIDGET_PREFERENCE, false).commit();
                writer.putInt(Constants.HOME_WIDGET_ID_PREFERENCE, -1).commit();
            } else {
                WidgetContainer wc = findContainer(appWidgetId);
                if (wc != null) {
                    widgets.remove(wc);
                }
            }
        }
    }

    public void addWidget(int appWidgetId, AppWidgetProviderInfo awpi){
        addWidget(appWidgetId, awpi, -1, false);
    }

    public void addWidget(final int appWidgetId, final AppWidgetProviderInfo awpi, int defaultHeight,
                          boolean toHome){
        if(addingHomescreenWidget){
            writer.putInt(Constants.HOME_WIDGET_ID_PREFERENCE, appWidgetId).commit();
            toHome = true;
        }

        final AppWidgetHostView hostView = widgetHost.createView(this, appWidgetId, awpi);
        hostView.setAppWidget(appWidgetId, awpi);

        //Set to minHeight
        int resultDp;
        int width;
        if(!toHome){
            width = (int) Utilities.convertPixelsToDp(widgetBar.getWidth(), this);
        } else {
            width = (int) Utilities.convertPixelsToDp(homeWidget.getWidth(), this);
        }

        if(defaultHeight == -1){
            resultDp = awpi.minHeight;
        } else { //Use
            resultDp = defaultHeight;
        }
        int resultPx = (int) Utilities.convertDpToPixel(resultDp, this);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            hostView.updateAppWidgetSize(null, width, resultDp, width, resultDp);
        }

        hostView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                resultPx));

        if(toHome){ //Hide time and date because there's a widget here
            Log.d(TAG, "Exiting addWidget early to handle new widget...");
            homeWidget.removeAllViews();
            homeWidget.addView(hostView);
            timeDateContainer.setVisibility(View.GONE);
            addingHomescreenWidget = false;
            return;
        }

        final View bigHost = getLayoutInflater().inflate(R.layout.widget_host, null);
        ((LinearLayout)bigHost.findViewById(R.id.widgetHostContainer)).addView(hostView);

        bigHost.findViewById(R.id.resizeWidget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResizeDialog(appWidgetId, hostView);
            }
        });
        if((awpi.resizeMode == AppWidgetProviderInfo.RESIZE_NONE ||
                awpi.resizeMode == AppWidgetProviderInfo.RESIZE_HORIZONTAL) &&
                !reader.getBoolean(Constants.RESIZE_ALL_PREFERENCE, false)){
            bigHost.findViewById(R.id.resizeWidget).setVisibility(View.GONE); //Disable because we can't resize
        }

        bigHost.findViewById(R.id.deleteWidget).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                widgetHost.deleteAppWidgetId(appWidgetId);
                widgetContainer.removeView(bigHost);
                WidgetContainer wc = findContainer(appWidgetId);
                if (wc != null) {
                    widgets.remove(wc);
                }
                Toast.makeText(HomeActivity.this, "Deleted.", Toast.LENGTH_SHORT).show();
            }
        });
        widgetContainer.addView(bigHost);

        if(!editingWidget) bigHost.findViewById(R.id.editView).setVisibility(View.GONE); //Hide for now

        //Add it to the widget list if needed
        if(defaultHeight == -1) {
            widgets.add(new WidgetContainer(appWidgetId, resultDp));
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

        try {
            int test = awpi.minResizeHeight;
        } catch (Exception e) { //Error condition; find widget and remove
            int removeIndex = -1;
            for(int i = 0; i < widgets.size(); i++){
                if(widgets.get(i).getWidgetId() == appWidgetId) removeIndex = i;
            }
            if(removeIndex != -1){
                widgets.remove(removeIndex);
                widgetContainer.removeViewAt(removeIndex);
                persistWidgets(widgets);
            }

            Toast.makeText(this, R.string.widget_no_longer_installed, Toast.LENGTH_SHORT).show();
            return;
        }

        //We know we can resize vertically; get bounds
        final int minHeight = awpi.minResizeHeight <= awpi.minHeight ? awpi.minResizeHeight : awpi.minHeight;
        Log.d(TAG, "Min height (px/dp): " + awpi.minHeight + "/" + minHeight);
        final int maxHeight = (int) Utilities.convertPixelsToDp(widgetBar.getHeight(), this);
        Log.d(TAG, "Max height (px/dp): " + widgetBar.getHeight() + "/" + maxHeight);
        final int width = (int) Utilities.convertPixelsToDp(awhv.getWidth(), this);
        Log.d(TAG, "Max width (dp): " + width);

        View v = getLayoutInflater().inflate(R.layout.widget_resize, null);
        final CheckBox cb = (CheckBox) v.findViewById(R.id.smallerMin);
        final SeekBar sb = (SeekBar) v.findViewById(R.id.heightBar);

        MaterialDialog md = new MaterialDialog.Builder(this)
                .title("Resize " + awhv.getAppWidgetInfo().label)
                .positiveText("Resize")
                .customView(v, false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        int position = sb.getProgress();
                        int max = sb.getMax();
                        float realMin = (cb.isChecked() ?
                                Utilities.convertDpToPixel(30, dialog.getContext()) : minHeight);

                        Log.d(TAG, "Position/Max: " + position + " and " + max);
                        if (max == 0) max = 1;
                        float percentSlid = (float) position / (float) max;
                        Log.d(TAG, "Percent slid: " + percentSlid);
                        float range = maxHeight - realMin;
                        int result = (int) (percentSlid * range);
                        result += realMin;
                        Log.d(TAG, "Range: " + range + " and result: " + result);

                        int resultPx = (int) Utilities.convertDpToPixel((float) result, dialog.getContext());
                        Log.d(TAG, "Result pixels: " + resultPx);

                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                            awhv.updateAppWidgetSize(null, width, result, width, result);
                        }
                        awhv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                resultPx));

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
            homeWidget.setAlpha(to);
            dockbarApps.setAlpha(to);
            bigTint.setAlpha(to);
            snackletHint.setAlpha(to);
        } else {
            ObjectAnimator.ofFloat(timeDateContainer, "alpha", to)
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(homeWidget, "alpha", to)
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(dockbarApps, "alpha", to)
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(snackletHint, "alpha", to)
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(bigTint, "alpha", 1 - to)
                    .setDuration(duration)
                    .start();
        }
    }

    public void expandSnackletBar(boolean animate, long duration){
        snackletBar.setVisibility(View.VISIBLE);

        if(!animate){
            snackletBar.setTranslationX(0);
            snackletHint.setTranslationX(-snackletHint.getWidth());
        } else {
            Log.d(TAG, "Animating widget bar away...");
            ObjectAnimator.ofFloat(snackletBar, "translationX", 0)
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(snackletHint, "translationX", -snackletHint.getWidth())
                    .setDuration(duration)
                    .start();
        }
    }

    public void collapseSnackletBar(boolean animate, boolean fadeDateTime, long duration){
        snackletBar.setVisibility(View.VISIBLE);

        if(!animate){
            snackletBar.setTranslationX(-snackletBar.getWidth());
            snackletHint.setTranslationX(0);
        } else {
            Log.d(TAG, "Animating snacklet away...");
            ObjectAnimator.ofFloat(snackletBar, "translationX", -snackletBar.getWidth())
                    .setDuration(duration)
                    .start();
            ObjectAnimator.ofFloat(snackletHint, "translationX", 0)
                    .setDuration(duration)
                    .start();
        }
    }

    public void expandWidgetBar(boolean animate, long duration){
        widgetBar.setVisibility(View.VISIBLE);

        if(!animate){
            widgetBar.setTranslationX(0);
        } else {
            ObjectAnimator.ofFloat(widgetBar, "translationX", 0)
                    .setDuration(duration)
                    .start();
        }
    }

    public void collapseWidgetBar(boolean animate, boolean fadeDateTime, long duration){
        widgetBar.setVisibility(View.VISIBLE);

        if(!animate){
            widgetBar.setTranslationX(widgetBar.getWidth());
        } else {
            ObjectAnimator.ofFloat(widgetBar, "translationX", widgetBar.getWidth())
                    .setDuration(duration)
                    .start();
        }

        if(editingWidget) toggleWidgetEditing();
    }

    public boolean widgetBarIsVisible(){
        boolean result = widgetBar.getTranslationX() <= 5;
        Log.d(TAG, "Widget bar visible = " + result);
        return result;
    }

    public boolean snackletBarIsVisible(){
        boolean result = snackletBar.getTranslationX() >= -5;
        Log.d(TAG, "Snacklet bar visible = " + result);
        return result;
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

    public void resetAppsList(final String query){
        new AsyncTask<PackageManager, Void, List<ApplicationIcon>>(){
            @Override
            protected List<ApplicationIcon> doInBackground(PackageManager... params) {
                List<ApplicationIcon> applicationIcons = new ArrayList<ApplicationIcon>();

                //Grab all matching applications
                final Intent allAppsIntent = new Intent(Intent.ACTION_MAIN, null);
                allAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                final List<ResolveInfo> packageList = params[0].queryIntentActivities(allAppsIntent, 0);
                String lowerQuery = query.toLowerCase(Locale.getDefault());
                for(ResolveInfo ri : packageList){
                    try {
                        String name = (String) ri.loadLabel(params[0]);
                        if(lowerQuery.equals("") || name.toLowerCase(Locale.getDefault())
                            .contains(lowerQuery))
                            applicationIcons.add(new ApplicationIcon(ri.activityInfo.packageName,
                                    name, ri.activityInfo.name));
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
                        allAppsScreen.setAdapter(new ApplicationIconAdapter(apps, HomeActivity.this,
                                drawableMap));
                        cachedHash = newHash;
                    }
                }
            }
        }.execute(this.getPackageManager());
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
                    List<Pair<String, String>> paPairs = new ArrayList<>();
                    String[] pairs = data.split(",");
                    Log.d(TAG, pairs.length + " elements in this");
                    for (int i = 0; i < pairs.length; i++) {
                        String[] packAndAct = pairs[i].split("\\|");
                        paPairs.add(new Pair<>(packAndAct[0], packAndAct[1]));
                    }
                    TypeCard tc = new TypeCard(title, graphicPackage, graphic, paPairs);
                    samples.add(tc);
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
                    widgets.add(new WidgetContainer(widgetId, size));

                    loadItems.moveToNext();
                }
            } else {
                Log.d(TAG, "No widgets found!");
            }
            loadItems.close();
        }

        //Now go add the widgets
        for(int i = 0; i < widgets.size(); i++){
            addWidget(widgets.get(i).getWidgetId(), widgets.get(i).getWidgetHeight(), false);
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

        v = getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
        v.findViewById(R.id.chooseIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), IconChooserActivity.class);
                startActivityForResult(i, REQUEST_CHOOSE_ICON);
            }
        });
        final EditText et = (EditText) v.findViewById(R.id.chooseName);
        new MaterialDialog.Builder(this)
                .customView(v, true)
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

            v = getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
            v.findViewById(R.id.chooseIcon).setOnClickListener(new View.OnClickListener() {
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

            ((ImageButton) v.findViewById(R.id.chooseIcon)).setImageDrawable(d);
            final EditText et = (EditText) v.findViewById(R.id.chooseName);
            et.setText(sgv.data.get(row).getTitle());
            new MaterialDialog.Builder(this)
                    .customView(v, false)
                    .title("Edit Folder")
                    .positiveText("Save")
                    .neutralText("Reorder")
                    .negativeText("Delete")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onNeutral(MaterialDialog dialog) {
                            showReorderDialog(row);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            sgv.data.remove(row);
                            persistList(samples);
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
                        }
                    }).show();
        }
    }

    public void showReorderDialog(int row){
        DragSortListView dsiv = (DragSortListView) LayoutInflater.from(this).inflate(R.layout.sort_list, null);
        dsiv.setAdapter(new AppEditAdapter(this,
                R.layout.app_edit_row, sgv.data.get(row).getPackages()));
        dsiv.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        new MaterialDialog.Builder(this)
                .customView(dsiv, false)
                .title("Re-order")
                .positiveText("Done")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        persistList(samples);
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
        this.runOnUiThread(new Runnable(){
            public void run(){
                Calendar cal = GregorianCalendar.getInstance();

                //set clock
                if(cal.get(Calendar.HOUR_OF_DAY) > 11){
                    //Bold minutes
                    minute.setTypeface(regular);
                    hour.setTypeface(light);
                } else {
                    //Bold hours
                    minute.setTypeface(light);
                    hour.setTypeface(regular);
                }

                hour.setText(hours.format(cal.getTime()));
                minute.setText(minutes.format(cal.getTime()));

                //Set date
                SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", Locale.US);
                SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM", Locale.US);
                SimpleDateFormat sdfDayOfMonth = new SimpleDateFormat("d", Locale.US);
                //check battery -- if charging, then add percent to left of clock
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent status = registerReceiver(null, filter);
                if(status != null) {
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
            }
        });
    }
}
