package Funding.Startreum.domain.virtualaccount.service;

import Funding.Startreum.domain.funding.entity.Funding;
import Funding.Startreum.domain.funding.service.FundingService;
import Funding.Startreum.domain.project.entity.Project;
import Funding.Startreum.domain.project.repository.ProjectRepository;
import Funding.Startreum.domain.transaction.entity.Transaction;
import Funding.Startreum.domain.transaction.service.TransactionService;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountRefundResponse;
import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static Funding.Startreum.domain.transaction.entity.Transaction.TransactionType.REFUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class AccountRefundServiceTest {

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private AccountQueryService accountQueryService;

    @MockitoBean
    private FundingService fundingService;

    @MockitoBean
    private ProjectRepository projectRepository;

    @Autowired
    private AccountRefundService accountRefundService;

    @Nested
    @DisplayName("refund() 테스트")
    class RefundTests {
        @Test
        void testRefund() {
            int payerAccountId = 1;
            int originalTransactionId = 10;
            BigDecimal refundAmount = BigDecimal.valueOf(50);

            // 1) 원 거래 설정
            Transaction oldTransaction = new Transaction();
            oldTransaction.setTransactionId(originalTransactionId);
            oldTransaction.setAmount(refundAmount);
            Funding oldFunding = new Funding();
            oldFunding.setFundingId(100);
            oldTransaction.setFunding(oldFunding);
            when(transactionService.getTransaction(originalTransactionId)).thenReturn(oldTransaction);

            // 2) 환불 받을 계좌 (결제자) 설정 (초기 잔액 100)
            VirtualAccount payerAccount = new VirtualAccount();
            payerAccount.setAccountId(payerAccountId);
            payerAccount.setBalance(BigDecimal.valueOf(100));
            when(accountQueryService.getAccount(payerAccountId)).thenReturn(payerAccount);

            // 3) 수혜자 계좌 설정 (프로젝트 계좌, 초기 잔액 200)
            VirtualAccount projectAccount = spy(new VirtualAccount());
            projectAccount.setAccountId(2);
            projectAccount.setBalance(BigDecimal.valueOf(200));
            when(accountQueryService.getReceiverAccountByTransactionId(originalTransactionId)).thenReturn(projectAccount);

            // transferTo 메서드 호출 시 잔액 업데이트 모의
            doAnswer(invocation -> {
                // projectAccount에서 refundAmount 차감, payerAccount에 더하기
                payerAccount.setBalance(payerAccount.getBalance().add(refundAmount));
                projectAccount.setBalance(projectAccount.getBalance().subtract(refundAmount));
                return null;
            }).when(projectAccount).transferTo(eq(refundAmount), eq(payerAccount));

            // 4) 펀딩 취소 모의
            Funding canceledFunding = new Funding();
            canceledFunding.setFundingId(101);
            when(fundingService.cancelFunding(oldFunding.getFundingId())).thenReturn(canceledFunding);

            // 5) 환불 거래 생성 모의
            Transaction newTransaction = new Transaction();
            newTransaction.setTransactionId(20);
            LocalDateTime now = LocalDateTime.now();
            newTransaction.setTransactionDate(now);
            when(transactionService.createTransaction(eq(canceledFunding), eq(projectAccount), eq(payerAccount), eq(refundAmount), eq(REFUND)))
                    .thenReturn(newTransaction);

            // 6) 프로젝트 조회 및 currentFunding 업데이트 모의
            Project project = new Project();
            project.setCurrentFunding(BigDecimal.valueOf(80));
            when(projectRepository.findProjectByTransactionId(originalTransactionId)).thenReturn(project);

            // 호출 전 결제자 계좌의 잔액 캡처
            BigDecimal beforeBalance = payerAccount.getBalance();

            AccountRefundResponse response = accountRefundService.refund(payerAccountId, originalTransactionId);

            // transferTo에 의해 payerAccount 잔액은 증가(refundAmount 만큼)
            assertEquals(beforeBalance.add(refundAmount), payerAccount.getBalance(), "환불 후 결제자 계좌 잔액이 갱신되어야 합니다.");
            // 프로젝트의 currentFunding은 환불 금액만큼 차감되어야 함
            assertEquals(BigDecimal.valueOf(80).subtract(refundAmount), project.getCurrentFunding(), "프로젝트 currentFunding이 갱신되어야 합니다.");

            // 응답 검증
            assertEquals(newTransaction.getTransactionId(), response.refundTransactionId());
            assertEquals(originalTransactionId, response.originalTransactionId());
            assertEquals(payerAccountId, response.accountId());
            assertEquals(beforeBalance, response.beforeMoney());
            assertEquals(refundAmount, response.refundAmount());
            assertEquals(payerAccount.getBalance(), response.afterMoney());
            assertEquals(now, response.transactionDate());
        }
    }
}
