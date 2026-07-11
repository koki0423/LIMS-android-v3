package com.example.lims_v3.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiConfigTest {

    @Test
    public void normalizeBaseUrl_addsTrailingSlash() {
        assertEquals("http://192.168.0.1:8080/api/v2/", ApiConfig.normalizeBaseUrl("http://192.168.0.1:8080/api/v2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeBaseUrl_rejectsMissingScheme() {
        ApiConfig.normalizeBaseUrl("192.168.0.1:8080/api/v2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeBaseUrl_rejectsQueryString() {
        ApiConfig.normalizeBaseUrl("http://example.com/api?v=1");
    }
}
