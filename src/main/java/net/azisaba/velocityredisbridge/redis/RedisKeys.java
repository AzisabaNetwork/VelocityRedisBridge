package net.azisaba.velocityredisbridge.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RedisKeys {
  ALL_KEY_PREFIX("velocity-redis-bridge"),

  // K/V keys
  PLAYERS_KEY_PREFIX("velocity-redis-bridge:players"),
  IP_KEY_PREFIX("velocity-redis-bridge:ip"),

  // Pub/Sub Keys
  SEND_SERVER_REQUEST("velocity-redis-bridge:send-request"),
  SEND_MESSAGE_TO_ALL("velocity-redis-bridge:send-message-to-all"),
  SEND_MESSAGE_TO_PLAYER("velocity-redis-bridge:send-message-to-player"),
  SEND_RAW_MESSAGE_TO_ALL("velocity-redis-bridge:send-raw-message-to-all"),
  SEND_RAW_MESSAGE_TO_PLAYER("velocity-redis-bridge:send-raw-message-to-player"),
  KICK_PLAYER("velocity-redis-bridge:kick-player"),
  INFO_UPDATE_NOTIFY("velocity-redis-bridge:info-update-notify");

  @Getter private final String key;

  @Override
  public String toString() {
    return key;
  }
}
