package com.app.portfolio.dto.profile;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileRequest {

    @Size(max = 100)
    private String name;
}
