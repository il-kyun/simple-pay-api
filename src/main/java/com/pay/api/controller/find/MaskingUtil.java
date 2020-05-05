package com.pay.api.controller.find;

import org.springframework.util.StringUtils;

class MaskingUtil {

    private static final char MASKING_CHAR = '*';

    /**
     * 앞 6자리와 뒤 3자리를 제외한 나머지를 마스킹처리
     *
     * @param cardNumber : target card number
     * @return masked cardNumber
     */
    static String getMaskedCardNumber(String cardNumber) {

        if (!StringUtils.hasText(cardNumber) || cardNumber.length() < 10 || cardNumber.length() > 16) {
            return "";
        }

        String c = cardNumber.trim();
        int size = c.length();
        StringBuilder b = new StringBuilder(size);
        b.append(c, 0, 6);
        int length = size - 3;
        for (int i = 6; i < length; i++) {
            b.append(MASKING_CHAR);
        }

        b.append(c, length, length + 3);
        return b.toString();
    }
}