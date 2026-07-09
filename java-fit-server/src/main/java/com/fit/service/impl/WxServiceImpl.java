package com.fit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.service.WxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 微信服务实现 —— 支持 Mock 模式和真实 code2Session 调用
 */
@Slf4j
@Service
public class WxServiceImpl implements WxService {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session"
            + "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wx.mock:false}")
    private boolean mock;

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

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
