package com.pay.api.integration

import com.pay.api.controller.cancel.CancelRequest
import com.pay.api.controller.cancel.CancelResponse
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

@Stepwise
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartialCancelTest2 extends Specification {

    private static final String URI = "/pay/transactions"

    @Autowired
    private TestRestTemplate restTemplate

    @Shared
    private static String transactionId = ""

    def "20,000(909)원 결제 성공"() {
        given:
        def amount = 20000
        def vat = 909

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


    def "10,000(0)원 취소 성공"() {
        given:
        def amount = 10000
        def vat = 0

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.remainAmount == 10000
        response.body.remainVat == 909
    }


    def "10,000(0)원 취소하려했으나 남은 부가 가치세 금액(909)이 더 크므로 실패"() {
        given:
        def amount = 10000
        def vat = 0

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.NOT_ACCEPTABLE
    }

    def "10,000(909)원 취소 성공"() {
        given:
        def amount = 10000
        def vat = 909

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.remainAmount == 0
        response.body.remainVat == 0
    }
}
