package com.example.lims_v3.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthSessionManager {
    private static final String PREF_NAME = "AuthSessionPrefs";
    private static final String KEY_AUTH_TOKEN = "AUTH_TOKEN";
    private static final String KEY_AUTH_USER_ID = "AUTH_USER_ID";

    private AuthSessionManager() {
    }

    public static void saveSession(Context context, String userId, String token) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_AUTH_USER_ID, userId);
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public static void clearSession(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(KEY_AUTH_USER_ID);
        editor.remove(KEY_AUTH_TOKEN);
        editor.apply();
    }

    public static String getUserId(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AUTH_USER_ID, "");
    }

    public static String getToken(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AUTH_TOKEN, "");
    }
}
