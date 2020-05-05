package com.pay.api.domain

import com.pay.api.card.CardApi
import com.pay.api.controller.cancel.CancelRequest
import com.pay.api.controller.find.FindResponse
import com.pay.api.controller.pay.PayRequest
import com.pay.api.domain.PayService
import com.pay.api.domain.Transaction
import com.pay.api.domain.TransactionRepository
import com.pay.api.exception.ConflictException
import com.pay.api.exception.IllegalStatusException
import com.pay.api.exception.TransactionNotFoundException
import spock.lang.Specification

import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

class PayServiceTest extends Specification {

    PayService payService
    TransactionRepository transactionRepository
    CardApi cardApi

    def setup() {
        transactionRepository = Mock()
        cardApi = Mock()
        payService = new PayService(transactionRepository, cardApi)
    }

    def "동일한 카드번호로 다른 요청이 처리되고 있으면 ConflictException 이 발생한다."() {
        given:
        def request = new PayRequest()
        request.cardNumber = "1234567890123456"
        request.expirationMonthYear = "1125"
        request.cvc = "777"
        request.installment = 0
        request.amount = 11000
        request.vat = 1000

        and:
        def cardNumber = "1234567890123456"
        def expirationMonthYear = "1125"
        def cvc = "777"
        def installment = 0
        def amount = 11000
        def vat = 1000
        def transaction = Transaction.newInstance(cardNumber, expirationMonthYear, cvc, installment, amount, vat)
        transaction.createdAt = LocalDateTime.now()
        transaction.remainAmount = 11000
        transaction.remainVat = 1000

        and:
        def pool = Executors.newFixedThreadPool(2)
        def conflictCount = 0

        when:
        try {
            List<Future> futures = (1..2).collect { num ->
                pool.submit({ ->
                    payService.pay(request)
                } as Callable)
            }
            futures.each { it ->
                try {
                    it.get()
                } catch (ExecutionException e) {
                    if (e.getCause().getClass() == ConflictException) {
                        conflictCount++
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            pool.shutdown()
        }

        then:
        1 * transactionRepository.save(_) >> transaction
        1 * cardApi.send(_) >> {
            sleep(3000)
            return true
        }
        conflictCount == 1
    }

    def "트랜잭션 생성 중 익셉션이 발생하면 점유하고 있던 키를 제거한다."() {
        given:
        def request = new PayRequest()
        request.cardNumber = "1234567890123456"
        request.expirationMonthYear = "1125"
        request.cvc = "777"
        request.installment = 0
        request.amount = 11000
        request.vat = 1000

        when:
        payService.pay(request)

        then:
        1 * transactionRepository.save(_) >> { throw new RuntimeException("FOR TEST") }
        0 * cardApi.send(_) >> true
        thrown(RuntimeException)
        !payService.map.containsKey(request.cardNumber)
    }


    def "취소를 요청할 원본 트랜잭션을 찾지 못하면 TransactionNotFoundException 을 발생한다. "() {
        given:
        def transactionId = "20050418045376626998"
        def cancelRequest = new CancelRequest()
        cancelRequest.amount = 10000
        cancelRequest.vat = 1000

        when:
        payService.cancel(transactionId, cancelRequest)

        then:
        1 * transactionRepository.findByTransactionId(_) >> Optional.empty()
        thrown(TransactionNotFoundException)
    }

    def "취소를 요청할 트랜잭션이 결제 트랜잭션이 아니면 IllegalStatusException 을 발생한다."() {
        given:
        def transactionId = "20050418045376626998"
        def cancelRequest = new CancelRequest()
        cancelRequest.amount = 10000
        cancelRequest.vat = 1000

        and:
        def cardNumber = "1234567890123456"
        def expirationMonthYear = "1212"
        def cvc = "123"
        def installment = 0
        def amount = 11000
        def vat = 1000
        def transaction = Transaction.newInstance(cardNumber, expirationMonthYear, cvc, installment, amount, vat)
        transaction.transactionType = "CANCEL"

        when:
        payService.cancel(transactionId, cancelRequest)

        then:
        1 * transactionRepository.findByTransactionId(_) >> Optional.of(transaction)
        thrown(IllegalStatusException)
    }

    def "요청한 트랜잭션 아이디로 트랜잭션을 찾지 못하면 TransactionNotFoundException 이 발생한다."() {
        given:
        def transactionId = "20050418045376626998"

        when:
        payService.find(transactionId)

        then:
        1 * transactionRepository.findByTransactionId(_) >> Optional.empty()
        thrown(TransactionNotFoundException)
    }

    def "요청한 트랜잭션 아이디로 트랜잭션을 찾아서 검증한다."() {
        given:
        def transactionId = "20050418045376626998"

        and:
        def cardNumber = "1234567890123456"
        def expirationMonthYear = "1212"
        def cvc = "123"
        def installment = 0
        def amount = 11000
        def vat = 1000
        def transaction = Transaction.newInstance(cardNumber, expirationMonthYear, cvc, installment, amount, vat)
        transaction.createdAt = LocalDateTime.now()
        transaction.remainAmount = 11000
        transaction.remainVat = 1000

        when:
        def result = payService.find(transactionId)

        then:
        1 * transactionRepository.findByTransactionId(_) >> Optional.of(transaction)
        result instanceof FindResponse
        result.expirationMonthYear == expirationMonthYear
        result.cvc == cvc
        result.installment == installment
        result.amount == amount
        result.vat == vat
    }
}
