package com.lensmatch.mobile;

import android.app.Application;
import com.lensmatch.mobile.data.AppState;

public class LensMatchApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppState.init(this);
    }
}
