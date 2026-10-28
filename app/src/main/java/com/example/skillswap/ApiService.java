package com.example.skillswap;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // Example endpoint using a reliable public test API (JSONPlaceholder)
    @GET("users")
    Call<List<LocationModel>> getMeetingLocations();
}