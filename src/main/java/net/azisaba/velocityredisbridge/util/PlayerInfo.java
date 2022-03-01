package net.azisaba.velocityredisbridge.util;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerInfo {

  private UUID uuid;
  private String username;
  private String hostName;
  private int port;
  private String proxyServer;
  private String childServer;

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PlayerInfo)) {
      return false;
    }

    final PlayerInfo other = (PlayerInfo) obj;

    return uuid.equals(other.uuid);
  }
}
