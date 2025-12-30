package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.codegym.lunchbot_be.dto.request.ReconciliationClaimDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationRequestCreateDTO;
import vn.codegym.lunchbot_be.dto.response.*;
import vn.codegym.lunchbot_be.service.RevenueReconciliationService;
import vn.codegym.lunchbot_be.service.RevenueReportService;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants/revenue-reconciliation")
@RequiredArgsConstructor
public class RevenueReconciliationController {

    private final RevenueReconciliationService revenueReconciliationService;
    private final MerchantServiceImpl merchantService;
    private final RevenueReportService revenueReportService;

    /**
     * GET /api/merchants/revenue-reconciliation/monthly?yearMonth=2024-12
     */
    @GetMapping("/monthly")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getMonthlyReconciliation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String yearMonth
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            // Nếu không truyền yearMonth, lấy tháng hiện tại
            YearMonth targetMonth = yearMonth != null
                    ? YearMonth.parse(yearMonth)
                    : YearMonth.now();

            MonthlyRevenueResponse response = revenueReconciliationService
                    .getMonthlyReconciliation(merchantId, targetMonth);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi đối soát doanh thu: " + e.getMessage()));
        }
    }

    // 2. Gửi yêu cầu đối soát (Submit) - MỚI
    @PostMapping("/request")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> createReconciliationRequest(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReconciliationRequestCreateDTO requestDTO
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            ReconciliationRequestResponse response = revenueReconciliationService
                    .createReconciliationRequest(merchantId, requestDTO);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // Lỗi nghiệp vụ (ví dụ: đã tồn tại request)
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi tạo yêu cầu đối soát: " + e.getMessage()));
        }
    }

    // 3. Xem lịch sử/danh sách yêu cầu - MỚI
    @GetMapping("/history")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            List<ReconciliationRequestResponse> history = revenueReconciliationService
                    .getMerchantReconciliationHistory(merchantId);

            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 4. Xem tổng quan (Summary) - MỚI (Tùy chọn, tốt cho Dashboard)
    @GetMapping("/summary")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getSummary(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            ReconciliationSummaryResponse summary = revenueReconciliationService
                    .getReconciliationSummary(merchantId);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    //  Báo cáo sai sót (Claim)
    @PostMapping("/claim")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> submitClaim(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReconciliationClaimDTO claimDTO
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            ReconciliationRequestResponse response = revenueReconciliationService
                    .submitRevenueClaim(merchantId, claimDTO);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi gửi báo cáo: " + e.getMessage()));
        }
    }
    @GetMapping("/detailed-report")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getDetailedRevenueReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String yearMonth
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            YearMonth targetMonth = yearMonth != null
                    ? YearMonth.parse(yearMonth)
                    : YearMonth.now();

            RevenueReportDTO report = revenueReportService.getDetailedRevenueReport(merchantId, targetMonth);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi lấy báo cáo: " + e.getMessage()));
        }
    }
    /**
     * FIX: Export Detailed Revenue Report to Excel
     */
    @GetMapping("/detailed-report/export")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> exportDetailedRevenueReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String yearMonth
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            YearMonth targetMonth = yearMonth != null
                    ? YearMonth.parse(yearMonth)
                    : YearMonth.now();

            RevenueReportDTO reportData = revenueReportService.getDetailedRevenueReport(merchantId, targetMonth);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();

            try {
                createSummarySheet(workbook, reportData);

                if (reportData.getCompletedOrderDetails() != null && !reportData.getCompletedOrderDetails().isEmpty()) {
                    createCompletedOrdersSheet(workbook, reportData);
                }

                if (reportData.getCancelledOrderDetails() != null && !reportData.getCancelledOrderDetails().isEmpty()) {
                    createCancelledOrdersSheet(workbook, reportData);
                }

                workbook.write(outputStream);
                workbook.close();

                byte[] fileContent = outputStream.toByteArray();
                String fileName = String.format("BaoCao_DoanhThu_%s_%s.xlsx",
                        reportData.getMerchantName().replaceAll("[^a-zA-Z0-9]", ""),
                        reportData.getPeriod().replace("/", "-"));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", fileName);
                headers.setContentLength(fileContent.length);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(fileContent);

            } catch (Exception e) {
                workbook.close();
                throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage());
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi xuất báo cáo: " + e.getMessage()));
        }
    }

    /**
     * POST /api/merchants/revenue-reconciliation/claim-with-file
     * Gửi báo cáo sai sót kèm file Excel
     */
    @PostMapping("/claim-with-file")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> submitClaimWithFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String yearMonth,
            @RequestParam String reason,
            @RequestParam(required = false) MultipartFile excelFile
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            YearMonth targetMonth = YearMonth.parse(yearMonth);

            // Tạo ReconciliationClaimDTO
            ReconciliationClaimDTO claimDTO = ReconciliationClaimDTO.builder()
                    .yearMonth(yearMonth)
                    .reason(reason)
                    .build();

            // Gửi claim
            ReconciliationRequestResponse response = revenueReconciliationService
                    .submitRevenueClaim(merchantId, claimDTO);

            // Nếu có file, lưu trữ file
            if (excelFile != null && !excelFile.isEmpty()) {
                saveClaimExcelFile(response.getId(), excelFile, merchantId);
            }

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi gửi báo cáo: " + e.getMessage()));
        }
    }

    /**
     * Helper: Lưu file Excel kèm báo cáo
     */
    private void saveClaimExcelFile(Long requestId, MultipartFile excelFile, Long merchantId) {
        try {
            String fileName = "claim_" + requestId + "_" + System.currentTimeMillis() + ".xlsx";
            String uploadDir = "claims/" + merchantId;

            // Tạo thư mục nếu chưa tồn tại
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // Lưu file
            byte[] bytes = excelFile.getBytes();
            java.nio.file.Path filePath = uploadPath.resolve(fileName);
            java.nio.file.Files.write(filePath, bytes);

            System.out.println("✅ Lưu file claim thành công: " + filePath);

        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi lưu file claim: " + e.getMessage());
            // Không throw exception vì claim đã lưu thành công, chỉ file chưa lưu
        }
    }

    // ============ HELPER METHODS FOR SHEETS ============

    private void createSummarySheet(Workbook workbook, RevenueReportDTO data) {
        Sheet sheet = workbook.createSheet("Tổng quan");
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 4000);

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle percentStyle = createPercentStyle(workbook);

        int rowNum = 0;

        // Tiêu đề
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO DOANH THU CHI TIẾT");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

        rowNum++;

        // Thông tin báo cáo
        addReportInfo(sheet, rowNum++, "Tên nhà hàng:", data.getMerchantName(), headerStyle, dataStyle);
        addReportInfo(sheet, rowNum++, "Kỳ báo cáo:", data.getPeriod(), headerStyle, dataStyle);
        addReportInfo(sheet, rowNum++, "Ngày xuất:", formatDateTimeDisplay(data.getExportedAt()), headerStyle, dataStyle);

        rowNum++;

        // Doanh thu tổng
        Row sectionRow = sheet.createRow(rowNum++);
        sectionRow.createCell(0).setCellValue("DOANH THU TỔNG");
        sectionRow.getCell(0).setCellStyle(headerStyle);

        addNumericInfo(sheet, rowNum++, "Tổng số đơn:", data.getTotalOrders(), headerStyle, dataStyle);
        addNumericInfo(sheet, rowNum++, "Đơn hoàn thành:", data.getCompletedOrders(), headerStyle, dataStyle);
        addNumericInfo(sheet, rowNum++, "Đơn hủy:", data.getCancelledOrders(), headerStyle, dataStyle);
        addCurrencyInfo(sheet, rowNum++, "Doanh thu gộp:", data.getTotalGrossRevenue(), headerStyle, currencyStyle);
        addCurrencyInfo(sheet, rowNum++, "Giá trị đơn trung bình:", data.getAverageOrderValue(), headerStyle, currencyStyle);

        rowNum++;

        // Chi phí và doanh thu ròng
        sectionRow = sheet.createRow(rowNum++);
        sectionRow.createCell(0).setCellValue("CHI PHÍ VÀ DOANH THU RÒNG");
        sectionRow.getCell(0).setCellStyle(headerStyle);

        // Dùng style cho % nhỏ (0.001% chẳng hạn)
        CellStyle smallPercentStyle = createSmallPercentStyle(workbook);
        double commissionPercent = data.getPlatformCommissionRate().doubleValue() * 100;
        addPercentInfo(sheet, rowNum++, "Mức chiết khấu (%):", commissionPercent, headerStyle, smallPercentStyle);
        addCurrencyInfo(sheet, rowNum++, "Tổng phí chiết khấu:", data.getTotalPlatformFee(), headerStyle, currencyStyle);
        addCurrencyInfo(sheet, rowNum++, "Doanh thu ròng (thực nhận):", data.getNetRevenue(), headerStyle, currencyStyle);

        rowNum++;

        // So sánh kỳ trước
        sectionRow = sheet.createRow(rowNum++);
        sectionRow.createCell(0).setCellValue("SO SÁNH KỲ TRƯỚC");
        sectionRow.getCell(0).setCellStyle(headerStyle);

        addCurrencyInfo(sheet, rowNum++, "Doanh thu tháng trước:", data.getPreviousMonthRevenue(), headerStyle, currencyStyle);
        addCurrencyInfo(sheet, rowNum++, "Sự thay đổi (VNĐ):", data.getRevenueChange(), headerStyle, currencyStyle);

        // Dùng CellStyle thông thường cho % (vì giá trị lớn: 11.11%, 5.25%, v.v.)
        CellStyle percentNormalStyle = createPercentStyle(workbook);
        addPercentInfo(sheet, rowNum++, "Thay đổi (%):", data.getRevenueChangePercent().doubleValue(), headerStyle, percentNormalStyle);

        String trendLabel = "Xu hướng: ";
        if ("UP".equals(data.getRevenueChangeStatus())) {
            trendLabel += "📈 Tăng";
        } else if ("DOWN".equals(data.getRevenueChangeStatus())) {
            trendLabel += "📉 Giảm";
        } else {
            trendLabel += "➡️ Không đổi";
        }
        addReportInfo(sheet, rowNum, "Xu hướng:", trendLabel, headerStyle, dataStyle);
    }

    /**
     * FIX: Create Completed Orders Sheet
     */
    private void createCompletedOrdersSheet(Workbook workbook, RevenueReportDTO data) {
        Sheet sheet = workbook.createSheet("Đơn hoàn thành");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 3500);
        sheet.setColumnWidth(2, 3500);
        sheet.setColumnWidth(3, 3000);
        sheet.setColumnWidth(4, 3000);
        sheet.setColumnWidth(5, 3000);

        CellStyle headerStyle = createTableHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Mã đơn", "Ngày đặt", "Hoàn thành", "Tổng mặt hàng", "Giảm giá", "Doanh thu"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (CompletedOrderDTO order : data.getCompletedOrderDetails()) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(order.getOrderNumber());
            row.getCell(0).setCellStyle(dataStyle);

            row.createCell(1).setCellValue(formatDateTimeDisplay(order.getOrderDate()));
            row.getCell(1).setCellStyle(dataStyle);

            row.createCell(2).setCellValue(formatDateTimeDisplay(order.getCompletedAt()));
            row.getCell(2).setCellStyle(dataStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue(order.getItemsTotal().doubleValue());
            cell3.setCellStyle(currencyStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue(order.getDiscountAmount().doubleValue());
            cell4.setCellStyle(currencyStyle);

            Cell cell5 = row.createCell(5);
            cell5.setCellValue(order.getRevenue().doubleValue());
            cell5.setCellStyle(currencyStyle);
        }
    }

    /**
     * FIX: Create Cancelled Orders Sheet
     */
    private void createCancelledOrdersSheet(Workbook workbook, RevenueReportDTO data) {
        Sheet sheet = workbook.createSheet("Đơn hủy");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 3500);
        sheet.setColumnWidth(2, 3500);
        sheet.setColumnWidth(3, 5000);
        sheet.setColumnWidth(4, 3000);

        CellStyle headerStyle = createTableHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Mã đơn", "Ngày đặt", "Hủy lúc", "Lý do hủy", "Hủy bởi"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (CancelledOrderDTO order : data.getCancelledOrderDetails()) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(order.getOrderNumber());
            row.getCell(0).setCellStyle(dataStyle);

            row.createCell(1).setCellValue(formatDateTimeDisplay(order.getOrderDate()));
            row.getCell(1).setCellStyle(dataStyle);

            row.createCell(2).setCellValue(formatDateTimeDisplay(order.getCancelledAt()));
            row.getCell(2).setCellStyle(dataStyle);

            String reason = order.getCancellationReason() != null ? order.getCancellationReason() : "Không rõ";
            row.createCell(3).setCellValue(reason);
            row.getCell(3).setCellStyle(dataStyle);

            String cancelledBy = "MERCHANT".equals(order.getCancelledBy()) ? "Nhà hàng" : "Khách hàng";
            row.createCell(4).setCellValue(cancelledBy);
            row.getCell(4).setCellStyle(dataStyle);
        }
    }

    // ============ CELL STYLE HELPERS ============

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createTableHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // Format cho số thập phân: hiển thị 2 chữ số sau dấu phẩy
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    /**
     * Style riêng cho % nhỏ (0.001% chẳng hạn)
     */
    private CellStyle createSmallPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // Format cho % rất nhỏ: hiển thị 4 chữ số sau dấu phẩy
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0000"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    // ============ DATA HELPERS (FIXED) ============

    /**
     * FIX: Format LocalDateTime to String (not vice versa)
     */
    private String formatDateTimeDisplay(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        try {
            return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * FIX: Format String to display (alternative if input is String)
     */
    private String formatDateTimeDisplay(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "N/A";
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateString);
            return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (Exception e) {
            return dateString;
        }
    }

    /**
     * FIX: Add report info
     */
    private void addReportInfo(Sheet sheet, int rowNum, String label, String value,
                               CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell cell1 = row.createCell(0);
        cell1.setCellValue(label);
        cell1.setCellStyle(labelStyle);

        Cell cell2 = row.createCell(1);
        cell2.setCellValue(value);
        cell2.setCellStyle(valueStyle);
    }

    /**
     * FIX: Add numeric info
     */
    private void addNumericInfo(Sheet sheet, int rowNum, String label, int value,
                                CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell cell1 = row.createCell(0);
        cell1.setCellValue(label);
        cell1.setCellStyle(labelStyle);

        Cell cell2 = row.createCell(1);
        cell2.setCellValue(value);
        cell2.setCellStyle(valueStyle);
    }

    /**
     * FIX: Add currency info
     */
    private void addCurrencyInfo(Sheet sheet, int rowNum, String label, BigDecimal value,
                                 CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell cell1 = row.createCell(0);
        cell1.setCellValue(label);
        cell1.setCellStyle(labelStyle);

        Cell cell2 = row.createCell(1);
        cell2.setCellValue(value.doubleValue());
        cell2.setCellStyle(valueStyle);
    }

    /**
     * FIX: Add percent info (overload cho cả double và BigDecimal)
     */
    private void addPercentInfo(Sheet sheet, int rowNum, String label, double value,
                                CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell cell1 = row.createCell(0);
        cell1.setCellValue(label);
        cell1.setCellStyle(labelStyle);

        Cell cell2 = row.createCell(1);
        cell2.setCellValue(value);
        cell2.setCellStyle(valueStyle);
    }

    /**
     * Overload cho BigDecimal
     */
    private void addPercentInfo(Sheet sheet, int rowNum, String label, BigDecimal value,
                                CellStyle labelStyle, CellStyle valueStyle) {
        addPercentInfo(sheet, rowNum, label, value.doubleValue(), labelStyle, valueStyle);
    }
}