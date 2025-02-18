package Funding.Startreum.domain.virtualaccount.service;

import Funding.Startreum.domain.funding.entity.Funding;
import Funding.Startreum.domain.funding.service.FundingService;
import Funding.Startreum.domain.project.entity.Project;
import Funding.Startreum.domain.project.service.ProjectService;
import Funding.Startreum.domain.transaction.entity.Transaction;
import Funding.Startreum.domain.transaction.service.TransactionService;
import Funding.Startreum.domain.virtualaccount.dto.request.AccountPaymentRequest;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountPaymentResponse;
import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static Funding.Startreum.domain.transaction.entity.Transaction.TransactionType.REMITTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class AccountPaymentServiceTest {

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private FundingService fundingService;

    @MockitoBean
    private AccountQueryService accountQueryService;

    @Autowired
    private AccountPaymentService accountPaymentService;

    @Nested
    @DisplayName("paymentByAccountId() 테스트")
    class PaymentByAccountIdTests {
        @Test
        void testPaymentByAccountId() {
            int accountId = 1;
            int projectId = 100;
            String username = "payer";
            BigDecimal paymentAmount = BigDecimal.valueOf(50);
            AccountPaymentRequest request = new AccountPaymentRequest(projectId, paymentAmount);

            // 프로젝트 객체 설정
            Project project = new Project();
            project.setProjectId(projectId);
            project.setCurrentFunding(BigDecimal.ZERO);
            when(projectService.getProject(projectId)).thenReturn(project);

            // 결제자 계좌 설정 (충전 전 잔액 200)
            VirtualAccount payerAccount = new VirtualAccount();
            payerAccount.setAccountId(accountId);
            payerAccount.setBalance(BigDecimal.valueOf(200));
            when(accountQueryService.getAccount(accountId)).thenReturn(payerAccount);

            // 프로젝트 수혜자 계좌 설정 (초기 잔액 100)
            VirtualAccount projectAccount = new VirtualAccount();
            projectAccount.setAccountId(2);
            projectAccount.setBalance(BigDecimal.valueOf(100));
            when(accountQueryService.getAccountByProjectId(projectId)).thenReturn(projectAccount);

            // 펀딩 생성 모의
            Funding funding = new Funding();
            funding.setFundingId(10);
            when(fundingService.createFunding(eq(project), eq(username), eq(paymentAmount))).thenReturn(funding);

            // 거래 생성 모의
            Transaction transaction = new Transaction();
            transaction.setTransactionId(5);
            LocalDateTime now = LocalDateTime.now();
            transaction.setTransactionDate(now);
            when(transactionService.createTransaction(eq(funding), eq(payerAccount), eq(projectAccount), eq(paymentAmount), eq(REMITTANCE)))
                    .thenReturn(transaction);

            AccountPaymentResponse response = accountPaymentService.paymentByAccountId(accountId, request, username);

            // 내부 로직: payerAccount.transferTo(paymentAmount, projectAccount)
            // 테스트를 위해 실제 transferTo가 호출되었다고 가정(예: 200-50=150)
            assertEquals(BigDecimal.valueOf(150), payerAccount.getBalance(), "결제 후 결제자 계좌 잔액이 갱신되어야 합니다.");
            // 프로젝트 funding은 프로젝트 객체 내부에서 currentFunding이 paymentAmount만큼 증가되어야 함
            assertEquals(paymentAmount, project.getCurrentFunding(), "프로젝트 currentFunding이 갱신되어야 합니다.");

            // 응답 검증
            assertEquals(transaction.getTransactionId(), response.transactionId());
            assertEquals(accountId, response.accountId());
            assertEquals(BigDecimal.valueOf(200), response.beforeMoney());
            assertEquals(paymentAmount, response.chargeAmount());
            assertEquals(payerAccount.getBalance(), response.afterMoney());
            assertEquals(now, response.transactionDate());
        }
    }

    @Nested
    @DisplayName("paymentByUsername() 테스트")
    class PaymentByUsernameTests {
        @Test
        void testPaymentByUsername() {
            int projectId = 200;
            String username = "payerUser";
            BigDecimal paymentAmount = BigDecimal.valueOf(80);
            AccountPaymentRequest request = new AccountPaymentRequest(projectId, paymentAmount);

            // 프로젝트 객체 설정
            Project project = new Project();
            project.setProjectId(projectId);
            project.setCurrentFunding(BigDecimal.ZERO);
            when(projectService.getProject(projectId)).thenReturn(project);

            // 결제자 계좌 설정 (충전 전 잔액 300)
            VirtualAccount payerAccount = new VirtualAccount();
            payerAccount.setAccountId(3);
            payerAccount.setBalance(BigDecimal.valueOf(300));
            when(accountQueryService.getAccount(username)).thenReturn(payerAccount);

            // 프로젝트 수혜자 계좌 설정 (초기 잔액 50)
            VirtualAccount projectAccount = new VirtualAccount();
            projectAccount.setAccountId(4);
            projectAccount.setBalance(BigDecimal.valueOf(50));
            when(accountQueryService.getAccountByProjectId(projectId)).thenReturn(projectAccount);

            // 펀딩 생성 모의
            Funding funding = new Funding();
            funding.setFundingId(20);
            when(fundingService.createFunding(eq(project), eq(username), eq(paymentAmount))).thenReturn(funding);

            // 거래 생성 모의
            Transaction transaction = new Transaction();
            transaction.setTransactionId(6);
            LocalDateTime now = LocalDateTime.now();
            transaction.setTransactionDate(now);
            when(transactionService.createTransaction(eq(funding), eq(payerAccount), eq(projectAccount), eq(paymentAmount), eq(REMITTANCE)))
                    .thenReturn(transaction);

            AccountPaymentResponse response = accountPaymentService.paymentByUsername(request, username);

            // 내부 로직에 따라 payerAccount.transferTo(paymentAmount, projectAccount) 실행 후: 300-80 = 220
            assertEquals(BigDecimal.valueOf(220), payerAccount.getBalance(), "결제 후 결제자 계좌 잔액이 갱신되어야 합니다.");
            // 프로젝트 currentFunding 업데이트 확인
            assertEquals(paymentAmount, project.getCurrentFunding(), "프로젝트 currentFunding이 갱신되어야 합니다.");

            assertEquals(transaction.getTransactionId(), response.transactionId());
            assertEquals(payerAccount.getAccountId(), response.accountId());
            assertEquals(BigDecimal.valueOf(300), response.beforeMoney());
            assertEquals(paymentAmount, response.chargeAmount());
            assertEquals(payerAccount.getBalance(), response.afterMoney());
            assertEquals(now, response.transactionDate());
        }
    }
}
