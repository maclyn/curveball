package com.inipage.homelylauncher.widgets;

public class WidgetContainer {
    private int widgetId;

    public WidgetContainer(String widgetPackage, int widgetId, int widgetHeight) {
        this.widgetId = widgetId;
    }

    public int getWidgetId() {
        return widgetId;
    }
}
