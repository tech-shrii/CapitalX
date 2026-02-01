package com.app.portfolio.controller;

import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.excel.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelService excelService;

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        excelService.importFromExcel(file, userPrincipal.getId());
        return ResponseEntity.ok().body(java.util.Map.of("message", "Excel imported successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        byte[] excelBytes = excelService.exportToExcel(userPrincipal.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "portfolio_export.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}
