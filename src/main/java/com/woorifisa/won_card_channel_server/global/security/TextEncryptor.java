package com.woorifisa.won_card_channel_server.global.security;

public interface TextEncryptor {

    String encrypt(String plainText);

    String decrypt(String cipherText);

    String decryptIfPossible(String value);
}
