package com.webapp.cognitodemo.entity.registration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionItem {
    private String companyName;
    private String role;
    private String duration;
}
