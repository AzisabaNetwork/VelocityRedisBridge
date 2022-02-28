package net.azisaba.velocityredisbridge.redis;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.cache.UnifiedPlayerCache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class RedisPlayerDataHandler {

  private final VelocityRedisBridge plugin;
  private final JedisPool jedisPool;
  private final String uniqueId;

  public void update() {
    Jedis jedis = jedisPool.getResource();

    if (jedis == null) {
      throw new IllegalStateException("Could not get Jedis instance.");
    }

    try {
      for (RegisteredServer server : plugin.getProxy().getAllServers()) {
        HashMap<String, String> uuidAndPlayerNames = new HashMap<>();
        for (Player p : server.getPlayersConnected()) {
          uuidAndPlayerNames.put(p.getUniqueId().toString(), p.getUsername());
        }
        String key =
            RedisKeys.PLAYERS_KEY_PREFIX + ":" + uniqueId + ":" + server.getServerInfo().getName();
        jedis.hset(key, uuidAndPlayerNames);
        jedis.expire(key, plugin.getVelocityRedisBridgeConfig().getRedisCacheExpireSeconds());
      }
    } finally {
      jedis.close();
    }
  }

  public UnifiedPlayerCache fetch() {
    Jedis jedis = jedisPool.getResource();

    if (jedis == null) {
      throw new IllegalStateException("Could not get a Jedis instance.");
    }

    HashMap<UUID, String> playerList = new HashMap<>();
    HashMap<String, Set<UUID>> childServerMap = new HashMap<>();
    HashMap<String, Set<UUID>> proxyServerMap = new HashMap<>();

    try {
      jedis
          .keys(RedisKeys.PLAYERS_KEY_PREFIX + ":*")
          .forEach(
              key -> {
                Map<String, String> fetchedPlayerMap = jedis.hgetAll(key);
                HashMap<UUID, String> convertedMap = new HashMap<>();
                for (String uuid : fetchedPlayerMap.keySet()) {
                  convertedMap.put(UUID.fromString(uuid), fetchedPlayerMap.get(uuid));
                }

                playerList.putAll(convertedMap);
                String[] splittedKey = key.split(":");
                String proxyServerId = splittedKey[splittedKey.length - 2];
                String childServerId = splittedKey[splittedKey.length - 1];

                if (proxyServerMap.containsKey(proxyServerId)) {
                  proxyServerMap.put(
                      proxyServerId,
                      combine(proxyServerMap.get(proxyServerId), convertedMap.keySet()));
                } else {
                  proxyServerMap.put(proxyServerId, convertedMap.keySet());
                }

                if (childServerMap.containsKey(childServerId)) {
                  childServerMap.put(
                      childServerId,
                      combine(childServerMap.get(childServerId), convertedMap.keySet()));
                } else {
                  childServerMap.put(childServerId, convertedMap.keySet());
                }
              });
    } finally {
      jedis.close();
    }

    return new UnifiedPlayerCache(playerList, childServerMap, proxyServerMap);
  }

  private Set<UUID> combine(Set<UUID> one, Set<UUID> two) {
    List<UUID> combinedList = new ArrayList<>(one);
    combinedList.addAll(two);
    return new HashSet<>(combinedList);
  }
}
