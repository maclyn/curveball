package com.inipage.homelylauncher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class ApplicationHideAdapter extends ArrayAdapter<ApplicationHiderIcon> {
    List<ApplicationHiderIcon> apps;
    Context ctx;
    Map<ApplicationIcon, Drawable> drawableMap;

    private float iconSize;

    public ApplicationHideAdapter(Context context, int resource, List<ApplicationHiderIcon> objects) {
        super(context, resource, objects);
        this.ctx = context;
        this.drawableMap = drawableMap;
        this.apps = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ApplicationHiderIcon ai = apps.get(position);

        RelativeLayout mainView = (RelativeLayout) convertView;
        if(mainView == null)
            mainView = (RelativeLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.application_icon_hidden, parent, false);

        TextView title = (TextView) mainView.findViewById(R.id.appIconName);
        final ImageView hiddenIcon = (ImageView) mainView.findViewById(R.id.appIconHidden);

        //Set title
        title.setText(ai.getName());

        //Set visibility icon
        hiddenIcon.setImageResource(ai.getIsHidden()
                ? R.drawable.ic_visibility_off_white_48dp
                : R.drawable.ic_visibility_white_48dp);

        //Set launch
        mainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ai.setIsHidden(!ai.getIsHidden());
                hiddenIcon.setImageResource(ai.getIsHidden()
                        ? R.drawable.ic_visibility_off_white_48dp
                        : R.drawable.ic_visibility_white_48dp);
            }
        });

        return mainView;
    }

    public List<ApplicationHiderIcon> getApps() {
        return apps;
    }
}
