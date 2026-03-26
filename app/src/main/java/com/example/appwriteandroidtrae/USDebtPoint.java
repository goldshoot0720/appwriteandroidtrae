package com.example.appwriteandroidtrae;

public class USDebtPoint {
    public final long debtValue;
    public final long fetchedAtMillis;

    public USDebtPoint(long debtValue, long fetchedAtMillis) {
        this.debtValue = debtValue;
        this.fetchedAtMillis = fetchedAtMillis;
    }
}
