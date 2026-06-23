package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    private IntentType type;
    private String normalizedText;
    private Integer index;
}

