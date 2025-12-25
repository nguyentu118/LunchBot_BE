package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.response.MonthlyRevenueResponse;

import java.time.YearMonth;

public interface RevenueReconciliationService {
    MonthlyRevenueResponse getMonthlyReconciliation(Long merchantId, YearMonth yearMonth);
}
