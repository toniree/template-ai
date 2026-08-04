package com.templateai.sandbox.widget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record WidgetDto(
        String id,
        @NotBlank String name,
        @PositiveOrZero int quantity
) {
}
