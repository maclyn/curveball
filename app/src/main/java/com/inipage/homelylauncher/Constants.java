package com.inipage.homelylauncher;

public class Constants {
    public static final String HAS_RUN_PREFERENCE = "has_run";
    public static final String HAS_REQUESTED_PERMISSIONS_PREF = "has_requested_pref";
    public static final String HAS_REQUESTED_USAGE_PERMISSION_PREF = "has_requested_usage_pref";
    public static final String VERSION_PREF = "version_pref";

    public class Versions {
        public static final int VERSION_0_2_3 = 1;
        public static final int VERSION_0_2_4 = 2;
        public static final int VERSION_0_3_1 = 3;

        public static final int CURRENT_VERSION = VERSION_0_3_1;
    }

    //region App preferences
    public static final String CLOCK_APP_PREFERENCE = "clock_pref";
    public static final String CALENDAR_APP_PREFERENCE = "cal_pref";
    public static final String WEATHER_APP_PREFERENCE = "weather_app_pref";
    public static final String CHARGING_APP_PREFERENCE = "charging_app_pref";
    public static final String LOW_POWER_APP_PREFERENCE = "low_power_app_pref";
    public static final String PHONE_APP_PREFERENCE = "phone_app_pref";
    public static final String ROWS_ABOVE_FOLD_PREFERENCE = "rows_fold_pref";
    public static final String COLUMN_COUNT_PREFERENCE = "column_count_pref";
    //endregion

    //region Weather preferences
    public static final String CACHED_WEATHER_RESPONSE_JSON_PREFERENCE = "cached_weather_response_json_pref";
    public static final String CACHED_WEATHER_RESPONSE_TIME_PREFERENCE = "cached_weather_response_time_pref";
    //endregion

    public static final String IS_PHONE_PREFERENCE = "is_phone_pref";
    public static final String ALLOW_ROTATION_PREF = "allow_rotation_pref";

    @Deprecated
    public static final String HOME_WIDGET_PREFERENCE = "home_widget_pref";
    @Deprecated
    public static final String HOME_WIDGET_ID_PREFERENCE = "home_widget_id_pref";

    public static final String CELCIUS_PREF = "celcius_pref";
}
