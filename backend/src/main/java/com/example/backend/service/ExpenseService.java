package com.example.backend.service;

import com.example.backend.model.dto.ExpenseRequest;
import com.example.backend.model.dto.ExpenseResponse;
import com.example.backend.model.dto.TotalsResponse;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {
    ExpenseResponse addExpense(Long userId, ExpenseRequest request);
    void deleteExpense(Long userId, Long expenseId);
    List<ExpenseResponse> listExpenses(Long userId);
    List<ExpenseResponse> filterByCategory(Long userId, String category);
    List<ExpenseResponse> filterByDateRange(Long userId, LocalDate start, LocalDate end);
    List<ExpenseResponse> filterByAmountRange(Long userId, String min, String max);
    // Combined search: any parameter may be null to skip
    List<ExpenseResponse> searchExpenses(Long userId, String category, LocalDate start, LocalDate end, String minAmount, String maxAmount, String sortBy, String sortDir);
    TotalsResponse totals(Long userId);
}
