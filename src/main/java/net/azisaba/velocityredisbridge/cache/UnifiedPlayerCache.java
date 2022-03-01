package net.azisaba.velocityredisbridge.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnifiedPlayerCache {

  private final HashMap<UUID, String> playerList;
  private final HashMap<String, Set<UUID>> childServerMap;
  private final HashMap<String, Set<UUID>> proxyServerMap;

  private final ReentrantLock lock = new ReentrantLock();

  private HashMap<String, UUID> userNameToUUID = null;

  public Map<UUID, String> getAllPlayers() {
    lock.lock();
    try {
      return new HashMap<>(playerList);
    } finally {
      lock.unlock();
    }
  }

  public Map<String, Set<UUID>> getChildServerMap() {
    lock.lock();
    try {
      return new HashMap<>(childServerMap);
    } finally {
      lock.unlock();
    }
  }

  public Map<String, Set<UUID>> getProxyServerMap() {
    lock.lock();
    try {
      return new HashMap<>(proxyServerMap);
    } finally {
      lock.unlock();
    }
  }

  public UUID getUUIDFromPlayerName(String playerName) {
    lock.lock();
    try {
      if (userNameToUUID == null) {
        userNameToUUID = new HashMap<>();
        for (Map.Entry<UUID, String> entry : playerList.entrySet()) {
          userNameToUUID.put(entry.getValue(), entry.getKey());
        }
      }
      return userNameToUUID.getOrDefault(playerName, null);
    } finally {
      lock.unlock();
    }
  }

  public int getPlayerCount() {
    lock.lock();
    try {
      return playerList.size();
    } finally {
      lock.unlock();
    }
  }

  public int getPlayerCount(String serverName) {
    lock.lock();
    try {
      if (childServerMap.containsKey(serverName)) {
        return childServerMap.get(serverName).size();
      }
      return 0;
    } finally {
      lock.unlock();
    }
  }

  public void setPlayerInfo(UUID uuid, String playerName, String proxy, String childServer) {
    lock.lock();
    try {
      if (playerName != null) {
        playerList.put(uuid, playerName);
      }

      if (proxy != null) {
        if (!proxyServerMap.containsKey(proxy)) {
          proxyServerMap.put(proxy, new HashSet<>());
        }
        proxyServerMap.get(proxy).add(uuid);
      }

      if (childServer != null) {
        if (!childServerMap.containsKey(childServer)) {
          childServerMap.put(childServer, new HashSet<>());
        }
        childServerMap.get(childServer).add(uuid);
      }
    } finally {
      lock.unlock();
    }
  }

  public void removePlayerInfo(UUID uuid) {
    lock.lock();
    try {
      String playerName = playerList.remove(uuid);
      if (userNameToUUID != null && playerName != null) {
        userNameToUUID.remove(playerName);
      }

      for (Map.Entry<String, Set<UUID>> entry : proxyServerMap.entrySet()) {
        if (entry.getValue().contains(uuid)) {
          entry.getValue().remove(uuid);
          break;
        }
      }
      for (Map.Entry<String, Set<UUID>> entry : childServerMap.entrySet()) {
        if (entry.getValue().contains(uuid)) {
          entry.getValue().remove(uuid);
          break;
        }
      }
    } finally {
      lock.unlock();
    }
  }
}
