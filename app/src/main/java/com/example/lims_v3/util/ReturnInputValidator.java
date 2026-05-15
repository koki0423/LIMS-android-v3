package com.example.lims_v3.util;

public final class ReturnInputValidator {

    private ReturnInputValidator() {
    }

    public static int parseReturnQuantity(String quantityText, int maxQuantity) {
        if (maxQuantity <= 0) {
            throw new IllegalArgumentException("返却可能な数量がありません");
        }

        if (quantityText == null || quantityText.trim().isEmpty()) {
            throw new IllegalArgumentException("返却数量を入力してください");
        }

        final int quantity;
        try {
            quantity = Integer.parseInt(quantityText.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("返却数量は整数で入力してください", exception);
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("返却数量は1以上で入力してください");
        }

        if (quantity > maxQuantity) {
            throw new IllegalArgumentException("返却数量が貸出数量を超えています");
        }

        return quantity;
    }
}
