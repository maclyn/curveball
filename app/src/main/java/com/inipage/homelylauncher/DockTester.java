package com.inipage.homelylauncher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.inipage.homelylauncher.drawer.ApplicationIcon;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.utils.Utilities;
import com.inipage.homelylauncher.views.DockElement;
import com.inipage.homelylauncher.views.DockView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DockTester extends Activity {
    @Bind(R.id.del)
    DockView del;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dock_tester);
        ButterKnife.bind(this);

        SharedPreferences reader = PreferenceManager.getDefaultSharedPreferences(this);

        List<DockElement> de = new ArrayList<>();
        for(int i = 1; i < 6; i++){
            String existingData = reader.getString("dockbarTarget_" + i, "null");
            Gson gson = new Gson();
            try {
                ApplicationIcon ai = gson.fromJson(existingData, ApplicationIcon.class);
                ComponentName cn = new ComponentName(ai.getPackageName(), ai.getActivityName());
                String label = getPackageManager().getActivityInfo(cn, 0).loadLabel(getPackageManager()).toString();

                de.add(new DockElement(cn, label, i - 1));
            } catch (Exception fallback) {
                de.add(new DockElement(null, null, i - 1));
            }
        }

        del.init(5, de);
    }
}
