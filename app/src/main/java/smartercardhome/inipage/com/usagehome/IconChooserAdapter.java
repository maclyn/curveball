package smartercardhome.inipage.com.usagehome;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
