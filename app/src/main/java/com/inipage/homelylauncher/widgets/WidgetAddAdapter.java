package com.inipage.homelylauncher.widgets;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.inipage.homelylauncher.R;

import java.util.List;

public class WidgetAddAdapter extends RecyclerView.Adapter<WidgetAddAdapter.WidgetAddVH> {
    private Context context;
    private List<AppWidgetProviderInfo> objects;
    private OnWidgetClickListener listener;

    public interface OnWidgetClickListener {
        void onClick(AppWidgetProviderInfo awpi);
    }

    public class WidgetAddVH extends RecyclerView.ViewHolder {
        private LinearLayout mainLayout;
        private TextView widgetName;
        private ImageView widgetPreview;

        public WidgetAddVH(View itemView) {
            super(itemView);
            mainLayout = (LinearLayout) itemView;
            widgetName = (TextView) mainLayout.findViewById(R.id.widgetPreviewText);
            widgetPreview = (ImageView) mainLayout.findViewById(R.id.widgetPreviewImage);
        }
    }

    @Override
    public WidgetAddVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new WidgetAddVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.widget_preview, parent, false));
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    @Override
    public void onBindViewHolder(WidgetAddVH holder, int position) {
        final AppWidgetProviderInfo awpi = objects.get(position);
        if(awpi != null){
            holder.widgetName.setText(awpi.label);

            //Do this in an AsyncTask() to avoid slowing stuff down needlessly
            Drawable preview = null;
            holder.widgetPreview.setImageDrawable(null); //Clear slate
            holder.widgetPreview.setTag(awpi); //We check at the end of the AsyncTask to ensure validity
            setImageAsync(awpi, holder.widgetPreview);

            holder.mainLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null) listener.onClick(awpi);
                }
            });
        }
    }

    public WidgetAddAdapter(List<AppWidgetProviderInfo> objects, Context context){
        this.objects = objects;
        this.context = context;
    }

    private void setImageAsync(final AppWidgetProviderInfo awpi, final ImageView iv){
        new AsyncTask<PackageManager, Void, Drawable>(){
            @Override
            protected Drawable doInBackground(PackageManager... params) {
                Drawable preview = null;
                try {
                    if(awpi.previewImage != 0) {
                        preview = params[0].getDrawable(awpi.provider.getPackageName(),
                                awpi.previewImage, null);
                    } else {
                        preview = params[0].getApplicationIcon(awpi.provider.getPackageName());
                    }
                } catch (Exception e) {
                    preview = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                }
                return preview;
            }

            @Override
            protected void onPostExecute(Drawable d){
                AppWidgetProviderInfo tag = (AppWidgetProviderInfo) iv.getTag();
                if(tag != null && tag.equals(awpi) && d != null){
                    iv.setImageDrawable(d);
                }
            }
        }.execute(context.getPackageManager());
    }

    public void setOnClickListener(OnWidgetClickListener listener) {
        this.listener = listener;
    }
}
