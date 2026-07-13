package com.fit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.service.WxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 微信服务实现 —— 支持 Mock 模式和真实 code2Session / getAccessToken 调用
 */
@Slf4j
@Service
public class WxServiceImpl implements WxService {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** access_token 内存缓存 */
    private volatile String cachedAccessToken;
    private volatile long tokenExpiresAt;

    @Value("${wx.mock:false}")
    private boolean mock;

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    /**
     * 获取微信 access_token（带内存缓存，提前 5 分钟刷新）
     * 供 SecurityCheckService 调用 msgSecCheck / imgSecCheck 使用
     */
    public String getAccessToken() {
        // 缓存有效则直接返回
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedAccessToken;
        }
        synchronized (this) {
            // 双重检查
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
                return cachedAccessToken;
            }
            try {
                String url = String.format(TOKEN_URL, appId, appSecret);
                log.debug("请求微信 access_token");
                String resp = restTemplate.getForObject(url, String.class);
                JsonNode node = objectMapper.readTree(resp);

                if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                    String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";
                    log.error("获取 access_token 失败: errcode={}, errmsg={}",
                            node.get("errcode"), errmsg);
                    throw new RuntimeException("获取微信 access_token 失败: " + errmsg);
                }

                cachedAccessToken = node.get("access_token").asText();
                int expiresIn = node.get("expires_in").asInt();
                // 提前 5 分钟刷新
                tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                log.info("access_token 已刷新，过期时间: {}s", expiresIn);
                return cachedAccessToken;

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.error("获取 access_token 异常", e);
                throw new RuntimeException("获取微信 access_token 异常", e);
            }
        }
    }

    @Override
    public String getOpenIdByCode(String code) {
        if (mock) {
            log.warn("微信Mock模式：跳过真实code换取，返回占位openid");
            return "mock_openid_" + Math.abs(code.hashCode());
        }

        try {
            String url = String.format(CODE2SESSION_URL, appId, appSecret, code);
            log.debug("请求微信 code2Session, code={}", code);

            String resp = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(resp);

            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";
                log.error("微信 code2Session 失败: errcode={}, errmsg={}", node.get("errcode"), errmsg);
                throw new RuntimeException("微信登录失败: " + errmsg);
            }

            String openid = node.get("openid").asText();
            log.info("微信 code2Session 成功, openid 已获取");
            return openid;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信 code2Session 调用异常", e);
            throw new RuntimeException("微信登录服务异常", e);
        }
    }
}
