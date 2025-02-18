package Funding.Startreum.domain.virtualaccount.repository;


import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, Integer> {
    Optional<VirtualAccount> findByUser_UserId(Integer userId);  // userId를 사용하여 VirtualAccount 찾기

    @Query("SELECT va FROM VirtualAccount va " +
            "JOIN va.user u " +
            "JOIN u.projects p " +
            "WHERE p.projectId = :projectId")
    Optional<VirtualAccount> findBeneficiaryAccountByProjectId(@Param("projectId") Integer projectId);

    @Query("SELECT va FROM VirtualAccount va " +
            "JOIN va.user u " +
            "WHERE u.name = :username")
    Optional<VirtualAccount> findBeneficiaryAccountByUser_Name(@Param("username") String username);

    @Query("SELECT va FROM VirtualAccount va " +
            "JOIN Transaction t ON t.receiverAccount = va " +
            "WHERE t.transactionId = :transactionId")
    Optional<VirtualAccount> findReceiverAccountByTransactionId(@Param("transactionId") Integer transactionId);

    Optional<VirtualAccount> findByUser_Name(String userName);
}
    
