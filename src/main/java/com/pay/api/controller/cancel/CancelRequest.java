package com.pay.api.controller.cancel;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;


@Getter
@Setter
public class CancelRequest {

    @NotNull
    @Max(1_000_000_000)
    @Min(0)
    private Long amount;

    @Min(0)
    private Long vat;
}
