package smartercardhome.inipage.com.usagehome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class IconChooserActivity extends ActionBarActivity {
    private static final String TAG = "IconChooserActivity";
    Spinner validSources;
    RecyclerView iconList;
    RecyclerView.Adapter iconListAdapter;
    EditText iconSearch;
    List<Pair<String, Integer>> iconData;
    List<String> matchingPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_chooser);
        iconData = new ArrayList<>();

        validSources = (Spinner) this.findViewById(R.id.iconSources);
        iconList = (RecyclerView) this.findViewById(R.id.iconPreviews);
        iconList.setLayoutManager(new GridLayoutManager(this, 5,
                LinearLayoutManager.VERTICAL, false));
        iconSearch = (EditText) this.findViewById(R.id.iconSearch);
        iconSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                if(str != null) updateList(str);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //Query device for icon packs
        final Intent allPacksIntent = new Intent("com.gau.go.launcherex.theme", null);
        List<ResolveInfo> matches = this.getPackageManager().queryIntentActivities(allPacksIntent, 0);
        matchingPackages = new ArrayList<>();
        List<String> names = new ArrayList<>();
        //Add ourselves
        matchingPackages.add(this.getPackageName());
        names.add("Built In Icons");
        //Add matches
        for(ResolveInfo ri : matches){
            matchingPackages.add(ri.activityInfo.packageName);
            String name = (String) ri.loadLabel(this.getPackageManager());
            names.add(name);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        validSources.setAdapter(spinnerAdapter);
        validSources.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Set an a adapter
                setNewAdapter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.icon_chooser_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void setNewAdapter(int i){
        iconData.clear();
        iconList.setAdapter(null);
        iconListAdapter = null;
        new AsyncTask<Integer, Void, IconChooserAdapter>(){
            @Override
            protected IconChooserAdapter doInBackground(Integer... params) {
                //Get resources for other application
                int position = params[0];
                try {
                    String pkg = matchingPackages.get(position);
                    Resources r = getPackageManager().getResourcesForApplication(pkg);
                    int drawableRes = r.getIdentifier("drawable", "xml", pkg);
                    XmlPullParser xrp = null;
                    if(drawableRes != 0){
                        xrp = r.getXml(drawableRes);
                        if(xrp == null) throw new Exception("Xml parser was null");
                    } else {
                        try {
                            AssetManager am = r.getAssets();
                            String[] s = am.list("");
                            for(String str : s){
                                if(str.equals("drawable.xml")){
                                    Log.d(TAG, "Found drawable.xml!");
                                    try {
                                        xrp = am.openXmlResourceParser(str);
                                    } catch (Exception e) {
                                        Log.d(TAG, "Failed to open directly!");
                                        try {
                                            InputStream is = am.open(str);
                                            Log.d(TAG, "Opened as file!");
                                            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                                            factory.setNamespaceAware(true);
                                            xrp = factory.newPullParser();
                                            xrp.setInput(is, null);
                                        } catch (Exception e2){
                                            Log.d(TAG, "Failed to open as file!");
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new Exception("No drawable XML found");
                        }
                    }
                    //int appFilterRes = r.getIdentifier("appfilter", "xml", pkg);

                    //Go through and grab drawable="" components
                    List<Integer> resourceIds = new ArrayList<Integer>();
                    int eventType = xrp.getEventType();

                    while(eventType != XmlPullParser.END_DOCUMENT){
                        if(eventType == XmlPullParser.START_TAG){
                            String val = xrp.getAttributeValue(null, "drawable");
                            if(val != null){
                                int identifier = r.getIdentifier(val, "drawable", pkg);
                                if(identifier != 0){
                                    resourceIds.add(identifier);
                                    String name = r.getResourceEntryName(identifier);
                                    iconData.add(new Pair<>(name, identifier));
                                }
                            }
                        }
                        eventType = xrp.next();
                    }

                    //Create an adapter with the resource IDs and package name
                    IconChooserAdapter ica = new IconChooserAdapter(
                            iconData, pkg,
                            IconChooserActivity.this,
                            r);
                    return ica;
                } catch (Exception e) {
                    Log.d(TAG, "Exception: " + e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(IconChooserAdapter ica){
                if(ica != null){
                    iconListAdapter = ica;
                    iconList.setAdapter(iconListAdapter);
                    iconSearch.setEnabled(true);
                } else {
                    Toast.makeText(IconChooserActivity.this,
                            "Unable to load icons. We probably don't support this theme.", Toast.LENGTH_LONG).show();
                    iconSearch.setEnabled(false);
                }
            }
        }.execute(i);
    }

    public void updateList(String query){
        if(iconData != null && iconData.size() > 0){
            if(query.length() > 0) {
                List<Pair<String, Integer>> forSetting = new ArrayList<>();
                String pkg = matchingPackages.get(validSources.getSelectedItemPosition());
                query = query.toLowerCase(Locale.US).replace(" ", "_");
                Log.d(TAG, "Query: " + query);
                for (Pair<String, Integer> p : iconData) {
                    //Log.d(TAG, "First: " + p.first);
                    if (p.first.contains(query)) {
                        forSetting.add(p);
                    }
                }
                try {
                    IconChooserAdapter ica = new IconChooserAdapter(forSetting, pkg,
                            this, getPackageManager().getResourcesForApplication(pkg));
                    iconListAdapter = ica;
                    iconList.setAdapter(iconListAdapter);
                } catch (Exception e) {
                }
            } else {
                String pkg = matchingPackages.get(validSources.getSelectedItemPosition());
                try {
                    IconChooserAdapter ica = new IconChooserAdapter(iconData, pkg,
                        this, getPackageManager().getResourcesForApplication(pkg));
                    iconListAdapter = ica;
                    iconList.setAdapter(iconListAdapter);
                } catch (Exception e) {
                }
            }
        }
    }
}
