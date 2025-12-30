package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.response.RevenueReportDTO;
import java.time.YearMonth;

public interface RevenueReportService {
    RevenueReportDTO getDetailedRevenueReport(Long merchantId, YearMonth yearMonth);
}