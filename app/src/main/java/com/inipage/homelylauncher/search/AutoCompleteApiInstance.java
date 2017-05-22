package com.inipage.homelylauncher.search;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AutoCompleteApiInstance {
    @GET("/complete/search")
    Call<AutoCompleteResult> getSearchSuggestions(@Query("output") String output, @Query("q") String query);
}
