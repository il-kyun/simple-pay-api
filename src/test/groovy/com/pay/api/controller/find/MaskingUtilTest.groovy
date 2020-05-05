package com.pay.api.controller.find


import spock.lang.Specification
import spock.lang.Unroll

class MaskingUtilTest extends Specification {

    @Unroll
    def "masking test : #description"() {
        when:
        def masked = MaskingUtil.getMaskedCardNumber(cardNumber)

        then:
        masked == maskedCardNumber

        where:
        cardNumber          | maskedCardNumber || description
        null                | ""               || "cardNumber is null"
        ""                  | ""               || "cardNumber is empty"
        " "                 | ""               || "cardNumber is space"
        "123456789"         | ""               || "cardNumber is ${cardNumber}"
        "12345678901234567" | ""               || "cardNumber is ${cardNumber}"
        "1234567890"        | "123456*890"     || "cardNumber is ${cardNumber}"
    }
}
