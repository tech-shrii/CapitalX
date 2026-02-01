package com.app.portfolio.dto.asset;

import com.app.portfolio.beans.Asset;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetRequest {

    @NotBlank(message = "Asset name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Category is required")
    private Asset.AssetCategory category;

    @Size(max = 50)
    private String symbol;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Buying rate is required")
    @DecimalMin(value = "0", message = "Buying rate must be non-negative")
    private BigDecimal buyingRate;

    @NotNull(message = "Purchase date is required")
    private LocalDate purchaseDate;

    @Size(max = 3)
    private String currency;
}
