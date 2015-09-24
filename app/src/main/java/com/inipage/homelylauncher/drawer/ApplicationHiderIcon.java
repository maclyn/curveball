package com.inipage.homelylauncher.drawer;

public class ApplicationHiderIcon extends ApplicationIcon {
    private boolean isHidden;

    public ApplicationHiderIcon(String packageName, String label, String activityName,
                                boolean isHidden){
        super(packageName, label, activityName);
        this.isHidden = isHidden;
    }

    public boolean getIsHidden(){
        return isHidden;
    }

    public void setIsHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }
}
