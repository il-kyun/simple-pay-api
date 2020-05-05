package com.pay.api.domain.card;


import lombok.Getter;

import javax.persistence.*;

import static java.util.Objects.requireNonNull;

@Getter
@Entity
public class CardCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "MESSAGE", nullable = false, length = 500)
    private String message;

    public CardCompany() {
    }

    public CardCompany(String message) {
        this.message = requireNonNull(message);
    }
}
