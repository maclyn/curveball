package com.inipage.homelylauncher.weather.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "minTemperature")
public class MinTemperatureModel {
    @Attribute
    float value;

    public float getValue() {
        return value;
    }
}
