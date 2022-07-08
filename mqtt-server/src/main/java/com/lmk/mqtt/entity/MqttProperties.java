package com.lmk.mqtt.entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author lmk
 * @version 1.0.0
 * @ClassName MqttProperties.java
 * @Description TODO
 * @createTime 2022-06-26 10:42:11
 */
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private Integer port;
    private String username;
    private String password;


    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "MqttProperties{" +
                "port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
