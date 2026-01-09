package com.example.backend.service.impl;

import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Income;
import com.example.backend.model.IncomeSource;
import com.example.backend.model.User;
import com.example.backend.model.dto.IncomeRequest;
import com.example.backend.model.dto.IncomeResponse;
import com.example.backend.model.dto.TotalsResponse;
import com.example.backend.repository.IncomeRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.IncomeService;
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
public class IncomeServiceImpl implements IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public IncomeResponse addIncome(Long userId, IncomeRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Income income = Income.builder()
                .description(request.getDescription())
                .source(request.getSource())
                .amount(request.getAmount())
                .incomeDate(request.getIncomeDate())
                .user(user)
                .build();
        Income saved = incomeRepository.save(income);
        return mapToResponse(saved);
    }

    @Override
    public void deleteIncome(Long userId, Long incomeId) {
        Income income = incomeRepository.findById(incomeId).orElseThrow(() -> new ResourceNotFoundException("Income", "id", incomeId));
        if (!income.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this income");
        }
        incomeRepository.delete(income);
    }

    @Override
    public List<IncomeResponse> listIncomes(Long userId) {
        return incomeRepository.findByUserIdOrderByIncomeDateDesc(userId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeResponse> filterBySource(Long userId, String source) {
        IncomeSource is = IncomeSource.valueOf(source);
        return incomeRepository.findByUserIdAndSource(userId, is).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeResponse> filterByDateRange(Long userId, LocalDate start, LocalDate end) {
        return incomeRepository.findByUserIdAndIncomeDateBetween(userId, start, end).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeResponse> filterByAmountRange(Long userId, String min, String max) {
        BigDecimal minimum = new BigDecimal(min);
        BigDecimal maximum = new BigDecimal(max);
        return incomeRepository.findByUserIdAndAmountBetween(userId, minimum, maximum).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeResponse> searchIncomes(Long userId, String source, LocalDate start, LocalDate end, String minAmount, String maxAmount, String sortBy, String sortDir) {
        Specification<Income> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(root.get("source"), IncomeSource.valueOf(source)));
            }
            // date range: support start-only, end-only, or both
            if (start != null && end != null) {
                predicates.add(cb.between(root.get("incomeDate"), start, end));
            } else if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("incomeDate"), start));
            } else if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("incomeDate"), end));
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
        List<Income> found = incomeRepository.findAll(spec);
        List<IncomeResponse> list = found.stream().map(this::mapToResponse).collect(Collectors.toList());

        if (sortBy != null && !sortBy.isBlank()) {
            Comparator<IncomeResponse> comp = null;
            if ("amount".equalsIgnoreCase(sortBy)) comp = Comparator.comparing(IncomeResponse::getAmount);
            else if ("source".equalsIgnoreCase(sortBy)) comp = Comparator.comparing(ir -> ir.getSource().name());
            else if ("date".equalsIgnoreCase(sortBy)) comp = Comparator.comparing(IncomeResponse::getIncomeDate);

            if (comp != null) {
                if ("desc".equalsIgnoreCase(sortDir)) list.sort(comp.reversed());
                else list.sort(comp);
            }
        }

        return list;
    }

    @Override
    public TotalsResponse totals(Long userId) {
        BigDecimal totalIncome = incomeRepository.sumByUserId(userId);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        return TotalsResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(BigDecimal.ZERO)
                .balance(totalIncome)
                .build();
    }

    private IncomeResponse mapToResponse(Income i) {
        return IncomeResponse.builder()
                .id(i.getId())
                .description(i.getDescription())
                .source(i.getSource())
                .amount(i.getAmount())
                .incomeDate(i.getIncomeDate())
                .build();
    }
}
