package com.inipage.homelylauncher.drawer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inipage.homelylauncher.R;

import java.util.List;
import java.util.Map;

public class ApplicationHideAdapter extends RecyclerView.Adapter<ApplicationHideAdapter.ApplicationVH> {
    List<ApplicationHiderIcon> apps;
    Context ctx;

    private float iconSize;

    public class ApplicationVH extends RecyclerView.ViewHolder {
        TextView title;
        ImageView hiddenIcon;

        public ApplicationVH(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.appIconName);
            hiddenIcon = (ImageView) itemView.findViewById(R.id.appIconHidden);
        }
    }

    public ApplicationHideAdapter(Context context, List<ApplicationHiderIcon> objects) {
        this.ctx = context;
        this.apps = objects;
    }

    @Override
    public ApplicationVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ApplicationVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.application_icon_hidden, parent, false));
    }

    @Override
    public void onBindViewHolder(ApplicationVH holder, int position) {
        final ApplicationHiderIcon ai = apps.get(position);

        TextView title = holder.title;
        final ImageView hiddenIcon = holder.hiddenIcon;

        //Set title
        title.setText(ai.getName());

        //Set visibility icon
        hiddenIcon.setImageResource(ai.getIsHidden()
                ? R.drawable.ic_visibility_off_white_48dp
                : R.drawable.ic_visibility_white_48dp);

        //Set launch
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ai.setIsHidden(!ai.getIsHidden());
                hiddenIcon.setImageResource(ai.getIsHidden()
                        ? R.drawable.ic_visibility_off_white_48dp
                        : R.drawable.ic_visibility_white_48dp);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public List<ApplicationHiderIcon> getApps() {
        return apps;
    }
}
