package com.example.backend.model.dto;

import com.example.backend.model.IncomeSource;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeResponse {
    private Long id;
    private String description;
    private IncomeSource source;
    private BigDecimal amount;
    private LocalDate incomeDate;
}

