package Funding.Startreum.domain.virtualaccount.controller;

import Funding.Startreum.domain.virtualaccount.service.AccountQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class VirtualAccountViewController {


    private final AccountQueryService accountQueryService;

    /**
     * 계좌 관리 페이지 반환 (뷰만 제공)
     */
    @GetMapping("/profile/account/{name}")
    public String showAccountPage(@PathVariable String name, Model model, Principal principal) {
        System.out.println("🔍 Principal Name: " + (principal != null ? principal.getName() : "NULL"));
        System.out.println("🔍 PathVariable name: " + name);

        // ✅ name 값이 정상적으로 전달되는지 로그 출력
        if (name == null || name.isEmpty()) {
            System.out.println("❌ name 값이 전달되지 않았음!");
        } else {
            System.out.println("✅ 전달된 name 값: " + name);
        }

        model.addAttribute("name", name);  // ✅ Thymeleaf에서 사용 가능하도록 추가

        return "virtualaccount/account"; // account.html 반환
    }
}