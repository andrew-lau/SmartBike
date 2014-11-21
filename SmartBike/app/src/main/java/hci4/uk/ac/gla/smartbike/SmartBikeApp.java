package hci4.uk.ac.gla.smartbike;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

/**
 * Created by andrewlau on 21/11/2014.
 */
public class SmartBikeApp extends Application {

    private static Context context;
    private static Resources resources;
    private static AssetManager assets;

    public void onCreate(){
        super.onCreate();
        SmartBikeApp.context = getApplicationContext();
        resources = context.getResources();
        assets = resources.getAssets();
    }

    public static Context getContext() {
        return context;
    }

    public static Resources getAppResources() {
        return resources;
    }

    public static AssetManager getAppAssets() {
        return assets;
    }
}
