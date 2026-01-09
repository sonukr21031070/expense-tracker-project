package com.example.backend.model.dto;

import com.example.backend.model.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private String description;
    private ExpenseCategory category;
    private BigDecimal amount;
    private LocalDate expenseDate;
}

