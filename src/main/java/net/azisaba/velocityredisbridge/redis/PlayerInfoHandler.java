package net.azisaba.velocityredisbridge.redis;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.util.PlayerInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

@RequiredArgsConstructor
public class PlayerInfoHandler {

  private final VelocityRedisBridge plugin;
  private final JedisPool jedisPool;
  private final String proxyId;
  private final Gson gson = new Gson();

  private final ReentrantLock lock = new ReentrantLock();
  private final HashMap<UUID, PlayerInfo> playerInfoMap = new HashMap<>();
  private final HashMap<String, Set<PlayerInfo>> playerInfoProxyServerMap = new HashMap<>();
  private final HashMap<String, Set<PlayerInfo>> playerInfoChildServerMap = new HashMap<>();

  public boolean register(PlayerInfo info) {
    boolean redisSuccess;
    try (Jedis jedis = jedisPool.getResource()) {
      String returnValue =
          jedis.set(
              RedisKeys.PLAYERS_KEY_PREFIX + ":" + info.getUuid().toString(),
              gson.toJson(info),
              SetParams.setParams()
                  .ex(plugin.getVelocityRedisBridgeConfig().getRedisCacheExpireSeconds())
                  .nx());

      redisSuccess = returnValue != null;

      if (redisSuccess) {
        jedis.publish(RedisKeys.INFO_UPDATE_NOTIFY.getKey(), gson.toJson(info));
      }
    }

    if (!redisSuccess) {
      return false;
    }

    lock.lock();
    try {
      updateToNewPlayerInfo(info);
    } finally {
      lock.unlock();
    }
    return true;
  }

  public void update(PlayerInfo info) {
    lock.lock();
    try {
      updateToNewPlayerInfo(info);
    } finally {
      lock.unlock();
    }

    try (Jedis jedis = jedisPool.getResource()) {
      String strData = gson.toJson(info);
      jedis.set(
          RedisKeys.PLAYERS_KEY_PREFIX + ":" + info.getUuid().toString(),
          strData,
          SetParams.setParams()
              .ex(plugin.getVelocityRedisBridgeConfig().getRedisCacheExpireSeconds()));

      jedis.publish(RedisKeys.INFO_UPDATE_NOTIFY.getKey(), strData);
    }
  }

  public void unregister(UUID uuid) {
    lock.lock();
    try {
      PlayerInfo info = playerInfoMap.remove(uuid);
      if (info != null) {
        playerInfoProxyServerMap
            .getOrDefault(info.getProxyServer(), Collections.emptySet())
            .remove(info);
        playerInfoChildServerMap
            .getOrDefault(info.getChildServer(), Collections.emptySet())
            .remove(info);
      }
    } finally {
      lock.unlock();
    }

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(RedisKeys.PLAYERS_KEY_PREFIX + ":" + uuid.toString());
      jedis.publish(
          RedisKeys.INFO_UPDATE_NOTIFY.getKey(),
          gson.toJson(new PlayerInfo(uuid, null, null, -1, null, null)));
    }
  }

  protected void receivedUpdateNotify(PlayerInfo info) {
    lock.lock();
    try {
      updateToNewPlayerInfo(info);
    } finally {
      lock.unlock();
    }
  }

  public int getAllPlayersCount() {
    lock.lock();
    try {
      return playerInfoMap.size();
    } finally {
      lock.unlock();
    }
  }

  public int getPlayersCount(String serverName) {
    lock.lock();
    try {
      return playerInfoChildServerMap.getOrDefault(serverName, Collections.emptySet()).size();
    } finally {
      lock.unlock();
    }
  }

  public PlayerInfo get(UUID uuid) {
    lock.lock();
    try {
      return playerInfoMap.get(uuid);
    } finally {
      lock.unlock();
    }
  }

  public PlayerInfo get(String userName) {
    lock.lock();
    try {
      for (PlayerInfo info : playerInfoMap.values()) {
        if (info.getUsername().equals(userName)) {
          return info;
        }
      }
    } finally {
      lock.unlock();
    }
    return null;
  }

  public HashMap<UUID, PlayerInfo> getAllPlayerInfo() {
    lock.lock();
    try {
      return new HashMap<>(playerInfoMap);
    } finally {
      lock.unlock();
    }
  }

  public List<PlayerInfo> getAllPlayerInfoInProxy(String proxySeverId) {
    lock.lock();
    try {
      if (!playerInfoProxyServerMap.containsKey(proxySeverId)) {
        return Collections.emptyList();
      }
      return new ArrayList<>(playerInfoProxyServerMap.get(proxySeverId));
    } finally {
      lock.unlock();
    }
  }

  public List<PlayerInfo> getAllPlayerInfoInChildServer(String childServer) {
    lock.lock();
    try {
      if (!playerInfoChildServerMap.containsKey(childServer)) {
        return Collections.emptyList();
      }
      return new ArrayList<>(playerInfoChildServerMap.get(childServer));
    } finally {
      lock.unlock();
    }
  }

  public void fetch() {
    lock.lock();
    try {
      try (Jedis jedis = jedisPool.getResource()) {
        playerInfoMap.clear();
        playerInfoProxyServerMap.clear();
        playerInfoChildServerMap.clear();

        for (String key : jedis.keys(RedisKeys.PLAYERS_KEY_PREFIX + ":*")) {
          String jsonStr = jedis.get(key);
          PlayerInfo info = gson.fromJson(jsonStr, PlayerInfo.class);

          updateToNewPlayerInfo(info);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  public void updateAllRedisKeys() {
    try (Jedis jedis = jedisPool.getResource()) {
      for (Player player : plugin.getProxy().getAllPlayers()) {
        String key = RedisKeys.PLAYERS_KEY_PREFIX + ":" + player.getUniqueId().toString();
        long result =
            jedis.expire(key, plugin.getVelocityRedisBridgeConfig().getRedisCacheExpireSeconds());

        if (result == 0) {
          PlayerInfo info =
              new PlayerInfo(
                  player.getUniqueId(),
                  player.getUsername(),
                  player.getRemoteAddress().getHostName(),
                  player.getRemoteAddress().getPort(),
                  proxyId,
                  player
                      .getCurrentServer()
                      .map(server -> server.getServerInfo().getName())
                      .orElse(null));

          register(info);
        }
      }
    }
  }

  private void updateToNewPlayerInfo(PlayerInfo info) {

    PlayerInfo old = playerInfoMap.remove(info.getUuid());
    if (old != null) {
      playerInfoProxyServerMap
          .getOrDefault(old.getProxyServer(), Collections.emptySet())
          .remove(old);
      playerInfoChildServerMap
          .getOrDefault(old.getChildServer(), Collections.emptySet())
          .remove(old);
    }

    if (info.getUuid() != null && info.getUsername() == null) {
      return;
    }

    playerInfoMap.put(info.getUuid(), info);
    if (playerInfoProxyServerMap.containsKey(info.getProxyServer())) {
      playerInfoProxyServerMap.get(info.getProxyServer()).add(info);
    } else {
      playerInfoProxyServerMap.put(
          info.getProxyServer(), new HashSet<>(Collections.singletonList(info)));
    }
    if (playerInfoChildServerMap.containsKey(info.getChildServer())) {
      playerInfoChildServerMap.get(info.getChildServer()).add(info);
    } else {
      playerInfoChildServerMap.put(
          info.getChildServer(), new HashSet<>(Collections.singletonList(info)));
    }
  }
}
