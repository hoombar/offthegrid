package net.rdyonline;

import android.content.Context;

public class OffTheGrid extends android.app.Application {

    private static OffTheGrid instance;

    public OffTheGrid() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }
}