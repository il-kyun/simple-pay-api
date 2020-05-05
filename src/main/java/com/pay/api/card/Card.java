package com.pay.api.card;


import lombok.Getter;

import javax.persistence.*;

@Getter
@Entity
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "MESSAGE", nullable = false, length = 500)
    private String message;

    public Card() {
    }

    public Card(String message) {
        this.message = message;
    }
}
