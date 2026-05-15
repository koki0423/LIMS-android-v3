package com.example.lims_v3.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClientFactory {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new FlexibleDateDeserializer())
            .create();

    private ApiClientFactory() {
    }

    public static <T> T createService(Context context, Class<T> serviceClass) {
        return createService(context, ApiConfig.requireBaseUrl(context), serviceClass);
    }

    public static <T> T createService(Context context, String baseUrl, Class<T> serviceClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(buildAuthorizedClient(context))
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .build();
        return retrofit.create(serviceClass);
    }

    public static <T> T createService(String baseUrl, Class<T> serviceClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .build();
        return retrofit.create(serviceClass);
    }

    private static OkHttpClient buildAuthorizedClient(Context context) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    String requestPath = originalRequest.url().encodedPath();
                    if (requestPath != null && requestPath.endsWith("/login")) {
                        return chain.proceed(originalRequest);
                    }

                    String token = AuthSessionManager.getToken(context);
                    if (token == null || token.trim().isEmpty()) {
                        return chain.proceed(originalRequest);
                    }

                    Request authorizedRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + token.trim())
                            .build();
                    return chain.proceed(authorizedRequest);
                })
                .build();
    }

    private static final class FlexibleDateDeserializer implements JsonDeserializer<Date> {
        private static final String[] DATE_PATTERNS = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String dateValue = json.getAsString();
            for (String pattern : DATE_PATTERNS) {
                try {
                    return buildDateFormat(pattern).parse(dateValue);
                } catch (ParseException ignored) {
                }
            }
            throw new JsonParseException("Unsupported date format: " + dateValue);
        }

        private SimpleDateFormat buildDateFormat(String pattern) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.US);
            dateFormat.setLenient(false);
            if (pattern.endsWith("'Z'")) {
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            return dateFormat;
        }
    }
}
