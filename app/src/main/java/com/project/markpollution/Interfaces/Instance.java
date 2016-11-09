package com.project.markpollution.Interfaces;

import android.app.ProgressDialog;

/**
 * IDE: Android Studio
 * Created by Nguyen Trong Cong  - 2DEV4U.COM
 * Name packge: com.project.markpollution.Interfaces
 * Name project: MarkPollutionBeta
 * Date: 11/7/2016
 * Time: 6:36 PM
 */
public class Instance {
    // Progress dialog
    public static ProgressDialog pDialog;
    private static Instance instance;

    //no outer class can initialize this class's object
    private Instance() {
    }

    public static Instance getInstance() {
        //if no instance is initialized yet then create new instance
        //else return stored instance
        if (instance == null) {
            instance = new Instance();
        }
        return instance;
    }

    public void showpDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    public void hidepDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
