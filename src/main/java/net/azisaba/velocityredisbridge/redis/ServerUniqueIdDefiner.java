package net.azisaba.velocityredisbridge.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class ServerUniqueIdDefiner {

  private final JedisPool jedisPool;

  @Getter private String uniqueId;

  public String define() {
    Jedis jedis = jedisPool.getResource();

    if (jedis == null) {
      throw new IllegalStateException("Could not get Jedis instance.");
    }

    String id = null;
    try {
      while (id == null || jedis.keys(RedisKeys.PLAYERS_KEY_PREFIX + ":" + id + ":*").size() > 0) {
        id = RandomStringUtils.randomAlphanumeric(8);
      }
    } finally {
      jedis.close();
    }
    uniqueId = id;
    return uniqueId;
  }
}
