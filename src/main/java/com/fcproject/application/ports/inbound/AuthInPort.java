package com.fcproject.application.ports.inbound;

public interface AuthInPort {
    String login(String email, String password);
}
