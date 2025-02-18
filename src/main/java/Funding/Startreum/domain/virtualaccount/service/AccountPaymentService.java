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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static Funding.Startreum.domain.transaction.entity.Transaction.TransactionType.REMITTANCE;
import static Funding.Startreum.domain.virtualaccount.dto.response.AccountPaymentResponse.mapToAccountPaymentResponse;

@Service
@RequiredArgsConstructor
public class AccountPaymentService {
    private final TransactionService transactionService;
    private final ProjectService projectService;
    private final FundingService fundingService;
    private final AccountQueryService accountQueryService;

    /**
     * 계좌 ID 기반으로 결제합니다.
     *
     * @param accountId 계좌 ID
     * @param request   프로젝트 ID, 결제 금액
     * @param username  유저 이름
     * @return 결제자 기반 DTO
     */
    @Transactional
    public AccountPaymentResponse paymentByAccountId(
            int accountId,
            AccountPaymentRequest request,
            String username
    ) {
        Project project = projectService.getProject(request.projectId());

        VirtualAccount payerAccount = accountQueryService.getAccount(accountId);
        VirtualAccount projectAccount = accountQueryService.getAccountByProjectId(request.projectId());

        return processPayment(project, payerAccount, projectAccount, request, username);
    }

    /**
     * username 기반을 결제합니다.
     *
     * @param request  프로젝트 ID, 결제 금액
     * @param username 유저 이름
     * @return 결제자 기반 DTO
     */
    @Transactional
    public AccountPaymentResponse paymentByUsername(
            AccountPaymentRequest request,
            String username
    ) {
        Project project = projectService.getProject(request.projectId());

        VirtualAccount payerAccount = accountQueryService.getAccount(username);
        VirtualAccount projectAccount = accountQueryService.getAccountByProjectId(request.projectId());

        return processPayment(project, payerAccount, projectAccount, request, username);
    }

    /**
     * 공통 결제 처리 로직입니다.
     *
     * @param project        결제 대상 프로젝트
     * @param from   결제자 계좌
     * @param to 수혜자 계좌
     * @param request        결제 정보 DTO
     * @param username       결제자 사용자 이름
     * @return 결제 후 갱신된 계좌 정보 DTO
     */
    private AccountPaymentResponse processPayment(
            Project project,
            VirtualAccount from,
            VirtualAccount to,
            AccountPaymentRequest request,
            String username
    ) {
        BigDecimal payerBalanceBefore = from.getBalance();
        BigDecimal paymentAmount = request.amount();

        from.transferTo(paymentAmount, to);

        project.setCurrentFunding(project.getCurrentFunding().add(paymentAmount));

        Funding funding = fundingService.createFunding(project, username, paymentAmount);
        Transaction transaction = transactionService.createTransaction(funding, from, to, paymentAmount, REMITTANCE);

        return mapToAccountPaymentResponse(from, transaction, payerBalanceBefore, paymentAmount);
    }

}
