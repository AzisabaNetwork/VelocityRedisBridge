package net.azisaba.velocityredisbridge.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnifiedPlayerCache {

  private final HashMap<UUID, String> playerList;
  private final HashMap<String, Set<UUID>> childServerMap;
  private final HashMap<String, Set<UUID>> proxyServerMap;

  private HashMap<String, UUID> userNameToUUID = null;

  public Map<UUID, String> getAllPlayers() {
    return new HashMap<>(playerList);
  }

  public Map<String, Set<UUID>> getChildServerMap() {
    return new HashMap<>(childServerMap);
  }

  public Map<String, Set<UUID>> getProxyServerMap() {
    return new HashMap<>(proxyServerMap);
  }

  public UUID getUUIDFromPlayerName(String playerName) {
    if (userNameToUUID == null) {
      userNameToUUID = new HashMap<>();
      for (Map.Entry<UUID, String> entry : playerList.entrySet()) {
        userNameToUUID.put(entry.getValue(), entry.getKey());
      }
    }
    return userNameToUUID.getOrDefault(playerName, null);
  }

  public int getPlayerCount() {
    return playerList.size();
  }

  public int getPlayerCount(String serverName) {
    if (childServerMap.containsKey(serverName)) {
      return childServerMap.get(serverName).size();
    }
    return 0;
  }
}
