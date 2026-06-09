package com.woorifisa.won_card_channel_server.domain.aidb.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Neo4jUser {

    public static final String LABEL = "User";

    private UUID userUuid;
    private UUID cardUserUuid;
    private UUID investUserUuid;
    private String displayName;
    private String userStatus;
}
