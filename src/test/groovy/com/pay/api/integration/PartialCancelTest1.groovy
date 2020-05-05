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
class PartialCancelTest1 extends Specification {

    private static final String URI = "/pay/transactions"

    @Autowired
    private TestRestTemplate restTemplate

    @Shared
    private static String transactionId = ""

    def "1. 11,000(1,000)원 결제 성공"() {
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


    def "2. 1,100(100)원 취소 성공"() {
        given:
        def amount = 1_100
        def vat = 100

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.remainAmount == 9900
        response.body.remainVat == 900
    }

    def "3. 3,300원 취소 성공"() {
        given:
        def amount = 3300
        def vat = null

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.remainAmount == 6600
        response.body.remainVat
    }

    def "4. 7,000원 취소하려 했으나 남은 결제금액 보다 커서 실패"() {
        given:
        def amount = 7000
        def vat = null

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.NOT_ACCEPTABLE
    }

    def "5. 6,600(700)원 취소하려 했으나 남은 부 가가치세보다 취소요청 부가가치세가 커서 실패"() {
        given:
        def amount = 6600
        def vat = 700

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.NOT_ACCEPTABLE
    }

    def "6. 6,600(600)원 성공"() {
        given:
        def amount = 6600
        def vat = 600

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

    def "7. 100원 취소하려했으나 남은 결제금액이 없어서 실패"() {
        given:
        def amount = 100
        def vat = null

        CancelRequest request = new CancelRequest()
        request.setAmount(amount)
        request.setVat(vat)

        HttpEntity<CancelRequest> httpEntity = new HttpEntity<>(request)

        when:
        ResponseEntity<CancelResponse> response = restTemplate.exchange(URI + "/${transactionId}", HttpMethod.DELETE, httpEntity, CancelResponse.class)

        then:
        response.statusCode == HttpStatus.NOT_ACCEPTABLE
    }
}