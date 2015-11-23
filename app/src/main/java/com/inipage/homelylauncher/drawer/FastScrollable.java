package com.inipage.homelylauncher.drawer;

public class FastScrollable {
    private String name;

    public FastScrollable(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getScrollableName(){
        return name == null ? "?" : name;
    }
}
