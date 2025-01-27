package Funding.Startreum.domain.virtualaccount;

import Funding.Startreum.domain.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "virtual_accounts")
public class VirtualAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountId; // 가상 계좌 ID

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 ID

    private BigDecimal balance; // 현재 잔액

    private LocalDateTime createdAt; // 계좌 생성 일자

    private LocalDateTime updatedAt; // 계좌 업데이트 일자

    private Boolean fundingBlock; // 펀딩 관련 송금 차단 여부
}
