package com.inipage.homelylauncher.drawer;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inipage.homelylauncher.HomeActivity;
import com.inipage.homelylauncher.R;
import com.inipage.homelylauncher.icons.IconCache;
import com.inipage.homelylauncher.utils.Utilities;

import java.util.List;

public class ApplicationIconAdapter extends
        RecyclerView.Adapter<ApplicationIconAdapter.AppIconHolder> implements
        HighlightableAdapter {
    List<ApplicationIcon> apps;
    Context ctx;
    private float iconSize;
    private int fourDp;
    private int highlightedItem = -1;

    public static class AppIconHolder extends RecyclerView.ViewHolder {
        RelativeLayout mainView;
        StickyImageView icon;
        TextView title;

        public AppIconHolder(RelativeLayout mainView) {
            super(mainView);
            this.mainView = mainView;
            this.title = (TextView) mainView.findViewById(R.id.appIconName);
            this.icon = (StickyImageView) mainView.findViewById(R.id.appIconImage);
        }
    }

    public ApplicationIconAdapter(List<ApplicationIcon> apps, Context context) {
        this.apps = apps;
        this.ctx = context;

        if (Utilities.isSmallTablet(context)){
            iconSize = Utilities.convertDpToPixel(64, context);
        } else if (Utilities.isLargeTablet(context)) {
            iconSize = Utilities.convertDpToPixel(72, context);
        } else {
            iconSize = Utilities.convertDpToPixel(48, context);
        }

        fourDp = (int) Utilities.convertDpToPixel(4, context);
    }

    @Override
    public AppIconHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        RelativeLayout relativeLayout = (RelativeLayout) LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.application_icon, viewGroup, false);
        return new AppIconHolder(relativeLayout);
    }

    //Set up specific customIcon with data
    @Override
    public void onBindViewHolder(final AppIconHolder viewHolder, int i) {
        final ApplicationIcon ai = apps.get(i);

        //Set title
        viewHolder.title.setText(ai.getName());

        //TODO: Debug highlighting code
        /*
        if(highlightedItem == i){
            viewHolder.title.setTypeface(viewHolder.title.getTypeface(), Typeface.BOLD);
        } else {
            viewHolder.title.setTypeface(viewHolder.title.getTypeface(), Typeface.NORMAL);
        }

        //Adjust icon "size" to highlight
        int padding = highlightedItem == i ? 0 : fourDp;
        viewHolder.icon.setPadding(padding, padding, padding, padding);

        //To avoid "re-draw" issues
        final String key = ai.getPackageName() + "|" + ai.getActivityName();
        if(key == viewHolder.icon.getTag()) return; //Icon/name already set ok
        */

        //Set customIcon
        RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams((int) iconSize,
                (int) iconSize);
        rllp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        viewHolder.icon.setLayoutParams(rllp);
        viewHolder.icon.setTag(ai);
        viewHolder.icon.setImageBitmap(IconCache.getInstance().getAppIcon(
                ai.getPackageName(),
                ai.getActivityName(),
                IconCache.IconFetchPriority.APP_DRAWER_ICONS,
                (int) iconSize,
                new IconCache.ItemRetrievalInterface() {
            @Override
            public void onRetrievalComplete(Bitmap result) {
                if(viewHolder.icon.getTag().equals(ai)){
                    viewHolder.icon.setImageBitmap(result);
                }
            }
        }));

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
                View.DragShadowBuilder dsb  = new View.DragShadowBuilder(v.findViewById(R.id.appIconImage));
                v.startDrag(cd, dsb, new ApplicationIcon(ai.getPackageName(), ai.getName(),
                                ai.getActivityName()), 0);
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

    @Override
    public void highlightItem(int position) {
        return;

        /*
        if(highlightedItem == position) return; //No-op if already set
        if(highlightedItem >= 0) unhighlightItem();

        highlightedItem = position;
        notifyItemChanged(position);
        */
    }

    @Override
    public void unhighlightItem(){
        return;

        /*
        int previouslyHighlighted = highlightedItem;
        highlightedItem = -1;
        if(highlightedItem >= 0) notifyItemChanged(previouslyHighlighted);
        */
    }

    @Override
    public long getItemId(int position) {
        return apps.get(position).hashCode();
    }
}
