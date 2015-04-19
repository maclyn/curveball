package smartercardhome.inipage.com.usagehome;

import android.util.Pair;
import java.util.List;

public class TypeCard {
    private String drawablePackage;
    private String drawableName;
    private String title;
    private List<Pair<String, String>> apps;

    TypeCard(String title, String drawableName, List<Pair<String, String>> apps) {
        this.title = title;
        this.drawablePackage = getClass().getPackage().getName();
        this.drawableName = drawableName;
        this.apps = apps;
    }

    TypeCard(String title, String drawablePackage, String drawableName, List<Pair<String, String>> apps) {
        this.title = title;
        this.drawablePackage = drawablePackage;
        this.drawableName = drawableName;
        this.apps = apps;
    }

    public String getDrawableName() {
        return drawableName;
    }

    public String getDrawablePackage(){
        return drawablePackage;
    }

    public void setDrawable(String dRes, String dPkg){
        this.drawableName = dRes;
        this.drawablePackage = dPkg;
    }

    public List<Pair<String, String>> getPackages() {
        return apps;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
