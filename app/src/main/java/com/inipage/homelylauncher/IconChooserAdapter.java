package com.inipage.homelylauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

public class IconChooserAdapter extends
        RecyclerView.Adapter<IconChooserAdapter.IconHolder> {
    List<Pair<String, Integer>> icons;
    String packageName;
    IconChooserActivity ctx;
    Resources r;

    public static class IconHolder extends RecyclerView.ViewHolder {
        ImageView icon;

        public IconHolder(ImageView mainView) {
            super(mainView);
            this.icon = mainView;
        }
    }

    public IconChooserAdapter(List<Pair<String, Integer>> icons, String packageName,
                              IconChooserActivity context, Resources r) {
        this.icons = icons;
        this.packageName = packageName;
        this.ctx = context;
        this.r = r;
    }

    @Override
    public IconHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ImageView icon = new ImageView(viewGroup.getContext());
        return new IconHolder(icon);
    }

    //Set up specific customIcon with data
    @Override
    public void onBindViewHolder(IconHolder viewHolder, final int i) {
        viewHolder.icon.setImageDrawable(r.getDrawable(icons.get(i).second));
        viewHolder.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("r_name", icons.get(i).first);
                resultIntent.putExtra("r_pkg", packageName);
                ctx.setResult(Activity.RESULT_OK, resultIntent);
                ctx.finish();
            }
        });
    }

    @Override
    public int getItemCount() {
        return icons.size();
    }
}
