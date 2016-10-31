package com.inipage.homelylauncher.weather.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "temperature")
public class TemperatureModel {
    @Attribute
    float value;

    public float getValue() {
        return value;
    }
}
