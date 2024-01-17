package com.example.securenoteapp.model.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class UserAgentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String userAgent;
    private String ip;
    private String method;
    private String url;
    private String payload;
    private String referrer;
    private String sessionId;
    private LocalDateTime timestamp;

    public UserAgentInfo() {
    }

    public UserAgentInfo(String userAgent, String ip, String method, String url, String payload, String referrer, String sessionId, LocalDateTime timestamp) {
        this.userAgent = userAgent;
        this.ip = ip;
        this.method = method;
        this.url = url;
        this.payload = payload;
        this.referrer = referrer;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
    }

    // getters and setters...
}
