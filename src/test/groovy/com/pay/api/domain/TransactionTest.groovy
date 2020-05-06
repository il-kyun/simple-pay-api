package com.pay.api.domain

import com.pay.api.exception.BadRequestException
import com.pay.api.exception.IllegalStatusException
import com.pay.api.type.TransactionType
import spock.lang.Specification
import spock.lang.Unroll

class TransactionTest extends Specification {

    @Unroll
    def "결제 시 #description 이면 IllegalArgumentException 이 발생한다."() {
        when:
        Transaction.newInstance(cardNumber, expirationMonthYear, cvc, installment, amount, vat)

        then:
        thrown(IllegalArgumentException)

        where:
        cardNumber          | expirationMonthYear | cvc    | installment | amount        | vat       || description
        null                | "1125"              | "777"  | 0           | 1_000_000_000 | 1_000_000 || "cardNumber is null"
        ""                  | "1125"              | "777"  | 0           | 1_000_000_000 | 1_000_000 || "cardNumber is empty"
        " "                 | "1125"              | "777"  | 0           | 1_000_000_000 | 1_000_000 || "cardNumber is space"
        "123456789"         | "1125"              | "777"  | 0           | 1_000_000_000 | 1_000_000 || "cardNumber length is short than 10"
        "12345678901234567" | "1125"              | "777"  | 0           | 1_000_000_000 | 1_000_000 || "cardNumber length is longer than 16"
        "1234567890123456"  | null                | "777"  | 0           | 1_000_000_000 | 1_000_000 || "expirationMonthYear is null"
        "1234567890123456"  | ""                  | "777"  | 0           | 1_000_000_000 | 1_000_000 || "expirationMonthYear is empty"
        "1234567890123456"  | " "                 | "777"  | 0           | 1_000_000_000 | 1_000_000 || "expirationMonthYear is space"
        "1234567890123456"  | "123"               | "777"  | 0           | 1_000_000_000 | 1_000_000 || "expirationMonthYear length is short than 4"
        "1234567890123456"  | "11251"             | "777"  | 0           | 1_000_000_000 | 1_000_000 || "expirationMonthYear length is longer than 4"
        "1234567890123456"  | "1125"              | null   | 0           | 1_000_000_000 | 1_000_000 || "cvc is null"
        "1234567890123456"  | "1125"              | ""     | 0           | 1_000_000_000 | 1_000_000 || "cvc is empty"
        "1234567890123456"  | "1125"              | " "    | 0           | 1_000_000_000 | 1_000_000 || "cvc is space"
        "1234567890123456"  | "1125"              | "77"   | 0           | 1_000_000_000 | 1_000_000 || "cvc length is short than 3"
        "1234567890123456"  | "1125"              | "7777" | 0           | 1_000_000_000 | 1_000_000 || "cvc length is longer than 3"
        "1234567890123456"  | "1125"              | "777"  | null        | 1_000_000_000 | 1_000_000 || "installment is null"
        "1234567890123456"  | "1125"              | "777"  | -1          | 1_000_000_000 | 1_000_000 || "installment is less than 0"
        "1234567890123456"  | "1125"              | "777"  | 13          | 1_000_000_000 | 1_000_000 || "installment is greater than 12"
        "1234567890123456"  | "1125"              | "777"  | 0           | null          | 1_000_000 || "amount is null"
        "1234567890123456"  | "1125"              | "777"  | 0           | 99            | 1_000_000 || "amount is less than 100"
        "1234567890123456"  | "1125"              | "777"  | 0           | 1_000_000_001 | 1_000_000 || "amount is greater than 1_000_000_00"
    }

    @Unroll
    def "취소 시 #description 이면 NPE 가 발생한다."() {
        given:
        def cardNumber = "123456789012345"
        def expirationMonthYear = "1212"
        def cvc = "123"
        def installment = 0
        def amount = 11000
        def vat = 1000
        def transaction = Transaction.newInstance(cardNumber, expirationMonthYear, cvc, installment, amount, vat)

        when:
        transaction.cancel(requestedAmount, requestedVat)

        then:
        thrown(NullPointerException)

        where:
        requestedAmount | requestedVat || description
        null            | 1000         || "requestedAmount is null"
    }

    @Unroll
    def "취소 시 #description 이면 IllegalStatusException 이 발생한다."() {
        given:
        def cardNumber = "123456789012345"
        def expirationMonthYear = "1212"
        def cvc = "123"
        def installment = 0
        def amount = 11000
        def vat = 1000
        def transaction = Transaction.newInstance(cardNumber, expirationMonthYear, cvc, installment, amount, vat)

        when:
        transaction.cancel(requestedAmount, requestedVat)

        then:
        thrown(IllegalStatusException)

        where:
        requestedAmount | requestedVat || description
        100             | 1000         || "requestedVat > requestedAmount"
        11001           | 1000         || "this.remainAmount - requestedAmount  < 0"
        11000           | 1001         || "this.remainVat - requestedVat  < 0"
        11000           | 999          || "this.remainAmount - requestedAmount == 0 && this.remainVat - requestedVat > 0"
    }


    @Unroll
    def "#description : BadRequestException 이 발생한다."() {
        when:
        Transaction.newInstance(cardNumber, expirationMonthYear, cvc, installment, amount, vat)

        then:
        thrown(BadRequestException)

        where:
        cardNumber        | expirationMonthYear | cvc   | installment | amount      | vat           || description
        "123456789012345" | "1212"              | "123" | 12          | 100_000_000 | 1_000_000_000 || "if vat is greater than amount"
    }

    def "vat 이 null 인 경우 round(amount/11)로 자동 계산된다."() {
        given:
        def amount = 20000
        def vat = null
        when:
        def transaction = Transaction.newInstance("123456789012345", "1212", "123", 0, amount, vat)

        then:
        transaction.amount == 20000
        transaction.vat == 1818
    }

    def "transaction 생성 시 20자리 transactionId가 자동으로 생성된다."() {
        when:
        def transaction = Transaction.newInstance("123456789012345", "1212", "123", 0, 10000, 909)

        then:
        transaction.transactionId != null
        !transaction.transactionId.isEmpty()
        transaction.transactionId.length() == 20
    }

    def "transaction type 이 PAY 이면서 취소할 잔액이 있으면 isCancellableTransaction 이 true 이다."() {
        when:
        def transaction = new Transaction()
        transaction.transactionType = TransactionType.PAY
        transaction.remainAmount = 1

        then:
        transaction.isCancellableTransaction()
    }

    def "transaction type 이 PAY 이면서 취소할 잔액이 없으면 isCancellableTransaction 이 false 이다."() {
        when:
        def transaction = new Transaction()
        transaction.transactionType = TransactionType.PAY
        transaction.remainAmount = 0

        then:
        !transaction.isCancellableTransaction()
    }

    def "transaction type 이 CANCEL 이면 isCancellableTransaction 이 false 이다."() {
        when:
        def transaction = new Transaction()
        transaction.transactionType = TransactionType.CANCEL

        then:
        !transaction.isCancellableTransaction()
    }
}
