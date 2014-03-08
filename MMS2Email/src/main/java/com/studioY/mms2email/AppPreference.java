package com.studioY.mms2email;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreference {
    private Context context;

    public AppPreference(Context context) {
        this.context = context;
    }

    public void saveValue(String key, String value){
        SharedPreferences pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String getValue(String key){
        SharedPreferences pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
        return pref.getString(key, "");
    }
}
