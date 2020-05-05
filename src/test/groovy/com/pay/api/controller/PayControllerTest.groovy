package com.pay.api.controller

import com.pay.api.controller.cancel.CancelResponse
import com.pay.api.controller.find.FindResponse
import com.pay.api.controller.pay.PayResponse
import com.pay.api.domain.PayService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [PayController])
class PayControllerTest extends Specification {

    private static final String URI = "/pay/transactions"

    @Autowired
    MockMvc mvc

    @SpringBean
    PayService payService = Mock()

    @Unroll
    def "결제 API : #description 400 error"() {
        given:
        def requestBody = """
                {
                    "cardNumber": "$cardNumber",
                    "expirationMonthYear": "$expirationMonthYear",
                    "cvc": "$cvc",
                    "installment": "$installment",
                    "amount": "$amount",
                    "vat" : "$vat"
                }"""

        when:
        def response = mvc.perform(
                post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )

        then:
        0 * payService.pay(_) >> new PayResponse()

        response.andExpect(status().isBadRequest())

        where:
        cardNumber          | expirationMonthYear | cvc    | installment | amount        | vat           || description
        null                | "1212"              | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "card number is null"
        ""                  | "1212"              | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "card number is empty"
        "  "                | "1212"              | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "card number is space"
        "12345678901234567" | "1212"              | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "card number size over 16"
        "123456789"         | "1212"              | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "card number size under 10"
        "a23456789"         | "1212"              | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "card number contains non-number"
        "0123456789"        | null                | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "expirationMonthYear is null"
        "0123456789"        | ""                  | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "expirationMonthYear is empty"
        "0123456789"        | " "                 | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "expirationMonthYear is space"
        "0123456789"        | "12121"             | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "expirationMonthYear size over 4"
        "0123456789"        | "121"               | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "expirationMonthYear size under 4"
        "0123456789"        | "12a"               | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "expirationMonthYear contains non-number"
        "0123456789"        | "1320"              | "123"  | 12          | 1_000_000_000 | 1_000_000_000 || "expirationMonthYear invalid month"
        "0123456789"        | "1212"              | null   | 12          | 1_000_000_000 | 1_000_000_000 || "cvc is null"
        "0123456789"        | "1212"              | ""     | 12          | 1_000_000_000 | 1_000_000_000 || "cvc is empty"
        "0123456789"        | "1212"              | "1234" | 12          | 1_000_000_000 | 1_000_000_000 || "cvc size over 3"
        "0123456789"        | "1212"              | "12 "  | 12          | 1_000_000_000 | 1_000_000_000 || "cvc size under 3"
        "0123456789"        | "1212"              | "12a"  | 12          | 1_000_000_000 | 1_000_000_000 || "cvc contains non-number"
        "0123456789"        | "1212"              | "123"  | null        | 1_000_000_000 | 1_000_000_000 || "installment is null"
        "0123456789"        | "1212"              | "123"  | -1          | 1_000_000_000 | 1_000_000_000 || "installment is minus"
        "0123456789"        | "1212"              | "123"  | 13          | 1_000_000_000 | 1_000_000_000 || "installment is over 12"
        "0123456789"        | "1212"              | "123"  | 12          | null          | 1_000_000_000 || "amount is null"
        "0123456789"        | "1212"              | "123"  | 12          | 99            | 1_000_000_000 || "amount under 100"
        "0123456789"        | "1212"              | "123"  | 12          | 1_000_000_001 | 1_000_000_000 || "amount over 1_000_000_000 100"
    }

    @Unroll
    def "결제 취소 API : #description 400 error"() {
        given:
        def requestBody = """
                {
                    "amount": "$amount",
                    "vat" : "$vat"
                }"""

        when:
        def response = mvc.perform(
                delete(URI + "/${transactionId}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )

        then:
        0 * payService.cancel(_) >> new CancelResponse()

        response.andExpect(status().isBadRequest())

        where:
        transactionId       | amount        | vat           || description
        null                | 1_000_000_000 | 1_000_000_000 || "transactionId is null"
        "  "                | 1_000_000_000 | 1_000_000_000 || "transactionId is space"
        "12345678901234567" | 1_000_000_000 | 1_000_000_000 || "transactionId size over 16"
        "123456789"         | 1_000_000_000 | 1_000_000_000 || "transactionId size under 10"
        "a23456789"         | 1_000_000_000 | 1_000_000_000 || "transactionId contains non-number"
        "0123456789"        | null          | 1_000_000_000 || "amount is null"
        "0123456789"        | 99            | 1_000_000_000 || "amount under 100"
        "0123456789"        | 1_000_000_001 | 1_000_000_000 || "amount over 1_000_000_000 100"
        "0123456789"        | 1000          | 2000          || "vat is greater than amount"
    }

    @Unroll
    def "조회 API : #description 400 error"() {
        when:
        def response = mvc.perform(
                get(URI + "/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
        )

        then:
        0 * payService.find(_) >> new FindResponse()

        response.andExpect(status().isBadRequest())

        where:
        transactionId           || description
        "  "                    || "transactionId is space"
        "123456789012345678901" || "transactionId size over 20"
        "1234567890123456789"   || "transactionId size under 20"
    }


}
