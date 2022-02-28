package net.azisaba.velocityredisbridge.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.util.UserHostAndPort;

@RequiredArgsConstructor
public class UnifiedIPCache {

  private final HashMap<UUID, String> ipMap;
  private final HashMap<UUID, Integer> portMap;

  public UserHostAndPort getIp(UUID uuid) {
    return new UserHostAndPort(uuid, ipMap.get(uuid), portMap.get(uuid));
  }

  public Map<UUID, UserHostAndPort> getAllIpMap() {
    Map<UUID, UserHostAndPort> map = new HashMap<>();
    for (UUID uuid : ipMap.keySet()) {
      map.put(uuid, new UserHostAndPort(uuid, ipMap.get(uuid), portMap.get(uuid)));
    }
    return map;
  }
}
