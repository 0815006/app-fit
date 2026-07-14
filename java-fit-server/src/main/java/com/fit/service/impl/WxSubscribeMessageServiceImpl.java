package com.fit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.service.WxService;
import com.fit.service.WxSubscribeMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxSubscribeMessageServiceImpl implements WxSubscribeMessageService {

    private static final String SEND_SUBSCRIBE_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WxService wxService;

    @Override
    public Map<String, Object> sendSubscribeMessage(String openId, String templateId,
                                                     String thing2, String thing3,
                                                     String time1, String page) {
        String accessToken = wxService.getAccessToken();

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

            // 微信 API 要求 Content-Type: application/json
            // RestTemplate 传 Map 默认用 x-www-form-urlencoded，需要显式设置
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            String resp = restTemplate.postForObject(url, request, String.class);
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
}
