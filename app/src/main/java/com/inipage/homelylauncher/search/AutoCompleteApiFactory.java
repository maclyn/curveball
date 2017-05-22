package com.inipage.homelylauncher.search;

import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class AutoCompleteApiFactory {
    private static AutoCompleteApiInstance instance;

    public static AutoCompleteApiInstance getInstance(){
        if (instance != null) return instance;

        Retrofit adapter = new Retrofit.Builder()
                .baseUrl("http://suggestqueries.google.com")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build();

        return (instance = adapter.create(AutoCompleteApiInstance.class));
    }
}
