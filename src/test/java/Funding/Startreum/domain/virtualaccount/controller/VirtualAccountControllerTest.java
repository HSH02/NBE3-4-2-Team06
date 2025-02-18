package Funding.Startreum.domain.virtualaccount.controller;

import Funding.Startreum.common.util.JwtUtil;
import Funding.Startreum.domain.project.repository.ProjectRepository;
import Funding.Startreum.domain.users.CustomUserDetailsService;
import Funding.Startreum.domain.users.UserService;
import Funding.Startreum.domain.virtualaccount.dto.request.AccountPaymentRequest;
import Funding.Startreum.domain.virtualaccount.dto.request.AccountRequest;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountPaymentResponse;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountRefundResponse;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountResponse;
import Funding.Startreum.domain.virtualaccount.exception.AccountNotFoundException;
import Funding.Startreum.domain.virtualaccount.repository.VirtualAccountRepository;
import Funding.Startreum.domain.virtualaccount.service.AccountChargeService;
import Funding.Startreum.domain.virtualaccount.service.AccountPaymentService;
import Funding.Startreum.domain.virtualaccount.service.AccountQueryService;
import Funding.Startreum.domain.virtualaccount.service.AccountRefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static Funding.Startreum.util.TokenUtil.createUserToken;
import static Funding.Startreum.util.utilMethod.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VirtualAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private VirtualAccountRepository virtualAccountRepository;
    @MockitoBean
    private AccountQueryService accountQueryService;
    @MockitoBean
    private CustomUserDetailsService userDetailsService;
    @MockitoBean
    private ProjectRepository projectRepository;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AccountChargeService accountChargeService;
    @MockitoBean
    private AccountPaymentService accountPaymentService;
    @MockitoBean
    private AccountRefundService accountRefundService;

    private static final String BASE_URL = "/api/account";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int ACCOUNT_ID = 100;
    private static final int NON_EXISTENT_ACCOUNT_ID = 500;
    private static final int PROJECT_ID = 1;
    private static final int TRANSACTION_ID = 200;
    private static final String OWNER = "owner";
    private static final String ADMIN = "admin";
    private static final String OTHER = "other";

    private String adminToken;
    private String ownerToken;
    private String notOwnerToken;

    @BeforeEach
    void setUp() {
        createVirtualAccount(virtualAccountRepository, ACCOUNT_ID, OWNER);
        createVirtualProject(projectRepository, PROJECT_ID, OWNER);

        createVirtualDetails(userDetailsService, ADMIN, "ADMIN");
        createVirtualDetails(userDetailsService, OWNER, "SPONSOR");
        createVirtualDetails(userDetailsService, OTHER, "SPONSOR");

        setVirtualUser(userService, 1, ADMIN, Funding.Startreum.domain.users.User.Role.ADMIN);
        setVirtualUser(userService, 2, OWNER, Funding.Startreum.domain.users.User.Role.SPONSOR);
        setVirtualUser(userService, 3, OTHER, Funding.Startreum.domain.users.User.Role.SPONSOR);

        adminToken = createUserToken(jwtUtil, ADMIN, "admin@test.com", "ADMIN");
        ownerToken = createUserToken(jwtUtil, OWNER, "owner@test.com", "SPONSOR");
        notOwnerToken = createUserToken(jwtUtil, OTHER, "other@test.com", "SPONSOR");
    }

    @Nested
    @DisplayName("계좌 조회 API 테스트 (accountId 기반)")
    class AccountInquiryTests {

        @Test
        @DisplayName("[조회 200] OWNER가 자신의 계좌를 조회할 경우")
        void getOwnAccount() throws Exception {
            AccountResponse response = new AccountResponse(ACCOUNT_ID, BigDecimal.valueOf(5000), LocalDateTime.now());
            given(accountQueryService.getAccountInfo(OWNER)).willReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("계좌 내역 조회에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID));
        }

        @Test
        @DisplayName("[조회 404] 존재하지 않는 계좌 조회 시")
        void getNonExistingAccount() throws Exception {
            given(accountQueryService.getAccountInfo(NON_EXISTENT_ACCOUNT_ID))
                    .willThrow(new AccountNotFoundException(NON_EXISTENT_ACCOUNT_ID));

            mockMvc.perform(get(BASE_URL + "/{accountId}", NON_EXISTENT_ACCOUNT_ID)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("계좌 충전 API 테스트 (username 기반)")
    class AccountChargeTests {

        @Test
        @DisplayName("[충전 200] OWNER가 자신의 계좌를 충전할 경우")
        void chargeOwnAccountByUserName() throws Exception {
            BigDecimal amount = BigDecimal.valueOf(1000);
            AccountPaymentResponse response = new AccountPaymentResponse(
                    0, ACCOUNT_ID, BigDecimal.ZERO, amount, amount, LocalDateTime.now()
            );
            given(accountChargeService.chargeByUsername(eq(OWNER), any(AccountRequest.class))).willReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ownerToken)
                            .content("{ \"amount\": 1000 }")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("계좌 충전에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$.data.afterMoney").value(1000));
        }
    }

    @Nested
    @DisplayName("계좌 충전 API 테스트 (accountId 기반)")
    class AccountChargeByAccountIdTests {

        @Test
        @DisplayName("[충전 200] OWNER가 자신의 계좌를 계좌ID 기반으로 충전할 경우")
        void chargeAccountByAccountIdSuccess() throws Exception {
            BigDecimal amount = BigDecimal.valueOf(1500);
            AccountPaymentResponse response = new AccountPaymentResponse(
                    0, ACCOUNT_ID, BigDecimal.valueOf(5000), amount, BigDecimal.valueOf(6500), LocalDateTime.now()
            );
            given(accountChargeService.chargeByAccountId(eq(ACCOUNT_ID), any(AccountRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post(BASE_URL + "/{accountId}", ACCOUNT_ID)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ownerToken)
                            .content("{ \"amount\": 1500 }")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("계좌 충전에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$.data.afterMoney").value(6500));
        }

        @Test
        @DisplayName("[충전 403] NOT OWNER가 계좌ID 기반으로 충전 요청할 경우")
        void chargeAccountByAccountIdNotOwner() throws Exception {
            mockMvc.perform(post(BASE_URL + "/{accountId}", ACCOUNT_ID)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + notOwnerToken)
                            .content("{ \"amount\": 1500 }")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("계좌 결제 API 테스트 (accountId 기반)")
    class AccountPaymentTests {

        @Test
        @DisplayName("[결제 200] OWNER가 자신의 계좌로 결제할 경우")
        void paymentByAccountId() throws Exception {
            BigDecimal amount = BigDecimal.valueOf(1000);
            AccountPaymentResponse response = new AccountPaymentResponse(
                    0, ACCOUNT_ID, BigDecimal.ZERO, amount, amount, LocalDateTime.now()
            );
            given(accountPaymentService.paymentByAccountId(eq(ACCOUNT_ID), any(AccountPaymentRequest.class), eq(OWNER)))
                    .willReturn(response);

            mockMvc.perform(post(BASE_URL + "/{accountId}/payment", ACCOUNT_ID)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ownerToken)
                            .content("{ \"projectId\": 1, \"amount\": 1000 }")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("결제에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$.data.afterMoney").value(1000));
        }

        @Test
        @DisplayName("[결제 403] NOT OWNER가 다른 계좌로 결제할 경우")
        void paymentNotOwner() throws Exception {
            mockMvc.perform(post(BASE_URL + "/{accountId}/payment", ACCOUNT_ID)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + notOwnerToken)
                            .content("{ \"projectId\": 1, \"amount\": 1000 }")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("계좌 결제 API 테스트 (username 기반)")
    class AccountPaymentByUserNameTests {

        @Test
        @DisplayName("[결제 200] 로그인한 사용자가 자신의 이름 기반으로 결제할 경우")
        void paymentByUserName() throws Exception {
            BigDecimal amount = BigDecimal.valueOf(1000);
            AccountPaymentResponse response = new AccountPaymentResponse(
                    0, ACCOUNT_ID, BigDecimal.ZERO, amount, amount, LocalDateTime.now()
            );
            given(accountPaymentService.paymentByUsername(any(AccountPaymentRequest.class), eq(OWNER)))
                    .willReturn(response);

            mockMvc.perform(post(BASE_URL + "/payment")
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ownerToken)
                            .content("{ \"projectId\": 1, \"amount\": 1000 }")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("결제에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$.data.afterMoney").value(1000));
        }
    }

    @Nested
    @DisplayName("계좌 환불 API 테스트 (accountId 기반)")
    class AccountRefundTests {


        @Test
        @DisplayName("[환불 200] OWNER가 자신의 거래를 환불할 경우 (accountId 기반)")
        void refundPayment() throws Exception {
            BigDecimal refundAmount = BigDecimal.valueOf(1000);
            AccountRefundResponse refundResponse = new AccountRefundResponse(
                    TRANSACTION_ID, TRANSACTION_ID, ACCOUNT_ID, BigDecimal.ZERO, refundAmount, refundAmount, LocalDateTime.now()
            );
            given(accountRefundService.refund(eq(ACCOUNT_ID), eq(TRANSACTION_ID))).willReturn(refundResponse);

            mockMvc.perform(post(BASE_URL + "/{accountId}/transactions/{transactionId}/refund", ACCOUNT_ID, TRANSACTION_ID)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("거래 환불에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$.data.refundTransactionId").value(TRANSACTION_ID));
        }

        @Test
        @DisplayName("[환불 403] NOT OWNER가 거래 환불을 요청할 경우 (accountId 기반)")
        void refundNotOwner() throws Exception {
            mockMvc.perform(post(BASE_URL + "/{accountId}/transactions/{transactionId}/refund", ACCOUNT_ID, TRANSACTION_ID)
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + notOwnerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

    }
}