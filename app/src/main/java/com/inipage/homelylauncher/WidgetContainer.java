package com.inipage.homelylauncher;

public class WidgetContainer{
    private int widgetId;
    private int widgetHeight;

    public WidgetContainer(int widgetId, int widgetHeight) {
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

}
