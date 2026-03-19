package com.example.appwriteandroidtrae;

public class OilPricePoint {
    public final long dateMillis;
    public final double price;
    public final long fetchedAtMillis;

    public OilPricePoint(long dateMillis, double price, long fetchedAtMillis) {
        this.dateMillis = dateMillis;
        this.price = price;
        this.fetchedAtMillis = fetchedAtMillis;
    }
}
