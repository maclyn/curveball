package smartercardhome.inipage.com.usagehome;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class WidgetAddAdapter extends ArrayAdapter<AppWidgetProviderInfo> {
    private static class ViewHolder {
        private LinearLayout mainLayout;
        private TextView widgetName;
        private ImageView widgetPreview;
    }

    public WidgetAddAdapter(Context context, int resource, List<AppWidgetProviderInfo> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        ViewHolder vh;
        if(convertView == null){
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.widget_preview,
                    parent, false);
            vh = new ViewHolder();
            vh.mainLayout = (LinearLayout) convertView;
            vh.widgetName = (TextView) vh.mainLayout.findViewById(R.id.widgetPreviewText);
            vh.widgetPreview = (ImageView) vh.mainLayout.findViewById(R.id.widgetPreviewImage);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        final AppWidgetProviderInfo awpi = getItem(position);
        if(awpi != null){
            vh.widgetName.setText(awpi.label);

            //Do this in an AsyncTask() to avoid slowing stuff down needlessly
            Drawable preview = null;
            vh.widgetPreview.setImageDrawable(null); //Clear slate
            vh.widgetPreview.setTag(awpi); //We check at the end of the AsyncTask to ensure validity
            setImageAsync(awpi, vh.widgetPreview);
        }
        return convertView;
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
                    preview = getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
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
        }.execute(getContext().getPackageManager());
    }
}
