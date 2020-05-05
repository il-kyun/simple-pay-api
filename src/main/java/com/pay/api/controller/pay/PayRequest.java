package com.pay.api.controller.pay;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.*;

@Getter
@Setter
public class PayRequest {

    @Pattern(regexp = "^[0-9]+$")
    @Size(min = 10, max = 16)
    @NotEmpty
    private String cardNumber;

    @Pattern(regexp = "^0[1-9]|^(11)|^(12)[0-9][0-9]$")
    @Size(min = 4, max = 4)
    @NotEmpty
    private String expirationMonthYear;

    @Pattern(regexp = "^[0-9]+$")
    @Size(min = 3, max = 3)
    @NotEmpty
    private String cvc;

    @NotNull
    @Max(12)
    @Min(0)
    private Integer installment;

    @NotNull
    @Max(1_000_000_000)
    @Min(100)
    private Long amount;

    @Min(0)
    private Long vat;
}
