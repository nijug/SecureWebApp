package com.example.securenoteapp.controller;

import com.example.securenoteapp.model.data.UserAgentInfo;
import com.example.securenoteapp.model.repository.UserAgentInfoRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Order(1)
@Component
public class PayloadLoggingFilter implements Filter {

    private final UserAgentInfoRepository userAgentInfoRepository;

    public PayloadLoggingFilter(UserAgentInfoRepository userAgentInfoRepository) {
        this.userAgentInfoRepository = userAgentInfoRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getRequestURI().startsWith("/admin")) {
            String payload = StreamUtils.copyToString(httpServletRequest.getInputStream(), StandardCharsets.UTF_8);
            String userAgent = httpServletRequest.getHeader("User-Agent");
            String ip = httpServletRequest.getRemoteAddr();
            String method = httpServletRequest.getMethod();
            String url = httpServletRequest.getRequestURL().toString();
            String referrer = httpServletRequest.getHeader("Referer");
            String sessionId = httpServletRequest.getSession().getId();
            LocalDateTime timestamp = LocalDateTime.now();

            UserAgentInfo userAgentInfo = new UserAgentInfo(userAgent, ip, method, url, payload, referrer, sessionId, timestamp);
            System.out.println("yoink "+userAgentInfo);
            userAgentInfoRepository.save(userAgentInfo);
        }

        chain.doFilter(request, response);
    }
}
