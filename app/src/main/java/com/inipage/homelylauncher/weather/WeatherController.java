package com.inipage.homelylauncher.weather;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.inipage.homelylauncher.Constants;
import com.inipage.homelylauncher.weather.model.LTSForecastModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A wrapper for fetching weather. Makes things nice and shiny.
 */
public class WeatherController {
    private static final String TAG = "WeatherController";
    private static final long WEATHER_CACHE_DURATION = 60 * 60 * 1000; //1 hour

    public interface WeatherPresenter {
        void requestLocationPermission();
        void onWeatherFound(LTSForecastModel weather);
        void onFetchFailure();
    }

    public static void requestWeather(Context context, WeatherPresenter presenter, boolean forceRefresh){
        SharedPreferences reader = PreferenceManager.getDefaultSharedPreferences(context);
        long lastWeatherResponseTime = reader.getLong(Constants.CACHED_WEATHER_RESPONSE_TIME_PREFERENCE, -1);
        if(lastWeatherResponseTime > (System.currentTimeMillis() - (WEATHER_CACHE_DURATION)) && !forceRefresh){
            try {
                Log.d(TAG, "Displaying cached weather...");
                presenter.onWeatherFound(LTSForecastModel.deserialize(reader.getString(Constants.CACHED_WEATHER_RESPONSE_JSON_PREFERENCE, null)));
            } catch (Exception e){
                Log.e(TAG, "Error displaying cached weather", e);
                refreshWeather(context, presenter);
            }
        } else {
            Log.d(TAG, "Weather data is stale; refreshing");
            refreshWeather(context, presenter);
        }
    }

    private static void refreshWeather(final Context context, final WeatherPresenter presenter) {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            presenter.requestLocationPermission();
            return;
        }

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_LOW);

        Location lastLocation = lm.getLastKnownLocation(lm.getBestProvider(criteria, true));
        if(lastLocation != null){
            fetchWeatherForLocation(context, presenter, lastLocation);
        } else {
            lm.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    fetchWeatherForLocation(context, presenter, location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            }, Looper.getMainLooper());
        }
    }

    private static void fetchWeatherForLocation(final Context context, final WeatherPresenter presenter, Location location){
        if (location != null) {
            Log.d(TAG, location.getLatitude() + "; " + location.getLongitude());
        } else {
            return;
        }

        WeatherApiFactory.getInstance().getWeatherLTS(location.getLatitude(), location.getLongitude()).enqueue(new Callback<LTSForecastModel>() {
            @Override
            public void onResponse(Call<LTSForecastModel> call, Response<LTSForecastModel> response) {
                Log.d(TAG, "Got weather response!");
                try {
                    LTSForecastModel model = response.body();
                    presenter.onWeatherFound(model);

                    //Cache me if you can
                    SharedPreferences.Editor writer = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    writer.putString(Constants.CACHED_WEATHER_RESPONSE_JSON_PREFERENCE, model.serialize());
                    writer.putLong(Constants.CACHED_WEATHER_RESPONSE_TIME_PREFERENCE, System.currentTimeMillis());
                    writer.commit();
                } catch (Exception ignored) {
                    Log.d(TAG, response.raw().toString());
                    presenter.onFetchFailure();
                }
            }

            @Override
            public void onFailure(Call<LTSForecastModel> call, Throwable t) {
                Log.e(TAG, "Failure fetching weather", t);
                presenter.onFetchFailure();
            }
        });
    }
}
