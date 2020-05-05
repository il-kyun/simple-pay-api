package com.pay.api.domain.card


import spock.lang.Specification

class CardCompanyRepositoryApiTest extends Specification {
    CardCompanyApi cardCompanyApi
    CardCompanyRepository repository

    def setup() {
        repository = Mock()
        cardCompanyApi = new CardCompanyRepositoryApi(repository)
    }

    def "CardCompanyRepositoryApi 에서 save 중 exception 이 발생해도 결과는 true 이다."() {
        given:
        def message = "ABC"

        when:
        def result = cardCompanyApi.send(message)

        then:
        1 * repository.save(_) >> { throw new RuntimeException("FOR TEST") }
        result
    }

    def "CardCompanyRepositoryApi 에서 message 가 null 이어서 CardCompany 객체 생성 중 NPE 이 발생해도 결과는 true 이다."() {
        given:
        def message = null

        when:
        def result = cardCompanyApi.send(message)

        then:
        result
    }
}