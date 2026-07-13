package com.fit.service;

/**
 * 微信内容安全检测服务
 */
public interface SecurityCheckService {

    /**
     * 文本内容安全检测
     *
     * @param content 待检测文本（如昵称）
     * @return true=安全，false=含违规内容
     */
    boolean checkText(String content);

    /**
     * 图片内容安全检测
     *
     * @param imageBytes 图片二进制数据
     * @return true=安全，false=含违规内容
     */
    boolean checkImage(byte[] imageBytes);
}
