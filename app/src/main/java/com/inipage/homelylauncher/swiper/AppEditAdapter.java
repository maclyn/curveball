package com.inipage.homelylauncher.swiper;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.views.ShortcutGestureView;
import com.mobeta.android.dslv.DragSortListView;

import java.util.List;

public class AppEditAdapter extends ArrayAdapter<Pair<String, String>> implements
        DragSortListView.DragListener, DragSortListView.RemoveListener,
        DragSortListView.DropListener {
    List<Pair<String, String>> objects;

    public AppEditAdapter(Context context, int resource, List<Pair<String, String>> objects) {
        super(context, resource, objects);
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View editRow;
        if(convertView != null){
            editRow = convertView;
        } else {
            editRow = LayoutInflater.from(getContext()).inflate(R.layout.app_edit_row, null);
        }

        //Set the text
        String packageName = getItem(position).first;
        String activityName = getItem(position).second;

        TextView name = (TextView) editRow.findViewById(R.id.appEditName);
        try {
            ActivityInfo ai = getContext().getPackageManager().getActivityInfo(new ComponentName(packageName, activityName), 0);
            String label = (String) ai.loadLabel(getContext().getPackageManager());
            name.setText(label);
        } catch (Exception e) {
            name.setText(packageName);
        }

        //Set icon
        ImageView icon = (ImageView) editRow.findViewById(R.id.appEditIcon);
        icon.setTag(packageName + activityName);
        setEntry(packageName, activityName, icon);
        return editRow;
    }

    @Override
    public void drag(int from, int to) {
        //Not used. We don't worry about the drags until the "drop"
    }

    @Override
    public void remove(int which) {
        if(getCount() > 1) {
            objects.remove(which);
            notifyDataSetChanged();
        } else {
            Toast.makeText(getContext(), R.string.cant_remove_last, Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        }
        redrawGestureView();
    }

    synchronized private void setEntry(final String packageName, final String activity, final ImageView iv){
        new AsyncTask<Void, Void, Drawable>(){
            @Override
            protected Drawable doInBackground(Void... params) {
                Drawable d;
                try {
                    ComponentName cm = new ComponentName(packageName, activity);
                    d = getContext().getPackageManager().getActivityIcon(cm);
                } catch (Exception e) {
                    d = getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                }
                return d;
            }

            @Override
            protected void onPostExecute(Drawable result){
                String s = (String) iv.getTag();
                if(s != null && s.equals(packageName + activity)){
                    iv.setImageDrawable(result);
                }
            }
        }.execute();
    }

    @Override
    public void drop(int from, int to) {
        //Edit underlying data set
        Pair<String, String> element = getItem(from);
        objects.remove(element);
        objects.add(to, element);
        notifyDataSetChanged();
        redrawGestureView();
    }

    private void redrawGestureView(){
        if(getContext() instanceof ShortcutGestureView.ShortcutGestureViewHost){
            ((ShortcutGestureView.ShortcutGestureViewHost) getContext()).invalidateGestureView();
        }
    }
}
