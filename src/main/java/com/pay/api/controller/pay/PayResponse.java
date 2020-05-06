package com.pay.api.controller.pay;

import com.pay.api.domain.Transaction;
import com.pay.api.type.TransactionType;
import lombok.Getter;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Getter
public class PayResponse {
    private String transactionId;

    private TransactionType transactionType;

    private LocalDateTime createdAt;

    private Integer installment;

    private Long amount;

    private Long vat;

    public PayResponse() {
    }

    public PayResponse(Transaction transaction) {

        this.transactionId = requireNonNull(transaction.getTransactionId());
        this.transactionType = requireNonNull(transaction.getTransactionType());
        this.createdAt = requireNonNull(transaction.getCreatedAt());
        this.installment = requireNonNull(transaction.getInstallment());
        this.amount = requireNonNull(transaction.getAmount());
        this.vat = requireNonNull(transaction.getVat());
    }
}
