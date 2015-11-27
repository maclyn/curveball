package com.inipage.homelylauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;

public class AttributeApplier {
    public static void ApplyDensity(Object source, Context paramSource) {
        DisplayMetrics metrics = paramSource.getResources().getDisplayMetrics();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(paramSource);

        for(Field field : source.getClass().getDeclaredFields()){
            if(field.isAnnotationPresent(SizeAttribute.class)){
                SizeAttribute sa = field.getAnnotation(SizeAttribute.class);
                SizeAttribute.AttributeType at = sa.attrType();

                if(field.getType() != Float.TYPE){
                    throw new InvalidParameterException("Must apply the SizeAttribute annotation to a float!");
                }

                try {
                    field.setAccessible(true);

                    float value = sa.value();
                    //See if something in default preferences overrides it
                    String setting = sa.setting();
                    if(!setting.equals(SizeAttribute.DEFAULT_PREF_KEY)){
                        if(preferences.contains(setting)){
                            try {
                                value = preferences.getFloat(setting, 5f);
                            } catch (Exception notAFloat) {
                                value = preferences.getInt(setting, 5);
                            }
                        }
                    }

                    float newValue = TypedValue.applyDimension(
                            at == SizeAttribute.AttributeType.SP ? TypedValue.COMPLEX_UNIT_SP :
                                    (at == SizeAttribute.AttributeType.DIP ? TypedValue.COMPLEX_UNIT_DIP :
                                            TypedValue.COMPLEX_UNIT_IN),
                            value, metrics);

                    field.setFloat(source, newValue);
                } catch (Exception ignored) {} //Can't get IllegalAccessExceptions
            }

            //TODO: Finish this
            /*
            if(field.isAnnotationPresent(VisibilityAttribute.class)){
                VisibilityAttribute va = field.getAnnotation(VisibilityAttribute.class);

                boolean visibility = va.visibility();

                if(isView(field)){
                    throw new InvalidParameterException("Must apply the VisibilityAttribute annotation to a View!");
                }

                try {
                    field.setAccessible(true);
                    field.get()
                    field.set("visibility", visibility);
                } catch (Exception ignored) {} //Can't get IllegalAccessExceptions
            }
            */
        }
    }

    public static boolean isView(Field f){
        boolean isAView = false;
        Class<?> currentClass = f.getClass();
        while(!isAView && currentClass != null) {
            if(currentClass == View.class){
                isAView = true;
            } else {
                currentClass = currentClass.getSuperclass();
            }
        }

        return isAView;
    }
}
