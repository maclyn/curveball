package com.inipage.homelylauncher.weather.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "location")
public class LocationModel {
    @Element(required = false)
    TemperatureModel temperature;

    @Element(required = false)
    MinTemperatureModel minTemperature;

    @Element(required = false)
    MaxTemperatureModel maxTemperature;

    @Element(required = false)
    SymbolModel symbol;

    @Element(required = false)
    PrecipitationModel precipitation;

    public TemperatureModel getTemperature() {
        return temperature;
    }

    public MinTemperatureModel getMinTemperature() {
        return minTemperature;
    }

    public MaxTemperatureModel getMaxTemperature() {
        return maxTemperature;
    }

    public SymbolModel getSymbol() {
        return symbol;
    }

    public PrecipitationModel getPrecipitation() {
        return precipitation;
    }
}
