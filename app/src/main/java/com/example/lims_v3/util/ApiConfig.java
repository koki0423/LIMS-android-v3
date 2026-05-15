package com.example.lims_v3.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lims_v3.ui.SettingsActivity;

import java.net.URI;
import java.net.URISyntaxException;

public final class ApiConfig {

    private ApiConfig() {
    }

    public static String requireBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String rawBaseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (rawBaseUrl == null || rawBaseUrl.trim().isEmpty()) {
            throw new IllegalStateException("設定画面でAPI URLを設定してください");
        }
        return normalizeBaseUrl(rawBaseUrl);
    }

    public static String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null) {
            throw new IllegalArgumentException("API URLが未設定です");
        }

        String trimmed = rawBaseUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("API URLを入力してください");
        }

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("API URLの形式が不正です", exception);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("API URLは http:// または https:// で入力してください");
        }

        if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("API URLのホスト名が不正です");
        }

        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("API URLにクエリやフラグメントは指定できません");
        }

        String normalized = uri.toString();
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized;
    }
}
