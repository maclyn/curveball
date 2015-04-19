package smartercardhome.inipage.com.usagehome;

public class ApplicationIcon {
    private String name;
    private String packageName;
    private String activityName;

    public ApplicationIcon(String packageName, String label, String activityName){
        this.name = label;
        this.packageName = packageName;
        this.activityName = activityName;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }
}
