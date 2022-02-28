package net.azisaba.velocityredisbridge.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.cache.UnifiedIPCache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class IpDataFetcher {

  private final JedisPool jedisPool;

  public UnifiedIPCache fetch() {
    Jedis jedis = jedisPool.getResource();

    if (jedis == null) {
      throw new IllegalStateException("Could not get a Jedis instance.");
    }

    HashMap<UUID, String> hostMap = new HashMap<>();
    HashMap<UUID, Integer> portMap = new HashMap<>();

    try {
      jedis
          .keys(RedisKeys.IP_KEY_PREFIX + ":*")
          .forEach(
              key -> {
                Map<String, String> fetchedIpMap = jedis.hgetAll(key);
                for (String uuidStr : fetchedIpMap.keySet()) {
                  UUID uuid = UUID.fromString(uuidStr);

                  String hostName = fetchedIpMap.get(uuidStr).split(":")[0];
                  int port = Integer.parseInt(fetchedIpMap.get(uuidStr).split(":")[1]);

                  hostMap.put(uuid, hostName);
                  portMap.put(uuid, port);
                }
              });
    } finally {
      jedis.close();
    }

    return new UnifiedIPCache(hostMap, portMap);
  }
}
