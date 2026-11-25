package com.enterprise.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  private String secret = "5a6f8b3c2d1e4f7a9b0c3d6e1f4a7b2c5d8e1f4a7b0c3d6e9f2a5b8c1d4e7f0";
  private long expirationMs = 86400000L;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpirationMs() {
    return expirationMs;
  }

  public void setExpirationMs(long expirationMs) {
    this.expirationMs = expirationMs;
  }
}
