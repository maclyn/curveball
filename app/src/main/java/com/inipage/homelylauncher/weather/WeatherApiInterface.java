package com.inipage.homelylauncher.weather;

import com.inipage.homelylauncher.weather.model.LTSForecastModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiInterface {
    @GET("locationforecastlts/1.3/")
    Call<LTSForecastModel> getWeatherLTS(@Query("lat") double lat, @Query("lon") double lon);
}
