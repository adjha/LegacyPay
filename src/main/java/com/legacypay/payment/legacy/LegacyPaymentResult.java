package com.legacypay.payment.legacy;

public class LegacyPaymentResult {

    public static final String SUCCESS_CODE = "00";
    public static final String INSUFFICIENT_FUNDS_CODE = "51";
    public static final String INVALID_ACCOUNT_CODE = "14";

    private final String resultCode;
    private final String description;

    public LegacyPaymentResult(String resultCode, String description) {
        this.resultCode = resultCode;
        this.description = description;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(resultCode);
    }
}
