package com.app.portfolio.service.excel;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import com.app.portfolio.exceptions.BadRequestException;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelServiceImpl implements ExcelService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AssetRepository assetRepository;

    @Override
    @Transactional
    public void importFromExcel(MultipartFile file, Long userId) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Excel file is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        // Skip header row
        Map<String, Client> clientMap = new HashMap<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String clientName = getCellValueAsString(row.getCell(0));
            String assetName = getCellValueAsString(row.getCell(1));
            String categoryStr = getCellValueAsString(row.getCell(2));
            String symbol = getCellValueAsString(row.getCell(3));
            BigDecimal quantity = getCellValueAsBigDecimal(row.getCell(4));
            BigDecimal buyingRate = getCellValueAsBigDecimal(row.getCell(5));
            java.time.Instant purchaseDateTime = getCellValueAsDate(row.getCell(6)).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            String currency = getCellValueAsString(row.getCell(7));
            BigDecimal sellingRate = getCellValueAsBigDecimal(row.getCell(8));
            java.time.Instant sellingDateTime = row.getCell(9) != null ? getCellValueAsDate(row.getCell(9)).atStartOfDay().toInstant(java.time.ZoneOffset.UTC) : null;
            boolean sold = row.getCell(10) != null && row.getCell(10).getBooleanCellValue();


            if (clientName == null || assetName == null) continue;

            // Get or create client
            Client client = clientMap.get(clientName);
            if (client == null) {
                client = clientRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .filter(c -> c.getName().equalsIgnoreCase(clientName))
                        .findFirst()
                        .orElse(null);

                if (client == null) {
                    client = new Client();
                    client.setUser(user);
                    client.setName(clientName);
                    client.setEmail(clientName.toLowerCase().replaceAll("\\s+", "") + "@client.com");
                    client.setCurrency(currency != null ? currency : "USD");
                    client = clientRepository.save(client);
                }
                clientMap.put(clientName, client);
            }

            // Create asset
            Asset.AssetCategory category;
            try {
                category = Asset.AssetCategory.valueOf(categoryStr.toUpperCase());
            } catch (Exception e) {
                category = Asset.AssetCategory.STOCK;
            }

            Asset asset = new Asset();
            asset.setClient(client);
            asset.setName(assetName);
            asset.setCategory(category);
            asset.setSymbol(symbol != null ? symbol : "");
            asset.setQuantity(quantity);
            asset.setBuyingRate(buyingRate);
            asset.setPurchaseDateTime(purchaseDateTime);
            asset.setCurrency(currency);
            asset.setSellingRate(sellingRate);
            asset.setSellingDateTime(sellingDateTime);
            asset.setSold(sold);
            assetRepository.save(asset);
        }

        workbook.close();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Client> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Portfolio Data");

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Client Name", "Asset Name", "Category", "Symbol", "Quantity", "Buying Rate", "PurchaseDateTime", "Currency", "Selling Rate", "Selling Date Time", "Sold"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        int rowNum = 1;
        for (Client client : clients) {
            List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(client.getId());
            for (Asset asset : assets) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(client.getName());
                row.createCell(1).setCellValue(asset.getName());
                row.createCell(2).setCellValue(asset.getCategory().name());
                row.createCell(3).setCellValue(asset.getSymbol() != null ? asset.getSymbol() : "");
                row.createCell(4).setCellValue(asset.getQuantity().doubleValue());
                row.createCell(5).setCellValue(asset.getBuyingRate().doubleValue());
                row.createCell(6).setCellValue(asset.getPurchaseDateTime().toString());
                row.createCell(7).setCellValue(asset.getCurrency());
                if (asset.getSellingRate() != null) {
                    row.createCell(8).setCellValue(asset.getSellingRate().doubleValue());
                }
                if (asset.getSellingDateTime() != null) {
                    row.createCell(9).setCellValue(asset.getSellingDateTime().toString());
                }
                row.createCell(10).setCellValue(asset.isSold());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        try {
            return new BigDecimal(cell.getStringCellValue());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate getCellValueAsDate(Cell cell) {
        if (cell == null) return LocalDate.now();
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        try {
            return LocalDate.parse(cell.getStringCellValue(), DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
