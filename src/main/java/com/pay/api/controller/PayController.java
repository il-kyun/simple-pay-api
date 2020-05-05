package com.pay.api.controller;

import com.pay.api.controller.cancel.CancelRequest;
import com.pay.api.controller.cancel.CancelResponse;
import com.pay.api.controller.find.FindResponse;
import com.pay.api.controller.pay.PayRequest;
import com.pay.api.controller.pay.PayResponse;
import com.pay.api.domain.PayService;
import com.pay.api.exception.FieldErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/pay/transactions")
public class PayController {

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    @PostMapping
    public ResponseEntity<PayResponse> pay(@Valid @RequestBody PayRequest payRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new FieldErrorException(bindingResult);
        }

        final PayResponse payResponse = payService.pay(payRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(payResponse);
    }

    @DeleteMapping("/{transactionId}")
    public CancelResponse cancel(@PathVariable @NotEmpty @Size(min = 20, max = 20) String transactionId, @Valid @RequestBody CancelRequest cancelRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new FieldErrorException(bindingResult);
        }

        return payService.cancel(transactionId, cancelRequest);
    }

    @GetMapping("/{transactionId}")
    public FindResponse find(@PathVariable @NotEmpty @Size(min = 20, max = 20) String transactionId) {
        return payService.find(transactionId);
    }
}