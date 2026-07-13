package com.fit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.service.WxSubscribeMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class WxSubscribeMessageServiceImpl implements WxSubscribeMessageService {

    private static final String GET_ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

    private static final String SEND_SUBSCRIBE_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantLock tokenLock = new ReentrantLock();

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    /** 缓存的 access_token */
    private String cachedAccessToken;
    /** access_token 过期时间戳（毫秒） */
    private long tokenExpireTime = 0L;

    @Override
    public Map<String, Object> sendSubscribeMessage(String openId, String templateId,
                                                     String thing2, String thing3,
                                                     String time1, String page) {
        String accessToken = getAccessToken();

        // 构建 data 字段
        Map<String, Object> thing2Data = new LinkedHashMap<>();
        thing2Data.put("value", thing2);
        Map<String, Object> thing3Data = new LinkedHashMap<>();
        thing3Data.put("value", thing3);
        Map<String, Object> time1Data = new LinkedHashMap<>();
        time1Data.put("value", time1);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("thing2", thing2Data);
        data.put("thing3", thing3Data);
        data.put("time1", time1Data);

        // 构建请求体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openId);
        body.put("template_id", templateId);
        body.put("page", page != null ? page : "pages/index/index");
        body.put("data", data);

        try {
            String url = String.format(SEND_SUBSCRIBE_URL, accessToken);
            log.debug("发送订阅消息: openId={}, templateId={}, body={}", openId, templateId, body);

            String resp = restTemplate.postForObject(url, body, String.class);
            JsonNode node = objectMapper.readTree(resp);

            int errcode = node.has("errcode") ? node.get("errcode").asInt() : -1;
            String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";

            Map<String, Object> result = new HashMap<>();
            result.put("errcode", errcode);
            result.put("errmsg", errmsg);

            if (errcode == 0) {
                log.info("订阅消息发送成功: openId={}, templateId={}", openId, templateId);
            } else {
                log.warn("订阅消息发送失败: openId={}, errcode={}, errmsg={}", openId, errcode, errmsg);
            }

            return result;
        } catch (Exception e) {
            log.error("订阅消息发送异常: openId={}, error={}", openId, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("errcode", -1);
            result.put("errmsg", e.getMessage());
            return result;
        }
    }

    @Override
    public String getAccessToken() {
        // 缓存未过期直接返回
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }

        tokenLock.lock();
        try {
            // 双重检查
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                return cachedAccessToken;
            }

            String url = String.format(GET_ACCESS_TOKEN_URL, appId, appSecret);
            log.info("请求微信 access_token...");

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
            // 提前 5 分钟过期，确保安全
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

            log.info("获取 access_token 成功, 有效期 {} 秒", expiresIn);
            return cachedAccessToken;
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
            throw new RuntimeException("获取微信 access_token 异常: " + e.getMessage());
        } finally {
            tokenLock.unlock();
        }
    }
}
