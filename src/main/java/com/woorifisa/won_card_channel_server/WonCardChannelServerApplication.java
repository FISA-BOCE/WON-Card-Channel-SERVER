package com.woorifisa.won_card_channel_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients
@SpringBootApplication
public class WonCardChannelServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WonCardChannelServerApplication.class, args);
    }

}
