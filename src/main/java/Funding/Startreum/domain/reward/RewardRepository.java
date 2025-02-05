package Funding.Startreum.domain.reward;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Integer> {
    List<Reward> findByProject_ProjectId(Integer projectId);

    // 조건에 맞는 가장 큰 첫번째 리워드 반환
    Optional<Reward> findTopByProject_ProjectIdAndAmountLessThanEqualOrderByAmountDesc(
            int projectId,
            BigDecimal paymentAmount
    );

}
