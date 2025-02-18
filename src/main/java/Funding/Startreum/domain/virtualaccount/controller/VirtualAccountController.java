package Funding.Startreum.domain.virtualaccount.controller;


import Funding.Startreum.common.util.ApiResponse;
import Funding.Startreum.domain.virtualaccount.dto.VirtualAccountDtos;
import Funding.Startreum.domain.virtualaccount.dto.request.AccountPaymentRequest;
import Funding.Startreum.domain.virtualaccount.dto.request.AccountRequest;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountPaymentResponse;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountRefundResponse;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountResponse;
import Funding.Startreum.domain.virtualaccount.service.AccountChargeService;
import Funding.Startreum.domain.virtualaccount.service.AccountPaymentService;
import Funding.Startreum.domain.virtualaccount.service.AccountQueryService;
import Funding.Startreum.domain.virtualaccount.service.AccountRefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class VirtualAccountController {

    private final AccountQueryService accountQueryService;
    private final AccountChargeService accountChargeService;
    private final AccountPaymentService accountPaymentService;
    private final AccountRefundService accountRefundService;

    /**
     * 특정 사용자의 계좌 조회 API (이름 기반)
     */
    @GetMapping("/user/{name}")
    public ResponseEntity<VirtualAccountDtos> getAccount(@PathVariable String name, Principal principal) {
        // System.out.println(principal);
        //  System.out.println("🔍 Principal 정보: " + (principal != null ? principal.getName() : "NULL"));
        // System.out.println("🔍 요청된 사용자: " + name);

        if (principal == null) {
            // System.out.println("❌ 인증되지 않은 사용자 요청");
            return ResponseEntity.status(401).body(new VirtualAccountDtos(false)); // Unauthorized
        }

        if (!principal.getName().equals(name)) {
            //  System.out.println("❌ 본인 또는 관리자가 아님: 접근 불가");
            return ResponseEntity.status(403).body(new VirtualAccountDtos(false)); // Forbidden
        }

        VirtualAccountDtos account = accountQueryService.findByName(name);
        return ResponseEntity.ok().body(account);
    }

    /**
     * 계좌 생성 API
     */
    @PostMapping("/user/{name}/create")
    public ResponseEntity<VirtualAccountDtos> createAccount(@PathVariable String name, Principal principal) {
        if (principal == null || !principal.getName().equals(name)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new VirtualAccountDtos(false));  // ✅ HttpStatus.FORBIDDEN 사용
        }

        try {
            VirtualAccountDtos newAccount = accountQueryService.createAccount(name);
            return ResponseEntity.ok().body(newAccount);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new VirtualAccountDtos(false));
        }
    }


    /**
     * 잔액 충전: 계좌에 금액을 충전합니다.
     *
     * @param accountId 충전할 계좌의 ID. 해당 계좌의 소유자여야 합니다.
     * @param request   충전할 금액 및 관련 정보를 담은 DTO.
     * @return ApiResponse 객체 안에 충전된 계좌 정보를 포함하여 반환합니다.
     */
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.isAccountOwner(principal, #accountId)")
    @PostMapping("/{accountId}")
    public ResponseEntity<?> chargeAccountByAccountId(
            @PathVariable("accountId") @P("accountId") int accountId,
            @RequestBody @Valid AccountRequest request
    ) {
        AccountPaymentResponse response = accountChargeService.chargeByAccountId(accountId, request);
        return ResponseEntity.ok(ApiResponse.success("계좌 충전에 성공했습니다.", response));
    }

    /**
     * 잔액 충전: 현재 로그인한 사용자의 계좌에 금액을 충전합니다.
     *
     * @param request 충전할 금액 및 관련 정보를 담은 DTO.
     * @return ApiResponse 객체 안에 충전된 계좌 정보를 포함하여 반환합니다.
     */
    @PostMapping
    public ResponseEntity<?> chargeOwnAccountByUserName(
            @RequestBody @Valid AccountRequest request,
            Principal principal
    ) {
        AccountPaymentResponse response = accountChargeService.chargeByUsername(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("계좌 충전에 성공했습니다.", response));
    }

    /**
     * 계좌 내역 조회: 특정 계좌의 거래 내역을 조회합니다.
     *
     * @param accountId 조회할 계좌의 ID.
     * @return ApiResponse 객체 안에 조회된 계좌 거래 내역 정보를 포함하여 반환합니다.
     */
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.isAccountOwner(principal, #accountId)")
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountByAccountId(
            @PathVariable("accountId") @P("accountId") int accountId
    ) {
        AccountResponse response = accountQueryService.getAccountInfo(accountId);
        return ResponseEntity.ok(ApiResponse.success("계좌 내역 조회에 성공했습니다.", response));
    }

    /**
     * 계좌 내역 조회: 현재 로그인한 사용자의 계좌 잔액을 조회합니다.
     * *
     *
     * @return ApiResponse 객체 안에 조회된 계좌 거래 내역 정보를 포함하여 반환합니다.
     */
    @GetMapping
    public ResponseEntity<?> getAccountByUserName(
            Principal principal
    ) {
        AccountResponse response = accountQueryService.getAccountInfo(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("계좌 내역 조회에 성공했습니다.", response));
    }

    /**
     * 결제 처리: 특정 계좌의 결제 요청을 처리합니다.
     *
     * @param accountId 결제를 진행할 계좌의 ID.
     * @param request   결제 요청 정보를 담은 DTO (예: 프로젝트 ID, 결제 금액 등).
     * @param principal 현재 인증된 사용자의 세부 정보를 포함하는 객체.
     * @return ApiResponse 객체 안에 결제가 완료된 계좌 정보를 포함하여 반환합니다.
     */
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.isAccountOwner(principal, #accountId)")
    @PostMapping("/{accountId}/payment")
    public ResponseEntity<?> processPaymentByAccountId(
            @PathVariable("accountId") @P("accountId") int accountId,
            @RequestBody @Valid AccountPaymentRequest request,
            Principal principal
    ) {
        AccountPaymentResponse response = accountPaymentService.paymentByAccountId(accountId, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("결제에 성공했습니다.", response));
    }

    /**
     * 결제 처리: 현재 로그인한 사용자의 계좌 결제 요청을 처리합니다
     *
     * @param request     결제 요청 정보를 담은 DTO (예: 프로젝트 ID, 결제 금액 등).
     * @param userDetails 현재 인증된 사용자의 세부 정보를 포함하는 객체.
     * @return ApiResponse 객체 안에 결제가 완료된 계좌 정보를 포함하여 반환합니다.
     */
    @PostMapping("/payment")
    public ResponseEntity<?> processPaymentByUserName(
            @RequestBody @Valid AccountPaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountPaymentResponse response = accountPaymentService.paymentByUsername(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("결제에 성공했습니다.", response));
    }

    /**
     * 특정 계좌 환불 처리
     *
     * @param accountId     환불을 요청하는 계좌의 ID (원래 결제에 사용된 계좌).
     * @param transactionId 환불할 거래의 ID.
     * @return ApiResponse 객체 안에 환불이 완료된 계좌 정보를 포함하여 반환합니다.
     */
    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.isAccountOwner(principal, #accountId)")
    @PostMapping("/{accountId}/transactions/{transactionId}/refund")
    public ResponseEntity<?> processRefund(
            @PathVariable("accountId") @P("accountId") int accountId,
            @PathVariable int transactionId
    ) {
        AccountRefundResponse response = accountRefundService.refund(accountId, transactionId);
        return ResponseEntity.ok(ApiResponse.success("거래 환불에 성공했습니다.", response));
    }

}