package com.pay.api.controller.find;

import com.pay.api.TransactionType;
import com.pay.api.domain.Transaction;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.pay.api.controller.find.MaskingUtil.getMaskedCardNumber;
import static java.util.Objects.requireNonNull;

@Getter
public class FindResponse {

    private String transactionId;

    private TransactionType transactionType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String cardNumber;

    private String expirationMonthYear;

    private String cvc;

    private Integer installment;

    private Long amount;

    private Long vat;

    private Long remainAmount;

    private Long remainVat;

    private String payTransactionId;

    private List<FindResponse> cancelTransactionList;

    public FindResponse() {
    }

    public FindResponse(Transaction transaction) {
        this.transactionId = requireNonNull(transaction.getTransactionId());
        this.transactionType = requireNonNull(transaction.getTransactionType());
        this.createdAt = requireNonNull(transaction.getCreatedAt());
        this.updatedAt = transaction.getUpdatedAt();
        this.cardNumber = requireNonNull(transaction.getCardNumber());
        this.expirationMonthYear = requireNonNull(transaction.getExpirationMonthYear());
        this.cvc = requireNonNull(transaction.getCvc());
        this.installment = requireNonNull(transaction.getInstallment());
        this.amount = requireNonNull(transaction.getAmount());
        this.vat = requireNonNull(transaction.getVat());
        this.remainAmount = requireNonNull(transaction.getRemainAmount());
        this.remainVat = requireNonNull(transaction.getRemainVat());
        this.payTransactionId = transaction.getPayTransaction() == null ? null : transaction.getPayTransaction().getTransactionId();
        this.cancelTransactionList = transaction.getCancelTransactionList().stream().map(FindResponse::new).collect(Collectors.toList());
    }

    public String getCardNumber() {
        return getMaskedCardNumber(this.cardNumber);
    }
}
