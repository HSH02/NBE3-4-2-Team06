package Funding.Startreum.domain.virtualaccount.service;

import Funding.Startreum.domain.transaction.entity.Transaction;
import Funding.Startreum.domain.transaction.service.TransactionService;
import Funding.Startreum.domain.virtualaccount.dto.request.AccountRequest;
import Funding.Startreum.domain.virtualaccount.dto.response.AccountPaymentResponse;
import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import Funding.Startreum.domain.virtualaccount.repository.VirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static Funding.Startreum.domain.transaction.entity.Transaction.TransactionType.REMITTANCE;
import static Funding.Startreum.domain.virtualaccount.dto.response.AccountPaymentResponse.mapToAccountPaymentResponse;

@Service
@RequiredArgsConstructor
public class AccountChargeService {
    private final VirtualAccountRepository virtualAccountRepository;
    private final TransactionService transactionService;
    private final AccountQueryService accountQueryService;


    /**
     * 계좌를 충전합니다. (계좌 ID 기반)
     *
     * @param accountId 조회할 계좌 ID
     * @param request   잔액 정보가 담겨진 DTO
     * @return 충전 후 갱신된 계좌 정보 DTO
     */
    @Transactional
    public AccountPaymentResponse chargeByAccountId(int accountId, AccountRequest request) {
        VirtualAccount account = accountQueryService.getAccount(accountId);
        return chargeAccount(account, request);
    }

    /**
     * 계좌를 충전합니다. (username 기반)
     *
     * @param request 잔액 정보가 담겨진 DTO
     * @return 충전 후 갱신된 계좌 정보 DTO
     */
    @Transactional
    public AccountPaymentResponse chargeByUsername(String username, AccountRequest request) {
        VirtualAccount account = accountQueryService.getAccount(username);
        return chargeAccount(account, request);
    }

    /**
     * 계좌를 충전합니다.
     *
     * @param account 충전할 VirtualAccount 엔티티
     * @param request 잔액 정보가 담긴 DTO
     * @return 충전 후 갱신된 계좌 정보 DTO
     */
    private AccountPaymentResponse chargeAccount(VirtualAccount account, AccountRequest request) {
        // 1. 잔액 업데이트
        BigDecimal beforeMoney = account.getBalance();
        account.setBalance(account.getBalance().add(request.amount()));

        // 2. 거래 내역 생성 (여기서 첫번째 파라미터는 외부 전달용 ID로, null로 처리)
        Transaction transaction = transactionService.createTransaction(null, account, account, request.amount(), REMITTANCE);

        // 3. 응답 객체 생성 및 반환
        return mapToAccountPaymentResponse(account, transaction, beforeMoney, request.amount());
    }
}
