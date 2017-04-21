package com.inipage.homelylauncher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.model.Favorite;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.ArrayList;

public class SettingsActivity extends ActionBarActivity {
    private static final int REQUEST_CHOOSE_APPLICATION = 500;
    SharedPreferences reader;
    SharedPreferences.Editor writer;
    String cachedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reader = PreferenceManager.getDefaultSharedPreferences(this);
        writer = reader.edit();

        getSupportActionBar();

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MainFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHOOSE_APPLICATION && resultCode == RESULT_OK){
            ActivityInfo activityInfo = data.resolveActivityInfo(getPackageManager(), 0);
            ApplicationIcon ai = new ApplicationIcon(activityInfo.packageName,
                    (String) activityInfo.loadLabel(getPackageManager()), activityInfo.name);

            //Store data in SharedPreferences
            Gson gson = new Gson();
            writer.putString(cachedPref, gson.toJson(ai)).apply();
        }
    }

    public static class MainFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            findPreference("clock_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    changeApp(Constants.CLOCK_APP_PREFERENCE);
                    return true;
                }
            });
            findPreference("calendar_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    changeApp(Constants.CALENDAR_APP_PREFERENCE);
                    return true;
                }
            });
            findPreference("weather_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    changeApp(Constants.WEATHER_APP_PREFERENCE);
                    return true;
                }
            });
            findPreference("charging_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    changeApp(Constants.CHARGING_APP_PREFERENCE);
                    return true;
                }
            });
            findPreference("low_power_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    changeApp(Constants.LOW_POWER_APP_PREFERENCE);
                    return true;
                }
            });
            findPreference("phone_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    changeApp(Constants.PHONE_APP_PREFERENCE);
                    return true;
                }
            });
            findPreference("attrs").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MaterialDialog.Builder(preference.getContext())
                            .title(R.string.attributions)
                            .content(R.string.attributions_message)
                            .negativeText(R.string.close)
                            .show();
                    return true;
                }
            });
            findPreference("log_show").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(preference.getContext())
                            .setTitle(R.string.show_debug_log)
                            .setMessage(Utilities.dumpLog())
                            .setPositiveButton(R.string.export_log, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    cm.setPrimaryClip(ClipData.newPlainText("Export log", Utilities.dumpLog()));

                                    Toast.makeText(getActivity(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                    return true;
                }
            });
            findPreference("log_clear").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Utilities.clearLog();
                    return true;
                }
            });
            findPreference("debug").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), DebugActivity.class));
                    return true;
                }
            });
            findPreference("drop_fav").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    DatabaseEditor.getInstance().saveFavorites(new ArrayList<Favorite>());
                    return true;
                }
            });
        }

        public void changeApp(String pref){
            ((SettingsActivity)getActivity()).cachedPref = pref;
            Utilities.grabActivity(getActivity(), REQUEST_CHOOSE_APPLICATION);
        }
    }
}
