package Funding.Startreum.domain.reward.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RewardUpdateRequest(
        @NotBlank(message = "리워드 설명은 필수입니다.")
        String description,  // 리워드 설명

        @NotNull(message = "리워드 금액은 필수입니다.")
        @Min(value = 1, message = "리워드 금액은 1 이상이어야 합니다.")
        BigDecimal amount    // 리워드 금액
) {

}