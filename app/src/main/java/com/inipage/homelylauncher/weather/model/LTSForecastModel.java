package com.inipage.homelylauncher.weather.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false, name = "weatherdata")
public class LTSForecastModel {
    @Element
    private ProductModel product;

    public ProductModel getProduct() {
        return product;
    }
}

