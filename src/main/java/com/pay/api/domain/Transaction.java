package com.pay.api.domain;

import com.google.common.base.Strings;
import com.pay.api.exception.BadRequestException;
import com.pay.api.exception.IllegalStatusException;
import com.pay.api.type.TransactionType;
import lombok.Getter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.*;
import static org.springframework.util.StringUtils.hasText;

@Getter
@OptimisticLocking(type = OptimisticLockType.ALL)
@DynamicUpdate
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"TRANSACTION_ID"}, name = "UK_TRANSACTION_ID")})
public class Transaction {

    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "TRANSACTION_ID", nullable = false, length = 20)
    private String transactionId;

    @Column(name = "TRANSACTION_TYPE", nullable = false, length = 15)
    @Enumerated(value = EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "ENCRYPTED_CARD_INFO", nullable = false, length = 300)
    private String encryptedCardInfo;

    @Column(name = "MESSAGE", nullable = false, length = 500)
    private String message;

    @Column(name = "INSTALLMENT", nullable = false, length = 3)
    private Integer installment;

    @Column(name = "AMOUNT", nullable = false, length = 20)
    private Long amount;

    @Column(name = "VAT", nullable = false, length = 20)
    private Long vat;

    @Column(name = "REMAIN_AMOUNT", nullable = false, length = 20)
    private Long remainAmount;

    @Column(name = "REMAIN_VAT", nullable = false, length = 20)
    private Long remainVat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAY_TRANSACTION_ID")
    private Transaction payTransaction;

    @Column
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "payTransaction")
    private List<Transaction> cancelTransactionList = new ArrayList<>();

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    private transient CardInfoCrypto cardInfoCrypto;

    public Transaction() {
    }

    private Transaction(TransactionType transactionType) {
        this.transactionId = generateTransactionId();
        this.transactionType = transactionType;
    }

    private Transaction(String cardNumber, String expirationMonthYear, String cvc, Integer installment, Long amount, Long vat) {
        this(TransactionType.PAY);

        this.installment = installment;
        this.encryptedCardInfo = CardInfoCrypto.encrypt(this.transactionId, cardNumber, expirationMonthYear, cvc).getEncryptedCardInfo();

        this.amount = amount;
        this.vat = vat;
        this.remainAmount = this.amount;
        this.remainVat = this.vat;

        this.message = MessageBuilder.newPaymentMessageBuilder()
                .id(this.transactionId)
                .cardNumber(cardNumber)
                .expirationMonthYear(expirationMonthYear)
                .cvc(cvc)
                .installment(installment)
                .amount(amount)
                .vat(vat)
                .encryptedCardInformation(this.encryptedCardInfo)
                .build();
    }

    private Transaction(Transaction payTransaction, Long requestedAmount, Long requestedVat) {
        this(TransactionType.CANCEL);

        this.installment = 0;
        this.encryptedCardInfo = payTransaction.getEncryptedCardInfo();

        this.amount = requestedAmount;
        this.vat = requestedVat;
        this.remainAmount = payTransaction.getRemainAmount() - requestedAmount;
        this.remainVat = payTransaction.getRemainVat() - requestedVat;

        this.payTransaction = payTransaction;

        final CardInfoCrypto cardInfoCrypto = payTransaction.getCardInfo();
        this.message = MessageBuilder.newCancelMessageBuilder()
                .id(this.transactionId)
                .cardNumber(cardInfoCrypto.getCardNumber())
                .expirationMonthYear(cardInfoCrypto.getExpirationMonthYear())
                .cvc(cardInfoCrypto.getCvc())
                .installment(this.installment)
                .amount(requestedAmount)
                .vat(requestedVat)
                .payTransactionId(payTransaction.getTransactionId())
                .encryptedCardInformation(this.encryptedCardInfo)
                .build();

        payTransaction.updateRemainAmountAndVat(this.remainAmount, this.remainVat);
    }

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String generateTransactionId() {
        int random = ThreadLocalRandom.current().nextInt(99999);
        String paddedRandom = Strings.padStart(String.valueOf(random), 5, '0');
        return LocalDateTime.now().format(FORMATTER) + paddedRandom;
    }

    static Transaction newInstance(String cardNumber, String expirationMonthYear, String cvc, Integer installment, Long amount, Long vat) {
        checkArgument(hasText(cardNumber) && cardNumber.length() >= 10 && cardNumber.length() <= 16, "illegal cardNumber");
        checkArgument(hasText(expirationMonthYear) && expirationMonthYear.length() == 4, "illegal expirationMonthYear");
        checkArgument(hasText(cvc) && cvc.length() == 3, "illegal cvc");
        checkArgument(nonNull(installment) && installment >= 0 && installment <= 12, "illegal installment");
        checkArgument(nonNull(amount) && amount >= 100 && amount <= 1_000_000_000, "illegal amount");

        if (isNull(vat)) {
            vat = calculateVat(amount);
        }

        //부가가치세는 결제금액보다 클 수 없습니다.
        if (vat > amount) {
            throw new BadRequestException("vat can not be greater than amount");
        }

        return new Transaction(cardNumber, expirationMonthYear, cvc, installment, amount, vat);
    }


    /**
     * optional 데이터이므로 값을 받지 않은 경우, 자동계산 합니다. 자동계산 수식 : 결제금액 / 11, 소수점이하 반올림
     * 결제금액이 1,000원일 경우, 자동계산된 부가가치세는 91원입니다.
     * 부가가치세는 결제금액보다 클 수 없습니다.
     * 결제금액이 1,000원일 때, 부가가치세는 0원일 수 있습니다.
     */
    Transaction cancel(Long requestedAmount, Long requestedVat) {
        requireNonNull(requestedAmount, "requestedAmount is mandatory.");

        if (isNull(requestedVat)) {
            requestedVat = calculateVat(requestedAmount);
            if (this.remainVat - requestedVat < 0) {
                requestedVat = this.remainVat;
            }
        }

        if (requestedVat > requestedAmount) {
            throw new IllegalStatusException("requestedVat > requestedAmount");
        }

        if (this.remainAmount - requestedAmount < 0) {
            throw new IllegalStatusException("this.remainAmount - requestedAmount  < 0");
        }

        if (this.remainVat - requestedVat < 0) {
            throw new IllegalStatusException("this.remainVat - requestedVat  < 0");
        }

        if (this.remainAmount - requestedAmount == 0 && this.remainVat - requestedVat > 0) {
            throw new IllegalStatusException("this.remainAmount - requestedAmount  == 0 && this.remainVat - requestedVat > 0");

        }

        return new Transaction(this, requestedAmount, requestedVat);
    }

    private void updateRemainAmountAndVat(Long remainAmount, Long remainVat) {
        this.remainAmount = remainAmount;
        this.remainVat = remainVat;
    }

    /**
     * 자동계산 수식 : 결제금액 / 11, 소수점이하 반올림
     *
     * @param requestedAmount : 결제 금액
     * @return 부가 가치세
     */
    private static long calculateVat(long requestedAmount) {
        return Math.round(requestedAmount / 11d);
    }

    public String getCardNumber() {
        CardInfoCrypto decryptedCardInfo = getCardInfo();
        return decryptedCardInfo.getCardNumber();
    }

    public String getExpirationMonthYear() {
        CardInfoCrypto decryptedCardInfo = getCardInfo();
        return decryptedCardInfo.getExpirationMonthYear();
    }

    public String getCvc() {
        CardInfoCrypto decryptedCardInfo = getCardInfo();
        return decryptedCardInfo.getCvc();
    }

    private CardInfoCrypto getCardInfo() {
        if (this.cardInfoCrypto != null) {
            return this.cardInfoCrypto;
        }

        final CardInfoCrypto decryptedCardInfo = CardInfoCrypto.decrypt(TransactionType.PAY.equals(this.transactionType) ? this.transactionId : this.payTransaction.getTransactionId(), this.encryptedCardInfo);
        this.cardInfoCrypto = decryptedCardInfo;
        return decryptedCardInfo;
    }

    boolean isCancellableTransaction() {
        return TransactionType.PAY.equals(this.transactionType) && this.remainAmount > 0;
    }
}
