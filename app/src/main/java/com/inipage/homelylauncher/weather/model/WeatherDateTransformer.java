package com.inipage.homelylauncher.weather.model;


import org.simpleframework.xml.transform.Transform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherDateTransformer implements Transform<Date> {
    private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private static Calendar calendar = new GregorianCalendar();

    static {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public WeatherDateTransformer(){
    }

    @Override
    public Date read(String value) throws Exception {
        Date date = format.parse(value);
        calendar.setTime(date);
        calendar.setTimeZone(TimeZone.getDefault());
        return calendar.getTime();
    }

    @Override
    public String write(Date value) throws Exception {
        return format.format(value); //Won't work! Not used!
    }
}