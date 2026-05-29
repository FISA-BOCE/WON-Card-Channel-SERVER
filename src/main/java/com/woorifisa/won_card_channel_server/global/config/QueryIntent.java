package com.woorifisa.won_card_channel_server.global.config;

public enum QueryIntent {

    // MySQL 카드
    TOTAL_SPEND, FOOD_SPEND, SHOPPING_SPEND, TRANSPORT_SPEND,
    SUBSCRIPTION_SPEND, POINT_BALANCE, POINT_EARNED, REWARD_STATUS,

    // MySQL 증권
    ETF_HOLDINGS, ETF_AMOUNT,

    // Neo4j
    MERCHANT_TO_ETF, POINT_BY_MERCHANT, SPEND_BY_CATEGORY,

    // 기타
    UNKNOWN
}
