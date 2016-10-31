package com.inipage.homelylauncher.weather.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Date;

@Root(strict = false, name = "time")
public class TimeModel {
    @Attribute
    private Date from;

    @Attribute
    private Date to;

    @Element
    LocationModel location;

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public LocationModel getLocation() {
        return location;
    }
}
