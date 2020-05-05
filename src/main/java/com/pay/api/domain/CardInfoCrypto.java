package com.pay.api.domain;

import com.pay.api.exception.CryptoFailException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.util.StringUtils.hasText;


/**
 * 암/복호화 대상 데이터는 카드번호, 유호기간, cvc입니다.
 * 카드정보는 모두 숫자로 이루어져 있으므로 하나의 string으로 합쳐서 암호화하고 데이터들 사이에 적당한 구
 * 분자를 추가해주세요.
 * ex. encrypt(카드정보|유효기간|cvc)
 * 암호화된 데이터를 복화화했을 때 카드정보의 각 데이터를 사용할 수 있도록 객체화해주세요. 암/복호화 방식은 자유롭게 선택합니다.
 */
@Slf4j
class CardInfoCrypto {

    private final String transactionId;
    private String cardNumber;
    private String expirationMonthYear;
    private String cvc;
    private String encryptedCardInfo;

    private CardInfoCrypto(String transactionId, String cardNumber, String expirationMonthYear, String cvc) {
        checkArgument(hasText(transactionId) && transactionId.length() == 20, "illegal transactionId");
        checkArgument(hasText(cardNumber) && cardNumber.length() >= 10 && cardNumber.length() <= 16, "illegal cardNumber");
        checkArgument(hasText(expirationMonthYear) && expirationMonthYear.length() == 4, "illegal expirationMonthYear");
        checkArgument(hasText(cvc) && cvc.length() == 3, "illegal cvc");

        this.transactionId = transactionId;
        this.cardNumber = cardNumber;
        this.expirationMonthYear = expirationMonthYear;
        this.cvc = cvc;
    }

    private CardInfoCrypto(String transactionId, String encryptedCardInfo) {
        checkArgument(hasText(transactionId) && transactionId.length() == 20, "illegal transactionId");
        checkArgument(hasText(encryptedCardInfo), "illegal encryptedCardInfo");

        this.transactionId = transactionId;
        this.encryptedCardInfo = encryptedCardInfo;
    }

    private CardInfoCrypto encrypt() {
        String cardInfo = String.join("|", this.cardNumber, this.expirationMonthYear, this.cvc);
        this.encryptedCardInfo = AES.encrypt(cardInfo, this.transactionId).orElseThrow(() -> new CryptoFailException("fail to encrypt"));

        return this;
    }

    private CardInfoCrypto decrypt() {
        String decrypted = AES.decrypt(this.encryptedCardInfo, this.transactionId).orElseThrow(() -> new CryptoFailException("fail to decrypt"));
        String[] array = decrypted.split("\\|");
        this.cardNumber = array[0];
        this.expirationMonthYear = array[1];
        this.cvc = array[2];

        return this;
    }

    static CardInfoCrypto encrypt(String transactionId, String cardNumber, String expirationMonthYear, String cvc) {
        return new CardInfoCrypto(transactionId, cardNumber, expirationMonthYear, cvc).encrypt();
    }

    static CardInfoCrypto decrypt(String transactionId, String encryptedCardInfo) {
        return new CardInfoCrypto(transactionId, encryptedCardInfo).decrypt();
    }

    String getCardNumber() {
        return cardNumber;
    }

    String getExpirationMonthYear() {
        return expirationMonthYear;
    }

    String getCvc() {
        return cvc;
    }

    String getEncryptedCardInfo() {
        return encryptedCardInfo;
    }

    private static class AES {
        private static final String UTF_8 = StandardCharsets.UTF_8.toString();
        private static final String ALGORITHM = "AES/ECB/PKCS5PADDING";

        private static SecretKeySpec getSecretKeySpec(String secret) {
            byte[] key;
            SecretKeySpec secretKey = null;
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-1");
                key = Arrays.copyOf(sha.digest(secret.getBytes(UTF_8)), 16);
                secretKey = new SecretKeySpec(key, "AES");
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                log.error("Error during SecretKeySpec", e);
            }
            return secretKey;
        }

        private static Optional<String> encrypt(String strToEncrypt, String secret) {
            try {
                SecretKeySpec secretKey = getSecretKeySpec(secret);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                return Optional.of(Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(UTF_8))));
            } catch (Exception e) {
                log.error("Error during encrypting", e);
            }
            return Optional.empty();
        }

        private static Optional<String> decrypt(String strToDecrypt, String secret) {
            try {
                SecretKeySpec secretKey = getSecretKeySpec(secret);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                return Optional.of(new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt))));
            } catch (Exception e) {
                log.error("Error during decrypting", e);
            }
            return Optional.empty();
        }
    }
}
