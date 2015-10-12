package com.inipage.homelylauncher.utils;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SizeAttribute {
    public static String DEFAULT_PREF_KEY = "null";

    enum AttributeType {
        DIP, SP
    }

    AttributeType attrType() default AttributeType.DIP;
    String setting() default DEFAULT_PREF_KEY;
    float value();
}
