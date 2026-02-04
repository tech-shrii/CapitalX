package com.app.portfolio.service.statement;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.dto.statement.StatementRequest;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.service.asset.AssetService;
import com.app.portfolio.service.email.EmailService;
import com.app.portfolio.service.pricing.PricingService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementServiceImpl implements StatementService {

    private final ClientRepository clientRepository;
    private final AssetRepository assetRepository;
    private final PricingService pricingService;
    private final EmailService emailService;
    private final AssetService assetService;

    @Override
    @Transactional(readOnly = true)
    public void generateAndSendStatement(StatementRequest request, Long userId) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", request.getClientId()));
        if (!client.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Client", request.getClientId());
        }

        byte[] pdfBytes = generatePdf(client, request.getStatementType());
        String fileName = "Statement_" + client.getName() + "_" + 
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        
        String emailTo = request.getEmailTo() != null ? request.getEmailTo() : client.getEmail();
        emailService.sendStatementEmail(emailTo, client.getName(), pdfBytes, fileName);
    }

    private byte[] generatePdf(Client client, StatementRequest.StatementType statementType) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        try {
            // Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Portfolio Statement", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Client Info
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            document.add(new Paragraph("Client: " + client.getName(), headerFont));
            document.add(new Paragraph("Email: " + client.getEmail(), normalFont));
            if (client.getPhone() != null) {
                document.add(new Paragraph("Phone: " + client.getPhone(), normalFont));
            }
            document.add(new Paragraph("Date: " + java.time.LocalDate.now().toString(), normalFont));
            document.add(new Paragraph(" "));

            List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(client.getId());
            
            if (statementType == StatementRequest.StatementType.BASIC_PNL) {
                addBasicPnLContent(document, client, assets, headerFont, normalFont);
            } else {
                addDetailedContent(document, client, assets, headerFont, normalFont);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addBasicPnLContent(Document document, Client client, List<Asset> assets,
                                     Font headerFont, Font normalFont) throws DocumentException {
        document.add(new Paragraph("Summary", headerFont));
        document.add(new Paragraph(" "));

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        for (Asset asset : assets) {
            BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
            BigDecimal currentPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
            if (currentPrice == null) {
                currentPrice = asset.getBuyingRate();
            }
            BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
            totalInvested = totalInvested.add(invested);
            totalCurrentValue = totalCurrentValue.add(currentValue);
        }

        BigDecimal totalPnL = totalCurrentValue.subtract(totalInvested);
        BigDecimal totalPnLPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? totalPnL.divide(totalInvested, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        String currencySymbol = getCurrencySymbol(client.getCurrency());

        document.add(new Paragraph("Total Invested: " + currencySymbol + totalInvested, normalFont));
        document.add(new Paragraph("Current Value: " + currencySymbol + totalCurrentValue, normalFont));
        document.add(new Paragraph("Profit/Loss: " + currencySymbol + totalPnL, normalFont));
        document.add(new Paragraph("Profit/Loss %: " + totalPnLPercent + "%", normalFont));
    }

    private String getCurrencySymbol(String currencyCode) {
        return switch (currencyCode.toUpperCase()) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY" -> "¥";
            case "INR" -> "₹";
            case "CHF" -> "CHF "; // Swiss Franc
            case "CAD" -> "C$";   // Canadian Dollar
            case "AUD" -> "A$";   // Australian Dollar
            case "CNY" -> "¥";   // Chinese Yuan
            case "SGD" -> "S$";   // Singapore Dollar
            case "HKD" -> "HK$";   // Hong Kong Dollar
            default -> currencyCode + " ";
        };
    }


    private void addDetailedContent(Document document, Client client, List<Asset> assets,
                                    Font headerFont, Font normalFont) throws DocumentException {
        document.add(new Paragraph("Asset Details", headerFont));
        document.add(new Paragraph(" "));

        com.lowagie.text.Table table = new com.lowagie.text.Table(7);
        table.setWidth(100);
        table.setPadding(5);

        // Header row
        table.addCell(new com.lowagie.text.Cell(new Phrase("Asset Name", headerFont)));
        table.addCell(new com.lowagie.text.Cell(new Phrase("Category", headerFont)));
        table.addCell(new com.lowagie.text.Cell(new Phrase("Quantity", headerFont)));
        table.addCell(new com.lowagie.text.Cell(new Phrase("Buying Rate", headerFont)));
        table.addCell(new com.lowagie.text.Cell(new Phrase("Final Price", headerFont)));
        table.addCell(new com.lowagie.text.Cell(new Phrase("P&L", headerFont)));
        table.addCell(new com.lowagie.text.Cell(new Phrase("Purchase Date & Time", headerFont)));

        for (Asset asset : assets) {
            BigDecimal finalPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
            if(finalPrice == null) {
                finalPrice = asset.getBuyingRate();
            }
            BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
            BigDecimal finalValue = finalPrice.multiply(asset.getQuantity());
            BigDecimal pnl = finalValue.subtract(invested);

            String currencySymbol = getCurrencySymbol(asset.getCurrency());

            table.addCell(new com.lowagie.text.Cell(new Phrase(asset.getName(), normalFont)));
            table.addCell(new com.lowagie.text.Cell(new Phrase(asset.getCategory().name(), normalFont)));
            table.addCell(new com.lowagie.text.Cell(new Phrase(asset.getQuantity().toString(), normalFont)));
            table.addCell(new com.lowagie.text.Cell(new Phrase(currencySymbol + asset.getBuyingRate(), normalFont)));
            table.addCell(new com.lowagie.text.Cell(new Phrase(currencySymbol + finalPrice, normalFont)));
            table.addCell(new com.lowagie.text.Cell(new Phrase(currencySymbol + pnl, normalFont)));
            table.addCell(new com.lowagie.text.Cell(new Phrase(asset.getPurchaseDateTime().toString(), normalFont)));
        }

        document.add(table);
    }
}
