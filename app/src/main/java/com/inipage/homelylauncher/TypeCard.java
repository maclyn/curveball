package com.inipage.homelylauncher;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class TypeCard {
    public enum TypeCardType {
        APP, ROW
    }

    private String drawablePackage;
    private String drawableName;
    private String title;
    private List<Pair<String, String>> apps;
    private TypeCardType type;

    public TypeCard(String title, String drawableName, List<Pair<String, String>> apps) {
        this.title = title;
        this.drawablePackage = getClass().getPackage().getName();
        this.drawableName = drawableName;
        this.apps = apps;
        this.type = TypeCardType.ROW;
    }

    public TypeCard(Pair<String, String> appName){
        this.title = "";
        this.drawablePackage = getClass().getPackage().getName();
        this.drawableName = "ic_launcher";
        this.apps = new ArrayList<>();
        this.apps.add(appName);
        this.type = TypeCardType.APP;
    }

    public TypeCard(String title, String drawablePackage, String drawableName, List<Pair<String, String>> apps) {
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

    public TypeCardType getType(){
        return this.type;
    }
}
