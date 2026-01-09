package com.example.backend.service.impl;

import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Expense;
import com.example.backend.model.ExpenseCategory;
import com.example.backend.model.User;
import com.example.backend.model.dto.ExpenseRequest;
import com.example.backend.model.dto.ExpenseResponse;
import com.example.backend.model.dto.TotalsResponse;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ExpenseResponse addExpense(Long userId, ExpenseRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Expense expense = Expense.builder()
                .description(request.getDescription())
                .category(request.getCategory())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .user(user)
                .build();
        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    public void deleteExpense(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this expense");
        }
        expenseRepository.delete(expense);
    }

    @Override
    public List<ExpenseResponse> listExpenses(Long userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseResponse> filterByCategory(Long userId, String category) {
        ExpenseCategory ec = ExpenseCategory.valueOf(category);
        return expenseRepository.findByUserIdAndCategory(userId, ec).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseResponse> filterByDateRange(Long userId, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseResponse> filterByAmountRange(Long userId, String min, String max) {
        BigDecimal minimum = new BigDecimal(min);
        BigDecimal maximum = new BigDecimal(max);
        return expenseRepository.findByUserIdAndAmountBetween(userId, minimum, maximum).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseResponse> searchExpenses(Long userId, String category, LocalDate start, LocalDate end, String minAmount, String maxAmount, String sortBy, String sortDir) {
        Specification<Expense> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // match user
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), ExpenseCategory.valueOf(category)));
            }
            // date range: support start-only, end-only, or both
            if (start != null && end != null) {
                predicates.add(cb.between(root.get("expenseDate"), start, end));
            } else if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), start));
            } else if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), end));
            }
            // amount range: support min-only, max-only, or both
            try {
                if (minAmount != null && !minAmount.isBlank() && maxAmount != null && !maxAmount.isBlank()) {
                    BigDecimal min = new BigDecimal(minAmount);
                    BigDecimal max = new BigDecimal(maxAmount);
                    predicates.add(cb.between(root.get("amount"), min, max));
                } else if (minAmount != null && !minAmount.isBlank()) {
                    BigDecimal min = new BigDecimal(minAmount);
                    predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), min));
                } else if (maxAmount != null && !maxAmount.isBlank()) {
                    BigDecimal max = new BigDecimal(maxAmount);
                    predicates.add(cb.lessThanOrEqualTo(root.get("amount"), max));
                }
            } catch (NumberFormatException nfe) {
                // ignore invalid numeric filters
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Expense> found = expenseRepository.findAll(spec);

        List<ExpenseResponse> list = found.stream().map(this::mapToResponse).collect(Collectors.toList());

        // Apply sorting in-memory if requested
        if (sortBy != null && !sortBy.isBlank()) {
            Comparator<ExpenseResponse> comp = null;
            if ("amount".equalsIgnoreCase(sortBy)) comp = Comparator.comparing(ExpenseResponse::getAmount);
            else if ("category".equalsIgnoreCase(sortBy)) comp = Comparator.comparing(er -> er.getCategory().name());
            else if ("date".equalsIgnoreCase(sortBy)) comp = Comparator.comparing(ExpenseResponse::getExpenseDate);

            if (comp != null) {
                if ("desc".equalsIgnoreCase(sortDir)) list.sort(comp.reversed());
                else list.sort(comp);
            }
        }

        return list;
    }

    @Override
    public TotalsResponse totals(Long userId) {
        BigDecimal totalExpense = expenseRepository.sumByUserId(userId);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        // total income will be provided by IncomeService; set to zero here, controller may combine
        return TotalsResponse.builder()
                .totalExpense(totalExpense)
                .totalIncome(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO.subtract(totalExpense))
                .build();
    }

    private ExpenseResponse mapToResponse(Expense e) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .description(e.getDescription())
                .category(e.getCategory())
                .amount(e.getAmount())
                .expenseDate(e.getExpenseDate())
                .build();
    }
}
