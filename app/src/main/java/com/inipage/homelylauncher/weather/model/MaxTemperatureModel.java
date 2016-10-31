package com.inipage.homelylauncher.weather.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "maxTemperature")
public class MaxTemperatureModel {
    @Attribute
    float value;

    public float getValue() {
        return value;
    }
}
