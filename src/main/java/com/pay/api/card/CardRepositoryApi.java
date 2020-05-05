package com.pay.api.card;

import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class CardRepositoryApi implements CardApi {

    private final CardRepository repository;

    public CardRepositoryApi(CardRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public boolean send(String message) {
        try {
            Card card = new Card(message);
            repository.save(card);
        } catch (Exception e) {
            //always success and return true
            //카드사와 통신하는 부분은 Embedded Database(ex. H2)에 string 데이터를 저장하는 것으로 대체하고 카드사로 전송하는 모든 요청은 성공이라고 가정합니다.
        }
        return true;
    }
}
