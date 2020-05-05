package com.pay.api.multithread

import com.pay.api.card.CardApi
import com.pay.api.controller.pay.PayRequest
import com.pay.api.domain.PayService
import com.pay.api.domain.Transaction
import com.pay.api.domain.TransactionRepository
import com.pay.api.exception.ConflictException
import spock.lang.Specification

import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * 결제 : 하나의 카드번호로 동시에 결제를 할 수 없습니다.
 */
class MultiThreadTest1 extends Specification {

    PayService payService
    TransactionRepository transactionRepository
    CardApi cardApi

    def setup() {
        transactionRepository = Mock()
        cardApi = Mock()
        payService = new PayService(transactionRepository, cardApi)
    }

    def "결제 : 하나의 카드번호로 동시에 결제를 할 수 없습니다."() {
        given:
        def request = new PayRequest()
        request.cardNumber = "9234567890123456"
        request.expirationMonthYear = "1125"
        request.cvc = "777"
        request.installment = 0
        request.amount = 11000
        request.vat = 1000

        and:
        def cardNumber = "9234567890123456"
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
        def pool = Executors.newFixedThreadPool(10)
        def conflictCount = 0

        try {
            List<Future> futures = (1..10).collect { num ->
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
        conflictCount == 9
        1 * transactionRepository.save(_) >> transaction
        1 * cardApi.send(_) >> {
            sleep(1000)
            return true
        }
    }
}