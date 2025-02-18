package Funding.Startreum.domain.users;

import Funding.Startreum.domain.admin.Admin;
import Funding.Startreum.domain.comment.entity.Comment;
import Funding.Startreum.domain.funding.entity.Funding;
import Funding.Startreum.domain.inquiry.Inquiry;
import Funding.Startreum.domain.project.entity.Project;
import Funding.Startreum.domain.virtualaccount.entity.VirtualAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId; // 사용자 고유 ID

    private String name; // 이름

    @Column(unique = true, nullable = false)
    private String email; // 이메일

    private String password; // 비밀번호

    @Enumerated(EnumType.STRING)
    private Role role; // 역할 (BENEFICIARY, SPONSOR, ADMIN)

    private LocalDateTime createdAt; // 가입 일자

    private LocalDateTime updatedAt; // 수정 일자

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private VirtualAccount virtualAccount; // 가상 계좌와 1:1 관계

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects; // 생성한 프로젝트 목록 (BENEFICIARY 전용)

    @OneToMany(mappedBy = "sponsor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Funding> fundings; // 후원 내역 (SPONSOR 전용)

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments; // 작성한 댓글

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Admin> adminActions; // 관리자가 수행한 행동

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inquiry> inquiries; // 작성한 문의 목록

    public enum Role {
        BENEFICIARY, // 수혜자
        SPONSOR,     // 후원자
        ADMIN        // 관리자
    }
}
