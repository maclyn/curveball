package com.inipage.homelylauncher.widgets;

public class WidgetContainer {
    private String widgetPackage;
    private int widgetId;
    private int widgetHeight;

    public WidgetContainer(String widgetPackage, int widgetId, int widgetHeight) {
        this.widgetPackage = widgetPackage;
        this.widgetId = widgetId;
        this.widgetHeight = widgetHeight;
    }

    public int getWidgetId() {
        return widgetId;
    }

    /** Height in dp.
     *
     * @return How many DPs tall the widget is.
     */
    public int getWidgetHeight() {
        return widgetHeight;
    }

    public void setWidgetHeight(int widgetHeight) {
        this.widgetHeight = widgetHeight;
    }

    public String getWidgetPackage() {
        return widgetPackage;
    }
}
