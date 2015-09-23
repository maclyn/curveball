package com.inipage.homelylauncher;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inipage.homelylauncher.icons.IconCache;

import java.util.List;
import java.util.Map;

public class ApplicationIconAdapter extends
        RecyclerView.Adapter<ApplicationIconAdapter.AppIconHolder> {
    List<ApplicationIcon> apps;
    Context ctx;

    private float iconSize;

    public static class AppIconHolder extends RecyclerView.ViewHolder {
        RelativeLayout mainView;
        ImageView icon;
        TextView title;

        public AppIconHolder(RelativeLayout mainView) {
            super(mainView);
            this.mainView = mainView;
            this.title = (TextView) mainView.findViewById(R.id.appIconName);
            this.icon = (ImageView) mainView.findViewById(R.id.appIconImage);
        }
    }

    public ApplicationIconAdapter(List<ApplicationIcon> apps, Context context,
                                  Map<ApplicationIcon, Drawable> drawableMap) {
        this.apps = apps;
        this.ctx = context;

        if (Utilities.isSmallTablet(context)){
            iconSize = Utilities.convertDpToPixel(64, context);
        } else if (Utilities.isLargeTablet(context)) {
            iconSize = Utilities.convertDpToPixel(72, context);
        } else {
            iconSize = Utilities.convertDpToPixel(48, context);
        }
    }

    @Override
    public AppIconHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        RelativeLayout relativeLayout = (RelativeLayout) LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.application_icon, viewGroup, false);
        return new AppIconHolder(relativeLayout);
    }

    //Set up specific customIcon with data
    @Override
    public void onBindViewHolder(AppIconHolder viewHolder, int i) {
        final ApplicationIcon ai = apps.get(i);

        //Set title
        viewHolder.title.setText(ai.getName());

        //Set customIcon
        RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams((int) iconSize,
                (int) iconSize);
        rllp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        viewHolder.icon.setLayoutParams(rllp);
        viewHolder.icon.setTag(ai);
        viewHolder.icon.setImageDrawable(null);

        IconCache.getInstance().setIcon(ai.getPackageName(), ai.getActivityName(), viewHolder.icon);

        //Set launch
        viewHolder.mainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startApp(ai, v.getContext());
            }
        });

        //Set drag option
        viewHolder.mainView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Start drag
                ClipData cd = ClipData.newPlainText("description", "Passing app customIcon");
                v.startDrag(cd, new View.DragShadowBuilder(v.findViewById(R.id.appIconImage)), ai, 0);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    private void startApp(ApplicationIcon ai, Context context){
        try {
            Intent appLaunch = new Intent();
            appLaunch.setClassName(ai.getPackageName(), ai.getActivityName());
            appLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appLaunch);
            if(ctx instanceof HomeActivity){
                ((HomeActivity)ctx).resetState(); //Self explanatory
            }
        } catch (Exception e) {
            Toast.makeText(context, "Couldn't start this app!", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void launchTop(){
        if(apps.size() > 0){
            startApp(apps.get(0), ctx);
        }
    }
}
