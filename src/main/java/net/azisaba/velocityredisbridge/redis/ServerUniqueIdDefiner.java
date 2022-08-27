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
      boolean success = false;
      while (!success) {
        id = RandomStringUtils.randomAlphanumeric(8);
        success = jedis.setnx(RedisKeys.SERVER_ID_PREFIX + ":" + id, "using") != 0;
      }

      jedis.expire(RedisKeys.SERVER_ID_PREFIX + ":" + id, 600);
    } finally {
      jedis.close();
    }
    uniqueId = id;
    return uniqueId;
  }
}
