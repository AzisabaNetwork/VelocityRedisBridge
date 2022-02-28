package net.azisaba.velocityredisbridge.util;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserHostAndPort {

  private final UUID uuid;
  private final String host;
  private final int port;
}
