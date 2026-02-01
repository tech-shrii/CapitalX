package com.app.portfolio.service.excel;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ExcelService {

    void importFromExcel(MultipartFile file, Long userId) throws IOException;

    byte[] exportToExcel(Long userId) throws IOException;
}
