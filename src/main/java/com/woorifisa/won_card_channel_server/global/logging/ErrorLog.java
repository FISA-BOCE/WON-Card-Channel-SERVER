package com.woorifisa.won_card_channel_server.global.logging;

import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "error_log")
public class ErrorLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int status;

    @Column(length = 10)
    private String method;

    @Column(length = 500)
    private String uri;

    private long elapsedMs;

    @Builder
    private ErrorLog(int status, String method, String uri, long elapsedMs) {
        this.status = status;
        this.method = method;
        this.uri = uri;
        this.elapsedMs = elapsedMs;
    }
}
