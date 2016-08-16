package org.redcross.openmapkit;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Jason Rogena - jrogena@ona.io on 8/16/16.
 */
public class OpenMapKit extends Application {
    public static RequestQueue VOLLEY_REQUEST_QUEUE;
    @Override
    public void onCreate() {
        super.onCreate();

        VOLLEY_REQUEST_QUEUE = Volley.newRequestQueue(this);
    }
}
