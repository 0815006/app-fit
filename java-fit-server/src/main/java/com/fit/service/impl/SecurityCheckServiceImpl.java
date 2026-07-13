package com.fit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.service.SecurityCheckService;
import com.fit.service.WxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 微信内容安全检测实现 —— 调用 msgSecCheck / imgSecCheck
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityCheckServiceImpl implements SecurityCheckService {

    private static final String MSG_SEC_CHECK_URL =
            "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=%s";
    private static final String IMG_SEC_CHECK_URL =
            "https://api.weixin.qq.com/wxa/img_sec_check?access_token=%s";

    private final WxService wxService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wx.mock:false}")
    private boolean mock;

    @Override
    public boolean checkText(String content) {
        if (content == null || content.isBlank()) {
            return true;
        }

        if (mock) {
            log.warn("微信Mock模式：跳过文本安全检测, content={}", content);
            return true;
        }

        try {
            String token = wxService.getAccessToken();
            String url = String.format(MSG_SEC_CHECK_URL, token);

            // 构建请求体 { "content": "...", "version": 2, "scene": 2 }
            String body = objectMapper.writeValueAsString(
                    new MsgSecCheckRequest(content, 2, 2));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String resp = restTemplate.postForObject(url, entity, String.class);
            JsonNode node = objectMapper.readTree(resp);

            int errcode = node.get("errcode").asInt();
            if (errcode == 0) {
                JsonNode result = node.get("result");
                String suggest = result != null ? result.get("suggest").asText() : "pass";
                boolean pass = "pass".equals(suggest);
                log.info("文本安全检测: pass={}, suggest={}", pass, suggest);
                return pass;
            }
            // 仅 errcode=87014 表示内容确实违规，其余均为 API 调用异常（如 token 失效等），放行
            String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "";
            if (errcode == 87014) {
                log.warn("文本安全检测不通过（内容违规）: errcode={}, errmsg={}", errcode, errmsg);
                return false;
            }
            log.warn("文本安全检测API异常（已放行）: errcode={}, errmsg={}", errcode, errmsg);
            return true;

        } catch (Exception e) {
            // 检测服务异常时放行，避免阻塞正常用户操作
            log.warn("文本安全检测服务异常（已放行）: {}", e.getMessage());
            return true;
        }
    }

    @Override
    public boolean checkImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return true;
        }

        if (mock) {
            log.warn("微信Mock模式：跳过图片安全检测, size={} bytes", imageBytes.length);
            return true;
        }

        try {
            String token = wxService.getAccessToken();
            String url = String.format(IMG_SEC_CHECK_URL, token);

            // 构建 multipart/form-data，字段名 media
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("media", new ByteArrayResourceWithFilename(imageBytes, "image.jpg"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            String resp = restTemplate.postForObject(url, entity, String.class);
            JsonNode node = objectMapper.readTree(resp);

            int errcode = node.get("errcode").asInt();
            if (errcode == 0) {
                log.info("图片安全检测: pass");
                return true;
            }
            // 仅 errcode=87014 表示内容确实违规，其余均为 API 调用异常（如 token 失效等），放行
            String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "";
            if (errcode == 87014) {
                log.warn("图片安全检测不通过（内容违规）: errcode={}, errmsg={}", errcode, errmsg);
                return false;
            }
            log.warn("图片安全检测API异常（已放行）: errcode={}, errmsg={}", errcode, errmsg);
            return true;

        } catch (Exception e) {
            // 检测服务异常时放行，避免阻塞正常用户操作
            log.warn("图片安全检测服务异常（已放行）: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 文本检测请求体
     */
    private record MsgSecCheckRequest(String content, int version, int scene) {}

    /**
     * ByteArrayResource 子类，支持自定义文件名
     */
    private static class ByteArrayResourceWithFilename extends org.springframework.core.io.ByteArrayResource {
        private final String filename;

        ByteArrayResourceWithFilename(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
