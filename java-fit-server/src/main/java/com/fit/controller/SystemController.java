package com.fit.controller;

import com.fit.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet6Address;
import java.net.InetAddress;

@Slf4j
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/info")
    public Result<SystemInfoDTO> info(HttpServletRequest request) {
        String loginIp = request.getHeader("X-Forwarded-For");
        if (loginIp == null || loginIp.isBlank()) {
            loginIp = request.getHeader("X-Real-IP");
        }
        if (loginIp == null || loginIp.isBlank()) {
            loginIp = request.getRemoteAddr();
        }
        // X-Forwarded-For may be a comma-separated proxy chain: "client, proxy1, proxy2"
        if (loginIp != null && loginIp.contains(",")) {
            loginIp = loginIp.split(",")[0].trim();
        }
        loginIp = toIpv4(loginIp);
        log.debug("System info requested, client IP: {}", loginIp);
        return Result.success(new SystemInfoDTO(loginIp));
    }

    /**
     * Convert IPv6 loopback / IPv4-mapped IPv6 addresses to pure IPv4.
     * Keeps the status bar showing the familiar dotted-quad format.
     */
    private String toIpv4(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            // IPv6 loopback → plain IPv4 loopback
            if (addr instanceof Inet6Address && addr.isLoopbackAddress()) {
                return "127.0.0.1";
            }
        } catch (Exception ignored) {
            // unparseable — return as-is
        }
        return ip;
    }

    public record SystemInfoDTO(String loginIp) {}
}
