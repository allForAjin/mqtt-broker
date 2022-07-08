package com.lmk.mqtt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author lmk
 */
@SpringBootApplication
public class MqttServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttServerApplication.class, args);
//        new MqttBroker().startUp();
    }

}
