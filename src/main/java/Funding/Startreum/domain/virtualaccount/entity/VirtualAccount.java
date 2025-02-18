package Funding.Startreum.domain.virtualaccount.entity;

import Funding.Startreum.domain.users.User;
import Funding.Startreum.domain.virtualaccount.exception.NotEnoughBalanceException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = "user") // 순환 참조 방지
@Entity
@Table(name = "virtual_accounts")
public class VirtualAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountId; // 가상 계좌 ID

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 ID

    @Column(nullable = false, precision = 18, scale = 0) // 정수만 저장
    private BigDecimal balance; // 현재 잔액

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 계좌 생성 일자

    private LocalDateTime updatedAt; // 계좌 업데이트 일자

    private Boolean fundingBlock; // 펀딩 관련 송금 차단 여부

    /**
     * 현재 계좌에서 출금하여 대상 계좌로 자금을 이체합니다.
     *
     * @param amount        거래 금액
     * @param targetAccount 입금(또는 환불 입금) 대상 계좌
     * @throws RuntimeException 잔액이 부족할 경우 예외 발생
     */
    public void transferTo(BigDecimal amount, VirtualAccount targetAccount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new NotEnoughBalanceException(this.balance);
        }
        // 출금
        this.balance = this.balance.subtract(amount);
        // 대상 계좌에 입금
        targetAccount.balance = targetAccount.balance.add(amount);
    }
}
