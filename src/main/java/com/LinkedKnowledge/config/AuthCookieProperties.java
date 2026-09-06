package com.LinkedKnowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {
    private String name = "token";
    private boolean httpOnly = true;
    private boolean secure = false;
    private String sameSite = "Lax";
    private String path = "/";
    private int maxAgeSeconds = 604800;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isHttpOnly() { return httpOnly; }
    public void setHttpOnly(boolean httpOnly) { this.httpOnly = httpOnly; }
    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }
    public String getSameSite() { return sameSite; }
    public void setSameSite(String sameSite) { this.sameSite = sameSite; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public int getMaxAgeSeconds() { return maxAgeSeconds; }
    public void setMaxAgeSeconds(int maxAgeSeconds) { this.maxAgeSeconds = maxAgeSeconds; }
}
