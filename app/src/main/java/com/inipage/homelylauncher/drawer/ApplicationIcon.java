package com.inipage.homelylauncher.drawer;

public class ApplicationIcon extends FastScrollable {
    private String packageName;
    private String activityName;
    private int hashCode;

    public ApplicationIcon(String packageName, String label, String activityName){
        super(label);
        this.packageName = packageName;
        this.activityName = activityName;
        this.hashCode = packageName.hashCode() + activityName.hashCode();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
