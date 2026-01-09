package com.example.backend.controller;

import com.example.backend.model.dto.*;
import com.example.backend.security.UserPrincipal;
import com.example.backend.service.ExpenseService;
import com.example.backend.service.IncomeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private IncomeService incomeService;

    // Add expense
    @PostMapping("/expense")
    public ResponseEntity<ExpenseResponse> addExpense(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse resp = expenseService.addExpense(principal.getUser().getId(), request);
        return ResponseEntity.ok(resp);
    }

    // Remove expense
    @DeleteMapping("/expense/{id}")
    public ResponseEntity<Void> deleteExpense(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id) {
        expenseService.deleteExpense(principal.getUser().getId(), id);
        return ResponseEntity.noContent().build();
    }

    // Add income
    @PostMapping("/income")
    public ResponseEntity<IncomeResponse> addIncome(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody IncomeRequest request) {
        IncomeResponse resp = incomeService.addIncome(principal.getUser().getId(), request);
        return ResponseEntity.ok(resp);
    }

    // Remove income
    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id) {
        incomeService.deleteIncome(principal.getUser().getId(), id);
        return ResponseEntity.noContent().build();
    }

    // List expenses with optional filters and sorting (supports combined filters)
    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseResponse>> listExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {
        Long userId = principal.getUser().getId();

        // Consider any single provided filter as active. This allows partial ranges (start-only, end-only, min-only, max-only).
        boolean anyFilter = (category != null && !category.isBlank())
                || (startDate != null && !startDate.isBlank())
                || (endDate != null && !endDate.isBlank())
                || (minAmount != null && !minAmount.isBlank())
                || (maxAmount != null && !maxAmount.isBlank())
                || (sortBy != null && !sortBy.isBlank());

        if (anyFilter) {
            LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
            LocalDate end = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : null;
            List<ExpenseResponse> list = expenseService.searchExpenses(userId, category != null ? category.toUpperCase() : null, start, end, minAmount, maxAmount, sortBy, sortDir);
            return ResponseEntity.ok(list);
        } else {
            return ResponseEntity.ok(expenseService.listExpenses(userId));
        }
    }

    // List incomes with optional filters and sorting (supports combined filters)
    @GetMapping("/incomes")
    public ResponseEntity<List<IncomeResponse>> listIncomes(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {
        Long userId = principal.getUser().getId();

        boolean anyFilter = (source != null && !source.isBlank())
                || (startDate != null && !startDate.isBlank())
                || (endDate != null && !endDate.isBlank())
                || (minAmount != null && !minAmount.isBlank())
                || (maxAmount != null && !maxAmount.isBlank())
                || (sortBy != null && !sortBy.isBlank());

        if (anyFilter) {
            LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
            LocalDate end = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : null;
            List<IncomeResponse> list = incomeService.searchIncomes(userId, source != null ? source.toUpperCase() : null, start, end, minAmount, maxAmount, sortBy, sortDir);
            return ResponseEntity.ok(list);
        } else {
            return ResponseEntity.ok(incomeService.listIncomes(userId));
        }
    }

    // Totals (combined)
    @GetMapping("/totals")
    public ResponseEntity<TotalsResponse> totals(@AuthenticationPrincipal UserPrincipal principal) {
        TotalsResponse e = expenseService.totals(principal.getUser().getId());
        TotalsResponse i = incomeService.totals(principal.getUser().getId());
        // combine
        TotalsResponse combined = TotalsResponse.builder()
                .totalExpense(e.getTotalExpense())
                .totalIncome(i.getTotalIncome())
                .balance(i.getTotalIncome().subtract(e.getTotalExpense()))
                .build();
        return ResponseEntity.ok(combined);
    }

    // Filters and sorts can be added via query params
}
