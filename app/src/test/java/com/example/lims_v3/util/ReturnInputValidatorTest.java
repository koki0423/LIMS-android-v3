package com.example.lims_v3.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReturnInputValidatorTest {

    @Test
    public void parseReturnQuantity_acceptsValidQuantity() {
        assertEquals(1, ReturnInputValidator.parseReturnQuantity("1", 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseReturnQuantity_rejectsZero() {
        ReturnInputValidator.parseReturnQuantity("0", 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseReturnQuantity_rejectsQuantityOverLimit() {
        ReturnInputValidator.parseReturnQuantity("4", 3);
    }
}
