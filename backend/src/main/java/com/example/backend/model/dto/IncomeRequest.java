package com.example.backend.model.dto;

import com.example.backend.model.IncomeSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeRequest {

    private String description;

    @NotNull
    private IncomeSource source;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate incomeDate;
}

