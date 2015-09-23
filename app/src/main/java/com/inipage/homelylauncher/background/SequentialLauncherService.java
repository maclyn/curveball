package com.inipage.homelylauncher.background;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.inipage.homelylauncher.ApplicationClass;
import com.inipage.homelylauncher.DatabaseHelper;
import com.inipage.homelylauncher.R;

import java.util.ArrayList;
import java.util.List;

public class SequentialLauncherService extends Service {
    private static final String TAG = "SequentialLauncherServ";

    public SequentialLauncherService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getExtras().containsKey("row_position")) {
            //Open databases
            DatabaseHelper dh = new DatabaseHelper(this);
            SQLiteDatabase db;
            try {
                db = dh.getWritableDatabase();
            } catch (Exception e) {
                Toast.makeText(this, R.string.failed_to_open_database, Toast.LENGTH_LONG).show();
                this.stopSelf();
                return START_NOT_STICKY;
            }

            //Load row
            int rowId = intent.getIntExtra("row_position", 1);
            Cursor appRow = db.query(DatabaseHelper.TABLE_ROWS, null, DatabaseHelper.COLUMN_ORDER + "=?",
                    new String[]{String.valueOf(rowId)}, null, null, null);
            if (appRow.moveToFirst()) {
                int dataColumn = appRow.getColumnIndex(DatabaseHelper.COLUMN_DATA);
                List<Pair<String, String>> paPairs = new ArrayList<>();

                while (!appRow.isAfterLast()) {
                    String data = appRow.getString(dataColumn);

                    String[] pairs = data.split(",");
                    Log.d(TAG, pairs.length + " elements in this");
                    for (int i = 0; i < pairs.length; i++) {
                        String[] packAndAct = pairs[i].split("\\|");
                        paPairs.add(new Pair<>(packAndAct[0], packAndAct[1]));
                    }
                    appRow.moveToNext();
                }

                if (!paPairs.isEmpty()) { //Actually go and launch the app(s)
                    //"Cached" paPairs in the application
                    //I'm sorry
                    ((ApplicationClass)getApplication()).storePairs(paPairs);
                    Intent launchApp = new Intent(this, SequentialLauncherService.class);
                    launchApp.putExtra("index_in_group", 0);
                    this.startService(launchApp);
                }
            }
            appRow.close();

            this.stopSelf();
            return START_NOT_STICKY;
        } else if (intent.getExtras().containsKey("index_in_group")) {
            int index = intent.getIntExtra("index_in_group", 0);
            List<Pair<String, String>> appsToLaunch = ((ApplicationClass)getApplication()).getPairs();
            if(index < appsToLaunch.size()){
                Pair<String, String> appToLaunch = appsToLaunch.get(index);
                Intent appLaunch = new Intent();
                appLaunch.setClassName(appToLaunch.first, appToLaunch.second);
                Log.d(TAG, "Launching " + appToLaunch.first + " and " + appToLaunch.second + "...");
                appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(appLaunch);
                    Log.d(TAG, "Launched!");
                } catch (Exception e) {
                    Toast.makeText(this, R.string.unable_to_start_app, Toast.LENGTH_SHORT).show();
                }


                if(index != appsToLaunch.size()-1){
                    //Wait 500ms for the next one
                    Handler h = new Handler();
                    final int next = index + 1;
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent launchApp = new Intent(SequentialLauncherService.this, SequentialLauncherService.class);
                            launchApp.putExtra("index_in_group", next);
                            SequentialLauncherService.this.startService(launchApp);
                        }
                    }, 500);
                    return START_STICKY_COMPATIBILITY;
                } else {
                    this.stopSelf();
                    return START_NOT_STICKY;
                }
            } else {
                this.stopSelf();
                return START_NOT_STICKY;
            }
        } else {
            Toast.makeText(this, R.string.no_row_specified, Toast.LENGTH_SHORT).show();
            this.stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
