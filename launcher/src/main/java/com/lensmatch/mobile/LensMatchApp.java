package com.lensmatch.mobile;

import android.app.Application;
import com.lensmatch.mobile.data.AppState;

public class LensMatchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        AppState.init(this);
    }
}
