package com.inipage.homelylauncher.weather.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false, name = "product")
public class ProductModel {
    @Attribute(name = "class")
    String className;

    @ElementList(inline = true)
    private List<TimeModel> timeEntries;

    public List<TimeModel> getTimeEntries() {
        return timeEntries;
    }
}
