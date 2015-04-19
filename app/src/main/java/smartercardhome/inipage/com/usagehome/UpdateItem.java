package smartercardhome.inipage.com.usagehome;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class UpdateItem {
    private static final String TAG = "UpdateItem";
    private List<PageDescription> pages;
    private Drawable hintIcon;
    private String senderPackage;
    public int selectedPage = 0;
    private String dataType;

    public UpdateItem(Context context, Intent i) throws Exception {
        String action = i.getAction();
        if(!action.equals(Constants.FOUND_DATA_INTENT)){
            throw new Exception("Invalid intent type");
        }

        senderPackage = i.getStringExtra("sender_package");
        if(senderPackage == null){
            throw new Exception("No sender package found");
        }

        dataType = i.getStringExtra("sender_data");
        if(dataType == null){
            throw new Exception("No sender data type found");
        }

        String hintDrawableName = i.getStringExtra("hint_drawable");
        if(hintDrawableName == null){
            throw new Exception("No hint drawable found");
        }
        hintIcon = grabDrawable(context, senderPackage, hintDrawableName);

        //Get individual bits now
        String names[] = i.getStringArrayExtra("names");
        String descs[] = i.getStringArrayExtra("descriptions");
        String phrases[] = i.getStringArrayExtra("phrases");
        String urls[] = i.getStringArrayExtra("urls");
        String icons[] = i.getStringArrayExtra("drawable_names");

        if(names == null || descs == null || phrases == null || urls == null || icons == null){
            throw new Exception("Missing required bits");
        }

        if(names.length != descs.length ||
                descs.length != phrases.length ||
                phrases.length != urls.length ||
                urls.length != icons.length || names.length == 0){
            throw new Exception("Invalid lengths");
        }

        pages = new ArrayList<>(names.length);
        for(int j = 0; j < names.length; j++){
            pages.add(new PageDescription(names[j], descs[j], phrases[j], urls[j],
                    grabDrawable(context, senderPackage, icons[j])));
        }
    }

    public List<PageDescription> getPages(){
        return pages;
    }

    public String getSenderPackage(){
        return senderPackage;
    }

    public Drawable getHintIcon(){
        return hintIcon;
    }

    private Drawable grabDrawable(Context context, String senderPackage, String name) throws Exception{
        Drawable icon;

        if(name == null){
            return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        } else {
            try {
                int resource =
                        context.getPackageManager()
                                .getResourcesForApplication(senderPackage)
                                .getIdentifier(name, "drawable", senderPackage);
                if(resource == 0){
                    return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                } else {
                    icon = context.getPackageManager().getResourcesForApplication(senderPackage)
                            .getDrawable(resource);
                    return icon;
                }
            } catch (Exception e) {
                return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }
        }
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}