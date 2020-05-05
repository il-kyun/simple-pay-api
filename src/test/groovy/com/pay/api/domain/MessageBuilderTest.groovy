package com.pay.api.domain

import spock.lang.Specification
import spock.lang.Unroll

class MessageBuilderTest extends Specification {

    def "PaymentMessageBuilder 정상 처리 테스"() {
        when:
        def message = MessageBuilder.newPaymentMessageBuilder()
                .id(transactionId)
                .cardNumber(cardNumber)
                .expirationMonthYear(expirationMonthYear)
                .cvc(cvc)
                .installment(installment)
                .amount(amount)
                .vat(vat)
                .encryptedCardInformation(encryptedCardInformation)
                .build()

        then:
        message.length() == 450
        message == expectedMessage


        where:
        transactionId          | cardNumber         | installment | expirationMonthYear | cvc   | amount | vat   | payTrlansactionId | encryptedCardInformation                                                                               | expectedMessage                                                                                                                                                                                                                                                                                                                                                                                                                                                      || description
        "XXXXXXXXXXXXXXXXXXXX" | "1234567890123456" | 0           | "1125"              | "777" | 110000 | 10000 | ""                | "YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY" | " 446PAYMENT   XXXXXXXXXXXXXXXXXXXX1234567890123456    001125777    1100000000010000                    YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY                                                                                                                                                                                                                                                       " || "참고2. string 데이터 예제 결제 string 데이터"
    }

    def "CancelMessageBuilder 정상 처리 테스트"() {
        when:
        def message = MessageBuilder.newCancelMessageBuilder()
                .id(transactionId)
                .cardNumber(cardNumber)
                .expirationMonthYear(expirationMonthYear)
                .cvc(cvc)
                .installment(installment)
                .amount(amount)
                .vat(vat)
                .payTransactionId(paytransactionId)
                .encryptedCardInformation(encryptedCardInformation)
                .build()

        then:
        message.length() == 450
        message == expectedMessage


        where:
        transactionId          | cardNumber         | installment | expirationMonthYear | cvc   | amount | vat   | paytransactionId       | encryptedCardInformation                                                                               | expectedMessage                                                                                                                                                                                                                                                                                                                                                                                                                                                      || description
        "ZZZZZZZZZZZZZZZZZZZZ" | "1234567890123456" | 0           | "1125"              | "777" | 110000 | 10000 | "XXXXXXXXXXXXXXXXXXXX" | "YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY" | " 446CANCEL    ZZZZZZZZZZZZZZZZZZZZ1234567890123456    001125777    1100000000010000XXXXXXXXXXXXXXXXXXXXYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY                                                                                                                                                                                                                                                       " || "참고2. string 데이터 예제 전체취소 string 데이터"
    }

    @Unroll
    def "CancelMessageBuilder : #description 이면 throw IllegalArgumentException"() {
        when:
        MessageBuilder.newCancelMessageBuilder()
                .id(transactionId)
                .cardNumber(cardNumber)
                .expirationMonthYear(expirationMonthYear)
                .cvc(cvc)
                .installment(installment)
                .amount(amount)
                .vat(vat)
                .payTransactionId(paytransactionId)
                .encryptedCardInformation(encryptedCardInformation)
                .build()

        then:
        thrown(IllegalArgumentException)


        where:
        transactionId           | cardNumber          | installment | expirationMonthYear | cvc    | amount       | vat   | paytransactionId        | encryptedCardInformation || description
        null                    | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "transactionId is null"
        ""                      | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "transactionId is empty"
        "2005041804537662699"   | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "transactionId length is short than 20"
        "200504180453766269988" | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "transactionId length is longer than 20"
        "20050418045376626998"  | null                | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cardNumber is null"
        "20050418045376626998"  | ""                  | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cardNumber is empty"
        "20050418045376626998"  | "123456789"         | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cardNumber length is short than 10"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cardNumber length is longer than 16"
        "20050418045376626998"  | "12345678901234567" | 0           | null                | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "expirationMonthYear is null"
        "20050418045376626998"  | "12345678901234567" | 0           | ""                  | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "expirationMonthYear is empty"
        "20050418045376626998"  | "12345678901234567" | 0           | "111"               | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "expirationMonthYear length is short than 4"
        "20050418045376626998"  | "12345678901234567" | 0           | "11255"             | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "expirationMonthYear length is longer than 4"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | null   | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cvc is null"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | ""     | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cvc is empty"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "77"   | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cvc length is short than 3"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "7777" | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "cvc length is longer than 3"
        "20050418045376626998"  | "12345678901234567" | -1          | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "installment is less than 0"
        "20050418045376626998"  | "12345678901234567" | 13          | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "installment is greater than 12"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "777"  | null         | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "amount is null"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "777"  | 99           | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "amount is less than 100"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "777"  | 1_000_000_01 | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "amount is greater than 1_000_000_00"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "777"  | 110000       | null  | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "vat is null"
        "20050418045376626998"  | "12345678901234567" | 0           | "1125"              | "777"  | 110000       | -1    | "XXXXXXXXXXXXXXXXXXXX"  | "YYYYYYYYYYYYYYYYYYYYYY" || "vat is minus"
        "20050418045376626998"  | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | null                    | "YYYYYYYYYYYYYYYYYYYYYY" || "payTransactionId is null"
        "20050418045376626998"  | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | ""                      | "YYYYYYYYYYYYYYYYYYYYYY" || "payTransactionId is empty"
        "2005041804537662699"   | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "2005041804537662699"   | "YYYYYYYYYYYYYYYYYYYYYY" || "payTransactionId length is short than 20"
        "200504180453766269988" | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "200504180453766269988" | "YYYYYYYYYYYYYYYYYYYYYY" || "payTransactionId length is longer than 20"
        "20050418045376626998"  | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | null                     || "payTransactionId is null"
        "20050418045376626998"  | "1234567890123456"  | 0           | "1125"              | "777"  | 110000       | 10000 | "XXXXXXXXXXXXXXXXXXXX"  | ""                       || "payTransactionId is empty"
    }
}
