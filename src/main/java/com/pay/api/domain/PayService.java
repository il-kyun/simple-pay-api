package com.pay.api.domain;

import com.pay.api.controller.cancel.CancelRequest;
import com.pay.api.controller.cancel.CancelResponse;
import com.pay.api.controller.find.FindResponse;
import com.pay.api.controller.pay.PayRequest;
import com.pay.api.controller.pay.PayResponse;
import com.pay.api.domain.card.CardCompanyApi;
import com.pay.api.exception.ConflictException;
import com.pay.api.exception.IllegalStatusException;
import com.pay.api.exception.TransactionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class PayService {

    private final ConcurrentMap<String, AtomicInteger> map;

    private final TransactionRepository transactionRepository;
    private final CardCompanyApi cardCompanyApi;

    public PayService(TransactionRepository transactionRepository, CardCompanyApi cardCompanyApi) {
        this.transactionRepository = transactionRepository;
        this.cardCompanyApi = cardCompanyApi;

        this.map = new ConcurrentHashMap<>();
    }

    @Transactional
    public PayResponse pay(PayRequest payRequest) {

        final String cardNumber = payRequest.getCardNumber();

        if (!tryAcquireBy(cardNumber)) {
            throw new ConflictException("Only one request per card number can be processed at the same time !!");
        }

        try {
            Transaction newTransaction = Transaction.newInstance(cardNumber, payRequest.getExpirationMonthYear(), payRequest.getCvc(), payRequest.getInstallment(), payRequest.getAmount(), payRequest.getVat());
            Transaction transaction = transactionRepository.save(newTransaction);

            cardCompanyApi.send(transaction.getMessage());

            return new PayResponse(transaction);

        } catch (Exception e) {
            log.error("Error during create pay transaction", e);
            throw e;
        } finally {
            release(cardNumber);
        }
    }

    private boolean tryAcquireBy(final String cardNumber) {
        return map.computeIfAbsent(cardNumber, key -> new AtomicInteger())
                .getAndIncrement() == 0;
    }

    private void release(final String cardNumber) {
        map.remove(cardNumber);
    }

    @Transactional
    public CancelResponse cancel(String transactionId, CancelRequest cancelRequest) {

        final Transaction targetTransaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("transactionId : " + transactionId));

        if (!targetTransaction.isCancellableTransaction()) {
            throw new IllegalStatusException("Cancellation requests are only available for pay transaction.");
        }

        Transaction cancelTransactionRequest = targetTransaction.cancel(cancelRequest.getAmount(), cancelRequest.getVat());
        Transaction cancelTransaction = transactionRepository.save(cancelTransactionRequest);

        cardCompanyApi.send(cancelTransaction.getMessage());
        return new CancelResponse(cancelTransaction);
    }

    @Transactional(readOnly = true)
    public FindResponse find(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .map(FindResponse::new)
                .orElseThrow(() -> new TransactionNotFoundException("transactionId : " + transactionId));
    }
}
