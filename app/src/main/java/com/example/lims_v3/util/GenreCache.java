package com.example.lims_v3.util;

import com.example.lims_v3.network.GenreResponse;

import java.util.ArrayList;
import java.util.List;

public final class GenreCache {
    private static boolean initialized;
    private static List<GenreResponse> cachedGenres = new ArrayList<>();

    private GenreCache() {
    }

    public static synchronized boolean isInitialized() {
        return initialized;
    }

    public static synchronized List<GenreResponse> getGenres() {
        return new ArrayList<>(cachedGenres);
    }

    public static synchronized void storeGenres(List<GenreResponse> genres) {
        cachedGenres = genres == null ? new ArrayList<>() : new ArrayList<>(genres);
        initialized = true;
    }

    public static synchronized String findGenreNameById(Long genreId) {
        if (genreId == null) {
            return "";
        }
        for (GenreResponse genre : cachedGenres) {
            if (genre != null && genre.getGenreId() == genreId) {
                String genreName = genre.getGenreName();
                if (genreName != null && !genreName.trim().isEmpty()) {
                    return genreName;
                }
                String genreCode = genre.getGenreCode();
                return genreCode != null ? genreCode : "";
            }
        }
        return "";
    }

    public static synchronized void clear() {
        cachedGenres = new ArrayList<>();
        initialized = false;
    }
}
