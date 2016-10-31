package com.inipage.homelylauncher.weather;

import com.inipage.homelylauncher.weather.model.WeatherDateTransformer;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.strategy.TreeStrategy;
import org.simpleframework.xml.transform.RegistryMatcher;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class WeatherApiFactory {
    private static WeatherApiInterface instance = null;

    public static WeatherApiInterface getInstance(){
        if(instance != null) return instance;

        RegistryMatcher matcher = new RegistryMatcher();
        matcher.bind(Date.class, new WeatherDateTransformer());

        //Weirdly fitting question: http://stackoverflow.com/questions/8862548/simple-xml-framework-on-android-class-attribute
        Strategy strategy = new TreeStrategy("clazz", "len");
        Serializer serializer = new Persister(strategy, matcher);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.met.no/weatherapi/")
                .addConverterFactory(SimpleXmlConverterFactory.create(serializer))
                .build();
        return instance = retrofit.create(WeatherApiInterface.class);
    }
}
