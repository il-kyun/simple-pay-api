package com.pay.api.domain;

import com.google.common.base.Strings;
import lombok.Getter;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.util.StringUtils.hasText;


class MessageBuilder {

    private static final int DEFAULT_LENGTH = 450;
    private static final String PAYMENT_TYPE = "PAYMENT";
    private static final String CANCEL_TYPE = "CANCEL";

    private final String length = "446";
    private final String type;
    private String id;
    private String cardNumber;
    private String installment;
    private String expirationMonthYear;
    private String cvc;
    private String amount;
    private String vat;
    private String payTransactionId = "";
    private String encryptedCardInformation;
    private final String temp = "";

    private MessageBuilder(String type) {
        this.type = type;
    }

    MessageBuilder id(String id) {
        checkArgument(hasText(id) && id.length() == 20, "illegal id");
        this.id = id;
        return this;
    }

    MessageBuilder cardNumber(String cardNumber) {
        checkArgument(hasText(cardNumber) && cardNumber.length() >= 10 && cardNumber.length() <= 16, "illegal cardNumber");

        this.cardNumber = cardNumber;
        return this;
    }


    MessageBuilder expirationMonthYear(String expirationMonthYear) {
        checkArgument(hasText(expirationMonthYear) && expirationMonthYear.length() == 4, "illegal expirationMonthYear");

        this.expirationMonthYear = expirationMonthYear;
        return this;
    }

    MessageBuilder cvc(String cvc) {
        checkArgument(hasText(cvc) && cvc.length() == 3, "illegal cvc");

        this.cvc = cvc;
        return this;
    }


    MessageBuilder installment(int installment) {
        checkArgument(installment >= 0 && installment <= 12, "illegal installment");

        this.installment = String.valueOf(installment);
        return this;
    }

    MessageBuilder amount(long amount) {
        checkArgument(amount >= 100 && amount <= 1_000_000_000, "illegal amount");

        this.amount = String.valueOf(amount);
        return this;
    }

    MessageBuilder vat(long vat) {
        checkArgument(vat > -1, "illegal vat");
        this.vat = String.valueOf(vat);
        return this;
    }

    MessageBuilder payTransactionId(String payTransactionId) {
        checkArgument(hasText(payTransactionId) && payTransactionId.length() == 20, "illegal payTransactionId");
        this.payTransactionId = payTransactionId;
        return this;
    }

    MessageBuilder encryptedCardInformation(String encryptedCardInformation) {
        checkArgument(hasText(encryptedCardInformation), "illegal encryptedCardInformation");
        this.encryptedCardInformation = encryptedCardInformation;
        return this;
    }

    static MessageBuilder newPaymentMessageBuilder() {
        return new MessageBuilder(PAYMENT_TYPE);
    }

    static MessageBuilder newCancelMessageBuilder() {
        return new MessageBuilder(CANCEL_TYPE);
    }

    String build() {
        final StringBuilder stringBuilder = new StringBuilder(DEFAULT_LENGTH);

        for (DataField field : DataField.values()) {
            final BiFunction<Integer, String, String> generator = field.getDataType().getGenerator();
            final String value = field.getValueSelector().apply(this);
            final int length = field.getLength();

            final String str = generator.apply(length, value);
            stringBuilder.append(str);
        }

        return stringBuilder.toString();
    }

    enum DataField {
        DATA_LENGTH(DataType.NUMBER, mb -> mb.length, 4),
        DATA_TYPE(DataType.STRING, mb -> mb.type, 10),
        UID(DataType.STRING, mb -> mb.id, 20),
        CARD_NUMBER(DataType.NUMBER_L, mb -> mb.cardNumber, 20),
        INSTALLMENT(DataType.NUMBER_0, mb -> mb.installment, 2),
        EXPIRATION_MONTH_YEAR(DataType.NUMBER_L, mb -> mb.expirationMonthYear, 4),
        CVC(DataType.NUMBER_L, mb -> mb.cvc, 3),
        AMOUNT(DataType.NUMBER, mb -> mb.amount, 10),
        VAT(DataType.NUMBER_0, mb -> mb.vat, 10),
        ORIGIN_TRANSACTION_ID(DataType.STRING, mb -> mb.payTransactionId, 20),
        ENCRYPTED_CARD_INFO(DataType.STRING, mb -> mb.encryptedCardInformation, 300),
        TEMP(DataType.STRING, mb -> mb.temp, 47);

        @Getter
        private DataType dataType;
        @Getter
        private Function<MessageBuilder, String> valueSelector;
        @Getter
        private int length;

        DataField(DataType dataType, Function<MessageBuilder, String> valueSelector, int length) {
            this.dataType = dataType;
            this.valueSelector = valueSelector;
            this.length = length;
        }
    }

    enum DataType {
        NUMBER(((integer, value) -> Strings.padStart(value, integer, ' '))),
        NUMBER_0(((integer, value) -> Strings.padStart(value, integer, '0'))),
        NUMBER_L(((integer, value) -> Strings.padEnd(value, integer, ' '))),
        STRING(((integer, value) -> Strings.padEnd(value, integer, ' ')));

        @Getter
        BiFunction<Integer, String, String> generator;

        DataType(BiFunction<Integer, String, String> generator) {
            this.generator = generator;
        }
    }

}