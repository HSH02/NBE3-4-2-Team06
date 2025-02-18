package Funding.Startreum.domain.virtualaccount.service;

import Funding.Startreum.domain.transaction.entity.Transaction;
import Funding.Startreum.domain.transaction.service.TransactionService;
import Funding.Startreum.domain.virtualaccount.dto.request.AccountRequest;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountPaymentResponse;
import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import Funding.Startreum.domain.virtualaccount.repository.VirtualAccountRepository;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@SpringBootTest
class AccountChargeServiceTest {

    @MockitoBean
    private VirtualAccountRepository virtualAccountRepository;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private AccountQueryService accountQueryService;

    @Autowired
    private AccountChargeService accountChargeService;

    @Nested
    @DisplayName("chargeByAccountId() 테스트")
    class ChargeByAccountIdTests {
        @Test
        void testChargeByAccountId() {
            int accountId = 1;
            BigDecimal initialBalance = BigDecimal.valueOf(100);
            BigDecimal chargeAmount = BigDecimal.valueOf(50);
            AccountRequest request = new AccountRequest(chargeAmount);

            // 테스트용 VirtualAccount 생성
            VirtualAccount account = new VirtualAccount();
            account.setAccountId(accountId);
            account.setBalance(initialBalance);

            when(accountQueryService.getAccount(accountId)).thenReturn(account);

            // 거래 생성 모의 (첫번째 파라미터 null)
            Transaction transaction = new Transaction();
            transaction.setTransactionId(1);
            LocalDateTime now = LocalDateTime.now();
            transaction.setTransactionDate(now);
            when(transactionService.createTransaction(isNull(), eq(account), eq(account), eq(chargeAmount), eq(REMITTANCE)))
                    .thenReturn(transaction);

            AccountPaymentResponse response = accountChargeService.chargeByAccountId(accountId, request);

            // 충전 후 잔액은 초기 잔액 + 충전 금액이어야 함
            assertEquals(initialBalance.add(chargeAmount), account.getBalance());
            assertEquals(transaction.getTransactionId(), response.transactionId());
            assertEquals(accountId, response.accountId());
            assertEquals(initialBalance, response.beforeMoney());
            assertEquals(chargeAmount, response.chargeAmount());
            assertEquals(account.getBalance(), response.afterMoney());
            assertEquals(now, response.transactionDate());
        }
    }

    @Nested
    @DisplayName("chargeByUsername() 테스트")
    class ChargeByUsernameTests {
        @Test
        void testChargeByUsername() {
            String username = "testUser";
            BigDecimal initialBalance = BigDecimal.valueOf(200);
            BigDecimal chargeAmount = BigDecimal.valueOf(30);
            AccountRequest request = new AccountRequest(chargeAmount);

            VirtualAccount account = new VirtualAccount();
            account.setAccountId(2);
            account.setBalance(initialBalance);

            when(accountQueryService.getAccount(username)).thenReturn(account);

            Transaction transaction = new Transaction();
            transaction.setTransactionId(2);
            LocalDateTime now = LocalDateTime.now();
            transaction.setTransactionDate(now);
            when(transactionService.createTransaction(isNull(), eq(account), eq(account), eq(chargeAmount), eq(REMITTANCE)))
                    .thenReturn(transaction);

            AccountPaymentResponse response = accountChargeService.chargeByUsername(username, request);

            assertEquals(initialBalance.add(chargeAmount), account.getBalance());
            assertEquals(transaction.getTransactionId(), response.transactionId());
            assertEquals(account.getAccountId(), response.accountId());
            assertEquals(initialBalance, response.beforeMoney());
            assertEquals(chargeAmount, response.chargeAmount());
            assertEquals(account.getBalance(), response.afterMoney());
            assertEquals(now, response.transactionDate());
        }
    }
}
