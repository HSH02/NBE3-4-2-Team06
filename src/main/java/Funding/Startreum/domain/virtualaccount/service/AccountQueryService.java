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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static Funding.Startreum.domain.virtualaccount.dto.response.AccountResponse.mapToAccountResponse;

@Service
@RequiredArgsConstructor
public class AccountQueryService {

    private final VirtualAccountRepository virtualAccountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    private final FundingService fundingService;
    private final TransactionService transactionService;
    private final ProjectService projectService;


    /**
     * 사용자의 계좌 정보를 가져와 DTO로 반환
     */
    public VirtualAccountDtos findByName(String name) {
        User user = userRepository.findByName(name).orElse(null);
        if (user == null) {
            return new VirtualAccountDtos(false); // 계좌 없음 응답
        }

        VirtualAccount account = virtualAccountRepository.findByUser_UserId(user.getUserId()).orElse(null);
        return (account != null) ? VirtualAccountDtos.fromEntity(account) : new VirtualAccountDtos(false);
    }

    /**
     * 계좌 생성
     */
    public VirtualAccountDtos createAccount(String name) {
        User user = userRepository.findByName(name).orElseThrow(() ->
                new IllegalArgumentException("사용자를 찾을 수 없습니다: " + name));

        // 이미 계좌가 있는지 확인
        if (virtualAccountRepository.findByUser_UserId(user.getUserId()).isPresent()) {
            throw new IllegalStateException("이미 계좌가 존재합니다.");
        }

        VirtualAccount newAccount = new VirtualAccount();
        newAccount.setUser(user);
        newAccount.setBalance(BigDecimal.ZERO); // 초기 잔액 0원
        newAccount.setFundingBlock(false); // 기본적으로 펀딩 차단 없음
        newAccount.setCreatedAt(LocalDateTime.now());
        newAccount.setUpdatedAt(LocalDateTime.now());

        virtualAccountRepository.save(newAccount);
        return VirtualAccountDtos.fromEntity(newAccount);
    }

    /**
     * 계좌를 조회합니다. (계좌 ID 기반)
     *
     * @param accountId 조회할 계좌 ID
     * @return 조회한 계좌를 반환합니다.
     */
    public VirtualAccount getAccount(int accountId) {
        return virtualAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    /**
     * 계좌를 조회합니다. (username 기반)
     *
     * @param username 조회할 유저 ID
     * @return 조회한 계좌를 반환합니다.
     */
    public VirtualAccount getAccount(String username) {
        return virtualAccountRepository.findByUser_Name(username)
                .orElseThrow(() -> new AccountNotFoundException(username));
    }

    /**
     * 프로젝트의 소유자 계좌를 조회합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 프로젝트 소유자 게좌
     */
    @Transactional(readOnly = true)
    public VirtualAccount getAccountByProjectId(int projectId) {
        return virtualAccountRepository.findBeneficiaryAccountByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("해당 프로젝트의 수혜자를 찾을 수 없습니다."));
    }

    /**
     * 거래내역의 수신자 계좌를 조회합니다.
     *
     * @param transactionId 거래내역 ID
     * @return 프로젝트 소유자 게좌
     */
    @Transactional(readOnly = true)
    public VirtualAccount getReceiverAccountByTransactionId(int transactionId) {
        return virtualAccountRepository.findReceiverAccountByTransactionId(transactionId)
                .orElseThrow(() -> new AccountNotFoundException(transactionId));
    }


    /**
     * 계좌를 조회합니다. (계좌 ID 기반)
     *
     * @param accountId 조회할 계좌 ID
     * @return 조회한 계좌의 정보 DTO를 반환합니다.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountInfo(int accountId) {
        return mapToAccountResponse(getAccount(accountId));
    }

    /**
     * 계좌를 조회합니다. (username 기반)
     *
     * @param username 현재 로그인한 유저의 이름
     * @return 조회한 계좌의 정보 DTO를 반환합니다.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountInfo(String username) {
        return mapToAccountResponse(getAccount(username));
    }

}