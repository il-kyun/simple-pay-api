package com.pay.api.multithread

import com.pay.api.TransactionType
import com.pay.api.controller.cancel.CancelRequest
import com.pay.api.controller.cancel.CancelResponse
import com.pay.api.controller.find.FindResponse
import com.pay.api.controller.pay.PayRequest
import com.pay.api.controller.pay.PayResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * 부분취소 : 결제 한 건에 대한 부분취소를 동시에 할 수 없습니다.
 */
@Stepwise
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultiThreadTest3 extends Specification {

    private static final String URI = "/pay/transactions"

    @Autowired
    private TestRestTemplate restTemplate

    @Shared
    private static String transactionId = ""

    @Shared
    private static successCount = 0

    def "11,000(1,000)원 결제 성공"() {
        given:
        def amount = 11_000
        def vat = 1_000

        def request = new PayRequest()
        request.setCardNumber("123456789012345")
        request.setCvc("123")
        request.setExpirationMonthYear("1212")
        request.setInstallment(0)
        request.setAmount(amount)
        request.setVat(vat)

        when:
        ResponseEntity<PayResponse> response = restTemplate.postForEntity(URI, request, PayResponse.class)
        transactionId = response.body.transactionId

        then:
        response.statusCode == HttpStatus.CREATED
        amount == response.body.amount
        vat == response.body.vat
        println transactionId

    }


    def "3,000(300)원 취소 성공"() {
        given:
        def amount = 3000
        def vat = 300

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        def pool = Executors.newFixedThreadPool(4)
        def conflictCount = 0
        List<ResponseEntity<CancelResponse>> successResponseEntityList = []
        try {
            List<Future<ResponseEntity<CancelResponse>>> futures = (1..4).collect { num ->
                pool.submit({ ->
                    restTemplate.exchange(MultiThreadTest3.URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)
                } as Callable)
            }
            futures.each { it ->
                def res = it.get()
                if (res.statusCode == HttpStatus.OK) {
                    successResponseEntityList.add(res)
                } else {
                    conflictCount++
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            pool.shutdown()
        }

        then:
        conflictCount == 4 - successResponseEntityList.size()
        successResponseEntityList.each { it ->
            it.body.transactionType == TransactionType.CANCEL
            it.body.amount == 3000
            it.body.vat == 300
            it.body.payTransactionId == transactionId

            successCount++
        }
    }

    def "11,000(1,000)원 취소 확인"() {
        when:
        ResponseEntity<FindResponse> response = restTemplate.getForEntity(URI + "/${transactionId}", FindResponse.class)

        then:
        response.body.transactionId == transactionId
        response.body.transactionType == TransactionType.PAY
        response.body.remainAmount == 11000 - (successCount * 3000)
        response.body.remainVat == 1000 - (successCount * 300)
        response.body.cancelTransactionList.size() == successCount
    }
}