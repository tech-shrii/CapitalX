package com.app.portfolio.service;

import com.app.portfolio.beans.*;
import com.app.portfolio.repository.*;
import com.app.portfolio.service.excel.ExcelServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExcelServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AssetRepository assetRepository;

    @InjectMocks private ExcelServiceImpl excelService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void importFromExcel_shouldImportAssets_whenValidFile() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(clientRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(clientRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workbook workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var header = sheet.createRow(0);
        header.createCell(0).setCellValue("Client Name");
        header.createCell(1).setCellValue("Asset Name");
        header.createCell(2).setCellValue("Category");
        header.createCell(3).setCellValue("Symbol");
        header.createCell(4).setCellValue("Quantity");
        header.createCell(5).setCellValue("Buying Rate");
        header.createCell(6).setCellValue("Purchase Date");

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue("Client1");
        row.createCell(1).setCellValue("Asset1");
        row.createCell(2).setCellValue("STOCK");
        row.createCell(3).setCellValue("AAPL");
        row.createCell(4).setCellValue(10);
        row.createCell(5).setCellValue(100);
        row.createCell(6).setCellValue(LocalDate.now().toString());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(baos.toByteArray()));

        excelService.importFromExcel(file, 1L);

        verify(assetRepository, atLeastOnce()).save(any());
    }

    @Test
    void importFromExcel_shouldThrow_whenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        assertThatThrownBy(() -> excelService.importFromExcel(file, 1L))
                .isInstanceOf(com.app.portfolio.exceptions.BadRequestException.class);
    }

    @Test
    void exportToExcel_shouldReturnNonEmptyByteArray_whenUserHasAssets() throws Exception {
        User user = new User();
        user.setId(1L);
        Client client = new Client();
        client.setId(2L);
        Asset asset = new Asset();
        asset.setName("Asset1");
        asset.setCategory(Asset.AssetCategory.STOCK);
        asset.setSymbol("AAPL");
        asset.setQuantity(BigDecimal.TEN);
        asset.setBuyingRate(BigDecimal.valueOf(100));
        asset.setPurchaseDate(LocalDate.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(clientRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(client));
        when(assetRepository.findByClientIdOrderByPurchaseDateDesc(2L)).thenReturn(List.of(asset));

        byte[] result = excelService.exportToExcel(1L);

        assertThat(result).isNotEmpty();
    }
}
