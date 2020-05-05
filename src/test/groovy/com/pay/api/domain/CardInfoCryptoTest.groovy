package com.pay.api.domain


import spock.lang.Specification
import spock.lang.Unroll

class CardInfoCryptoTest extends Specification {

    @Unroll
    def "CardInfoCrypto test : #description throw IllegalArgumentException"() {
        when:
        CardInfoCrypto.encrypt(transactionId, cardNumber, expirationMonthYear, cvc)

        then:
        thrown(IllegalArgumentException)

        where:
        transactionId           | cardNumber          | expirationMonthYear | cvc    || description
        null                    | "1234567890123456"  | "1125"              | "777"  || "transactionId is null"
        ""                      | "1234567890123456"  | "1125"              | "777"  || "transactionId is empty"
        "2005041804537662699"   | "1234567890123456"  | "1125"              | "777"  || "transactionId length is short than 20"
        "200504180453766269988" | "1234567890123456"  | "1125"              | "777"  || "transactionId length is longer than 20"
        "20050418045376626998"  | null                | "1125"              | "777"  || "cardNumber is null"
        "20050418045376626998"  | ""                  | "1125"              | "777"  || "cardNumber is empty"
        "20050418045376626998"  | "123456789"         | "1125"              | "777"  || "cardNumber length is short than 10"
        "20050418045376626998"  | "12345678901234567" | "1125"              | "777"  || "cardNumber length is longer than 16"
        "20050418045376626998"  | "1234567890123456"  | null                | "777"  || "expirationMonthYear is null"
        "20050418045376626998"  | "1234567890123456"  | ""                  | "777"  || "expirationMonthYear is empty"
        "20050418045376626998"  | "1234567890123456"  | "123"               | "777"  || "expirationMonthYear length is short than 4"
        "20050418045376626998"  | "1234567890123456"  | "11251"             | "777"  || "expirationMonthYear length is longer than 4"
        "20050418045376626998"  | "1234567890123456"  | "1125"              | null   || "cvc is null"
        "20050418045376626998"  | "1234567890123456"  | "1125"              | ""     || "cvc is empty"
        "20050418045376626998"  | "1234567890123456"  | "1125"              | "77"   || "cvc length is short than 3"
        "20050418045376626998"  | "1234567890123456"  | "1125"              | "7777" || "cvc length is longer than 3"
    }


    @Unroll
    def "CardInfoCrypto rollback test : #description "() {
        when:
        def cardInfoCrypto = CardInfoCrypto.encrypt(transactionId, cardNumber, expirationMonthYear, cvc)

        then:
        def encryptedCardInfo = cardInfoCrypto.getEncryptedCardInfo()
        def decryptedCardInfoCrypto = CardInfoCrypto.decrypt(transactionId, encryptedCardInfo)

        decryptedCardInfoCrypto.getCardNumber() == cardNumber
        decryptedCardInfoCrypto.getExpirationMonthYear() == expirationMonthYear
        decryptedCardInfoCrypto.getCvc() == cvc

        where:
        transactionId          | cardNumber         | expirationMonthYear | cvc   || description
        "20050418045376626998" | "1234567890123456" | "1125"              | "777" || "encrypt data and restore from encrypted data"
    }
}
