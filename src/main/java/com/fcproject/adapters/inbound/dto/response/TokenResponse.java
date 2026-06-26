package com.fcproject.adapters.inbound.dto.response;

public class TokenResponse {
    private String acessToken;
    private boolean authenticated;
    private String refreshToken;
    private Long expiresIn;
    private String type = "Bearer";

    public TokenResponse(){}

    public TokenResponse(String acessToken, boolean authenticated, String refreshToken, Long expiresIn, String type) {
        this.acessToken = acessToken;
        this.authenticated = authenticated;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.type = type;
    }

    public String getAcessToken() {
        return acessToken;
    }

    public void setAcessToken(String acessToken) {
        this.acessToken = acessToken;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

