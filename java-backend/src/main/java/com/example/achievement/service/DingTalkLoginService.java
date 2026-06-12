package com.example.achievement.service;

import com.example.achievement.config.DingTalkProperties;
import com.example.achievement.entity.UserConfig;
import com.example.achievement.repository.UserConfigRepository;
import com.example.achievement.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkLoginService {

    private final DingTalkProperties props;
    private final UserConfigRepository userConfigRepository;
    private final JwtTokenUtil jwtTokenUtil;

    private static final String USER_ACCESS_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final String USER_INFO_URL = "https://api.dingtalk.com/v1.0/contact/users/me";

    /**
     * Exchange DingTalk auth code for user info, create/update local user, and return JWT + user info.
     *
     * @param authCode the temporary auth code from DingTalk JSAPI dd.getAuthCode()
     * @return map containing "token" (JWT string) and "user" (user info map)
     */
    public Map<String, Object> login(String authCode) {
        if (!props.isConfigured()) {
            throw new IllegalStateException("钉钉未配置");
        }
        if (authCode == null || authCode.trim().isEmpty()) {
            throw new IllegalArgumentException("authCode不能为空");
        }

        // Step 1: Exchange auth code for user access token
        Map<String, Object> tokenResponse = exchangeAuthCode(authCode);
        String accessToken = (String) tokenResponse.get("accessToken");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new RuntimeException("获取钉钉用户accessToken失败: " + tokenResponse);
        }
        log.debug("Got DingTalk user accessToken");

        // Step 2: Get user info with the access token
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String openId = (String) userInfo.get("openId");
        String unionId = (String) userInfo.get("unionId");
        String nick = (String) userInfo.get("nick");
        String mobile = (String) userInfo.get("mobile");
        String email = (String) userInfo.get("email");
        String avatarUrl = (String) userInfo.get("avatarUrl");

        log.info("DingTalk user login: openId={}, unionId={}, nick={}", openId, unionId, nick);

        // Use unionId as the primary identifier, fallback to openId
        String dingtalkUserId = (unionId != null && !unionId.isEmpty()) ? unionId : openId;
        if (dingtalkUserId == null || dingtalkUserId.isEmpty()) {
            throw new RuntimeException("无法获取钉钉用户ID");
        }

        // Step 3: Find or create local user
        UserConfig user = findOrCreateUser(dingtalkUserId, nick, avatarUrl, mobile, email);

        // Step 4: Generate JWT
        String jwt = jwtTokenUtil.generateToken(dingtalkUserId);

        // Step 5: Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwt);

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("username", user.getUsername());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("role", user.getRole());
        userMap.put("avatar", user.getAvatar());
        userMap.put("userId", user.getUserId());
        result.put("user", userMap);

        return result;
    }

    /**
     * Exchange auth code for user access token via DingTalk API.
     */
    private Map<String, Object> exchangeAuthCode(String authCode) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("clientId", props.getAppKey());
        body.put("clientSecret", props.getAppSecret());
        body.put("code", authCode);
        body.put("grantType", "authorization_code");

        ResponseEntity<Map> resp = rt.postForEntity(USER_ACCESS_TOKEN_URL,
                new HttpEntity<>(body, headers), Map.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            log.error("交换钉钉authCode失败: status={}", resp.getStatusCode());
            throw new RuntimeException("交换钉钉authCode失败: " + resp.getStatusCode());
        }

        return resp.getBody();
    }

    /**
     * Get DingTalk user info using the user access token.
     */
    private Map<String, Object> getUserInfo(String accessToken) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", accessToken);

        ResponseEntity<Map> resp = rt.exchange(USER_INFO_URL, HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            log.error("获取钉钉用户信息失败: status={}", resp.getStatusCode());
            throw new RuntimeException("获取钉钉用户信息失败: " + resp.getStatusCode());
        }

        return resp.getBody();
    }

    /**
     * Find existing user by userId (unionId) or create a new one.
     */
    private UserConfig findOrCreateUser(String dingtalkUserId, String nick, String avatarUrl,
                                        String mobile, String email) {
        // Search by userId field
        Optional<UserConfig> existingByUserId = userConfigRepository.findByUserId(dingtalkUserId);
        if (existingByUserId.isPresent()) {
            UserConfig user = existingByUserId.get();
            // Update profile info
            boolean updated = false;
            if (nick != null && !nick.equals(user.getDisplayName())) {
                user.setDisplayName(nick);
                updated = true;
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatar())) {
                user.setAvatar(avatarUrl);
                updated = true;
            }
            if (updated) {
                userConfigRepository.save(user);
                log.info("Updated DingTalk user profile: userId={}", dingtalkUserId);
            }
            return user;
        }

        // Also try to find by username (userId) - backward compatibility
        Optional<UserConfig> existingByUsername = userConfigRepository.findByUsername(dingtalkUserId);
        if (existingByUsername.isPresent()) {
            UserConfig user = existingByUsername.get();
            if (user.getUserId() == null) {
                user.setUserId(dingtalkUserId);
            }
            if (nick != null) {
                user.setDisplayName(nick);
            }
            if (avatarUrl != null) {
                user.setAvatar(avatarUrl);
            }
            userConfigRepository.save(user);
            return user;
        }

        // Create new user
        UserConfig newUser = new UserConfig();
        newUser.setId(UUID.randomUUID().toString().replace("-", ""));
        newUser.setUsername(dingtalkUserId);
        newUser.setUserId(dingtalkUserId);
        newUser.setDisplayName(nick != null ? nick : dingtalkUserId);
        newUser.setAvatar(avatarUrl);
        newUser.setRole("USER");
        newUser.setEnabled(1);
        userConfigRepository.save(newUser);
        log.info("Created new DingTalk user: userId={}, username={}", dingtalkUserId, dingtalkUserId);

        return newUser;
    }
}
