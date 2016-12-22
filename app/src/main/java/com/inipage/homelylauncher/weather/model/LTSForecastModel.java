package com.inipage.homelylauncher.weather.model;

import com.google.gson.Gson;

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

    /**
     * Serialize the object for easier storage. I do see the irony of deserializing from XML and
     * then reserializing as JSON. ¯\_(ツ)_/¯
     * @return Serialization.
     */
    public String serialize(){
        return new Gson().toJson(this);
    }

    public static LTSForecastModel deserialize(String serialization){
        return new Gson().fromJson(serialization, LTSForecastModel.class);
    }
}

