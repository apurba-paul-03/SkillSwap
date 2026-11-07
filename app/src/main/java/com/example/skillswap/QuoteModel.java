package com.example.skillswap;

import com.google.gson.annotations.SerializedName;

public class QuoteModel {
    @SerializedName("quote")
    private String quote;

    @SerializedName("author")
    private String author;

    public QuoteModel() {}

    public QuoteModel(String quote, String author) {
        this.quote = quote;
        this.author = author;
    }

    public String getQuote() { return quote; }
    public String getAuthor() { return author; }
}
