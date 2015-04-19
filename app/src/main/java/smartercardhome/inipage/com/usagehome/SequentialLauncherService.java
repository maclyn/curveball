package smartercardhome.inipage.com.usagehome;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SequentialLauncherService extends Service {
    private static final String TAG = "SequentialLauncherServ";

    public SequentialLauncherService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!intent.getExtras().containsKey("row_id")){
            Toast.makeText(this, R.string.no_row_specified, Toast.LENGTH_SHORT).show();
            this.stopSelf();
            return START_NOT_STICKY;
        } else {
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
            int rowId = intent.getIntExtra("row_id", 1);
            Cursor appRow = db.query(DatabaseHelper.TABLE_ROWS, null, DatabaseHelper.COLUMN_ID + "=?",
                    new String[] { String.valueOf(rowId)}, null, null, null);
            if(appRow.moveToFirst()){
                int dataColumn = appRow.getColumnIndex(DatabaseHelper.COLUMN_DATA);
                List<Pair<String, String>> paPairs = new ArrayList<>();

                while(!appRow.isAfterLast()){
                    String data = appRow.getString(dataColumn);

                    String[] pairs = data.split(",");
                    Log.d(TAG, pairs.length + " elements in this");
                    for (int i = 0; i < pairs.length; i++) {
                        String[] packAndAct = pairs[i].split("\\|");
                        paPairs.add(new Pair<>(packAndAct[0], packAndAct[1]));
                    }
                    appRow.moveToNext();
                }

                if(!paPairs.isEmpty()){ //Actually go and launch the file
                    for(Pair<String, String> appToLaunch : paPairs){
                        Intent appLaunch = new Intent();
                        appLaunch.setClassName(appToLaunch.first, appToLaunch.second);
                        appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(appLaunch);
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.unable_to_start_app, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
            appRow.close();
            return START_NOT_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
