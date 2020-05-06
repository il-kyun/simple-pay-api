package com.pay.api.controller.cancel;

import com.pay.api.domain.Transaction;
import com.pay.api.type.TransactionType;
import lombok.Getter;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Getter
public class CancelResponse {

    private String transactionId;

    private TransactionType transactionType;

    private LocalDateTime createdAt;

    private Long amount;

    private Long vat;

    private Long remainAmount;

    private Long remainVat;

    private String payTransactionId;

    public CancelResponse() {
    }

    public CancelResponse(Transaction transaction) {
        this.transactionId = requireNonNull(transaction.getTransactionId());
        this.transactionType = requireNonNull(transaction.getTransactionType());
        this.createdAt = requireNonNull(transaction.getCreatedAt());
        this.amount = requireNonNull(transaction.getAmount());
        this.vat = requireNonNull(transaction.getVat());
        this.remainAmount = requireNonNull(transaction.getRemainAmount());
        this.remainVat = requireNonNull(transaction.getRemainVat());
        this.payTransactionId = requireNonNull(transaction.getPayTransaction().getTransactionId());
    }
}
