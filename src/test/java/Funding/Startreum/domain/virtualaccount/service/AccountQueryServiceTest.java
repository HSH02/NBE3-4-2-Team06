package Funding.Startreum.domain.virtualaccount.service;

import Funding.Startreum.domain.funding.service.FundingService;
import Funding.Startreum.domain.project.repository.ProjectRepository;
import Funding.Startreum.domain.project.service.ProjectService;
import Funding.Startreum.domain.transaction.repository.TransactionRepository;
import Funding.Startreum.domain.transaction.service.TransactionService;
import Funding.Startreum.domain.users.User;
import Funding.Startreum.domain.users.UserRepository;
import Funding.Startreum.domain.virtualaccount.dto.VirtualAccountDtos;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountResponse;
import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import Funding.Startreum.domain.virtualaccount.exception.AccountNotFoundException;
import Funding.Startreum.domain.virtualaccount.repository.VirtualAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountQueryServiceTest {

    @MockitoBean
    private VirtualAccountRepository virtualAccountRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private FundingService fundingService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private ProjectService projectService;

    @Autowired
    private AccountQueryService accountQueryService;

    @Nested
    @DisplayName("findByName() 관련 테스트")
    class FindByNameTests {

        @Test
        @DisplayName("유저를 찾을 수 없는 경우")
        void whenUserNotFound_thenReturnDtosWithFalse() {
            String name = "nonexistent";
            when(userRepository.findByName(name)).thenReturn(Optional.empty());

            VirtualAccountDtos dtos = accountQueryService.findByName(name);

            assertFalse(dtos.isAccountExists(), "User가 없으면 isAccountExists는 false여야 합니다.");
        }

        @Test
        @DisplayName("유저는 존재하지만 계좌를 찾을 수 없는 경우")
        void whenUserFoundButAccountNotFound_thenReturnDtosWithFalse() {
            String name = "user";
            User user = new User();
            user.setUserId(1);
            when(userRepository.findByName(name)).thenReturn(Optional.of(user));
            when(virtualAccountRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());

            VirtualAccountDtos dtos = accountQueryService.findByName(name);

            assertFalse(dtos.isAccountExists(), "계좌가 없으면 isAccountExists는 false여야 합니다.");
        }

        @Test
        @DisplayName("계좌가 올바르게 존재할 경우")
        void whenUserAndAccountFound_thenReturnDtosFromEntity() {
            String name = "user";
            User user = new User();
            user.setUserId(1);
            when(userRepository.findByName(name)).thenReturn(Optional.of(user));

            VirtualAccount account = new VirtualAccount();
            account.setUser(user);
            account.setBalance(BigDecimal.valueOf(100));
            account.setCreatedAt(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());
            when(virtualAccountRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(account));

            VirtualAccountDtos dtos = accountQueryService.findByName(name);

            assertTrue(dtos.isAccountExists(), "계좌가 존재하면 isAccountExists는 true여야 합니다.");
            assertEquals(BigDecimal.valueOf(100), dtos.getBalance(), "계좌 잔액이 매핑되어야 합니다.");
        }
    }

    @Nested
    @DisplayName("createAccount() 관련 테스트")
    class CreateAccountTests {

        @Test
        @DisplayName("유저를 찾을 수 없는 경우")
        void whenUserNotFound_thenThrowException() {
            String name = "nonexistent";
            when(userRepository.findByName(name)).thenReturn(Optional.empty());

            Exception exception = assertThrows(IllegalArgumentException.class, () ->
                    accountQueryService.createAccount(name)
            );
            assertTrue(exception.getMessage().contains("사용자를 찾을 수 없습니다"), "적절한 에러 메시지가 포함되어야 합니다.");
            verify(userRepository, times(1)).findByName(name);
        }

        @Test
        @DisplayName("계좌가 이미 존재할 경우")
        void whenAccountAlreadyExists_thenThrowException() {
            String name = "user";
            User user = new User();
            user.setUserId(1);
            when(userRepository.findByName(name)).thenReturn(Optional.of(user));

            VirtualAccount existingAccount = new VirtualAccount();
            existingAccount.setUser(user);
            when(virtualAccountRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(existingAccount));

            Exception exception = assertThrows(IllegalStateException.class, () ->
                    accountQueryService.createAccount(name)
            );
            assertEquals("이미 계좌가 존재합니다.", exception.getMessage());
        }

        @Test
        @DisplayName("계좌를 성공적으로 생성했을 경우")
        void whenValid_thenCreateAccount() {
            String name = "user";
            User user = new User();
            user.setUserId(1);
            when(userRepository.findByName(name)).thenReturn(Optional.of(user));
            when(virtualAccountRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
            when(virtualAccountRepository.save(any(VirtualAccount.class)))
                    .thenAnswer(invocation -> {
                        VirtualAccount account = invocation.getArgument(0);
                        account.setUser(user);
                        return account;
                    });

            VirtualAccountDtos dtos = accountQueryService.createAccount(name);

            assertTrue(dtos.isAccountExists(), "계좌 생성 시 success flag는 true여야 합니다.");
            assertEquals(BigDecimal.ZERO, dtos.getBalance(), "새 계좌의 초기 잔액은 0이어야 합니다.");
            verify(virtualAccountRepository, times(1)).save(any(VirtualAccount.class));
        }
    }

    @Nested
    @DisplayName("getAccountInfo() 관련 테스트")
    class GetAccountInfoTests {

        @Test
        @DisplayName("계좌 ID 기반 계좌 정보 조회")
        void testGetAccountInfoByAccountId() {
            int accountId = 1;
            VirtualAccount account = new VirtualAccount();
            account.setAccountId(accountId);
            account.setBalance(BigDecimal.valueOf(500));
            when(virtualAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

            AccountResponse response = accountQueryService.getAccountInfo(accountId);

            assertNotNull(response, "계좌 정보 응답은 null이 아니어야 합니다.");
            assertEquals(accountId, response.accountId(), "응답 계좌 ID가 일치해야 합니다.");
        }

        @Test
        @DisplayName("username 기반 계좌 정보 조회")
        void testGetAccountInfoByUsername() {
            String username = "userInfo";
            VirtualAccount account = new VirtualAccount();
            account.setAccountId(2);
            account.setBalance(BigDecimal.valueOf(750));
            User user = new User();
            user.setName(username);
            account.setUser(user);
            when(virtualAccountRepository.findByUser_Name(username)).thenReturn(Optional.of(account));

            AccountResponse response = accountQueryService.getAccountInfo(username);

            assertNotNull(response, "계좌 정보 응답은 null이 아니어야 합니다.");
            assertEquals(2, response.accountId(), "응답 계좌 ID가 일치해야 합니다.");
        }
    }

    @Nested
    @DisplayName("getAccount() 예외 처리 테스트")
    class GetAccountExceptionTests {

        @Test
        @DisplayName("계좌 ID로 계좌를 찾을 수 없는 경우")
        void whenAccountNotFoundById_thenThrowAccountNotFoundException() {
            int accountId = 999;
            when(virtualAccountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () -> accountQueryService.getAccount(accountId));
        }

        @Test
        @DisplayName("username으로 계좌를 찾을 수 없는 경우")
        void whenAccountNotFoundByUsername_thenThrowAccountNotFoundException() {
            String username = "nonexistentUser";
            when(virtualAccountRepository.findByUser_Name(username)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () -> accountQueryService.getAccount(username));
        }
    }
}
