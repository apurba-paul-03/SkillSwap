package com.example.skillswap;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // REST API endpoint returning random study motivation quote (JSON)
    @GET("quotes/random")
    Call<QuoteModel> getRandomQuote();
}
