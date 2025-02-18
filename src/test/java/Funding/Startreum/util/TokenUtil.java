package Funding.Startreum.util;

import Funding.Startreum.common.util.JwtUtil;

public class TokenUtil {

    /**
     * 가상 유저 AccessToken을 생성합니다.
     *
     * @param jwtUtil  jwtUtil 생성
     * @param username 유저 이름
     * @param email    이메일
     * @param role     역할
     * @return AccessToken
     */
    public static String createUserToken(JwtUtil jwtUtil, String username, String email, String role) {
        return jwtUtil.generateAccessToken(username, email, role);
    }
}
