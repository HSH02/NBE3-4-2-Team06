package Funding.Startreum.domain.transaction.service;

import Funding.Startreum.domain.funding.entity.Funding;
import Funding.Startreum.domain.transaction.entity.Transaction;
import Funding.Startreum.domain.transaction.repository.TransactionRepository;
import Funding.Startreum.domain.transaction.transaction.TransactionNotFoundException;
import Funding.Startreum.domain.users.UserRepository;
import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public void transfer(String email, Integer projectId) {

    }

    @Transactional
    public Transaction getTransaction(int transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    /**
     * 거래 내역 생성 메서드
     *
     * @param funding         관련 펀딩 내역
     * @param from   자금 출금 계좌 (결제 시에는 결제자, 환불 시에는 프로젝트 계좌)
     * @param to 자금 입금 계좌 (결제 시에는 프로젝트 계좌, 환불 시에는 결제자 계좌)
     * @param amount          거래 금액
     * @param type            거래 유형 (REMITTANCE 또는 REFUND)
     * @return 생성된 Transaction 객체
     */
    @Transactional
    public Transaction createTransaction(
            Funding funding,
            VirtualAccount from,
            VirtualAccount to,
            BigDecimal amount,
            Transaction.TransactionType type
    ) {
        Transaction transaction = new Transaction();
        transaction.setFunding(funding);
        transaction.setAdmin(userRepository.findByName("Admin").orElse(null));
        transaction.setSenderAccount(from);
        transaction.setReceiverAccount(to);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(transaction);

        return transaction;
    }
}
