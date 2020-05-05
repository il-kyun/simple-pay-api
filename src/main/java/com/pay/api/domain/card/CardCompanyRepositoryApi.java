package com.pay.api.domain.card;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Slf4j
@Service
public class CardCompanyRepositoryApi implements CardCompanyApi {

    private final CardCompanyRepository repository;

    public CardCompanyRepositoryApi(CardCompanyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public boolean send(String message) {
        try {
            CardCompany card = new CardCompany(message);
            repository.save(card);
        } catch (Exception e) {
            log.info("CardCompanyApi error", e);
            //always success and return true
            //카드사와 통신하는 부분은 Embedded Database(ex. H2)에 string 데이터를 저장하는 것으로 대체하고 카드사로 전송하는 모든 요청은 성공이라고 가정합니다.
        }
        return true;
    }
}
