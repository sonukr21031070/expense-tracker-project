package com.example.backend.service;

import com.example.backend.model.dto.IncomeRequest;
import com.example.backend.model.dto.IncomeResponse;
import com.example.backend.model.dto.TotalsResponse;

import java.time.LocalDate;
import java.util.List;

public interface IncomeService {
    IncomeResponse addIncome(Long userId, IncomeRequest request);
    void deleteIncome(Long userId, Long incomeId);
    List<IncomeResponse> listIncomes(Long userId);
    List<IncomeResponse> filterBySource(Long userId, String source);
    List<IncomeResponse> filterByDateRange(Long userId, LocalDate start, LocalDate end);
    List<IncomeResponse> filterByAmountRange(Long userId, String min, String max);
    List<IncomeResponse> searchIncomes(Long userId, String source, LocalDate start, LocalDate end, String minAmount, String maxAmount, String sortBy, String sortDir);
    TotalsResponse totals(Long userId);
}
