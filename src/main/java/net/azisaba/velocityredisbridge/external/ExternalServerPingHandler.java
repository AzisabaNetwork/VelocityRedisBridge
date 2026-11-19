package net.azisaba.velocityredisbridge.external;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;

/**
 * Periodically pings servers outside the Velocity network and caches their online player counts.
 */
public class ExternalServerPingHandler {

  private final VelocityRedisBridge plugin;
  private final List<RegisteredServer> targets;
  private final Map<String, Integer> playerCounts = new ConcurrentHashMap<>();
  private final Set<String> failingServers = ConcurrentHashMap.newKeySet();

  public ExternalServerPingHandler(VelocityRedisBridge plugin, List<String> addresses) {
    this.plugin = plugin;
    this.targets =
        addresses.stream()
            .map(ExternalServerPingHandler::parseAddress)
            .map(
                address ->
                    plugin
                        .getProxy()
                        .createRawRegisteredServer(
                            new ServerInfo(
                                "vrb-external-"
                                    + address.getHostString()
                                    + ":"
                                    + address.getPort(),
                                address)))
            .collect(Collectors.toList());
  }

  public void start(long intervalSeconds) {
    if (targets.isEmpty()) {
      return;
    }
    plugin
        .getProxy()
        .getScheduler()
        .buildTask(plugin, this::pingAll)
        .repeat(Math.max(1, intervalSeconds), TimeUnit.SECONDS)
        .schedule();
  }

  private void pingAll() {
    for (RegisteredServer server : targets) {
      String name = server.getServerInfo().getName();
      server
          .ping()
          .orTimeout(5, TimeUnit.SECONDS)
          .thenApply(ping -> ping.getPlayers().map(ServerPing.Players::getOnline).orElse(0))
          .whenComplete(
              (count, throwable) -> {
                if (throwable != null) {
                  playerCounts.remove(name);
                  if (failingServers.add(name)) {
                    plugin
                        .getLogger()
                        .log(
                            Level.WARNING,
                            "Failed to ping external server " + name + ": " + throwable);
                  }
                  return;
                }
                playerCounts.put(name, count);
                if (failingServers.remove(name)) {
                  plugin.getLogger().info("External server " + name + " is reachable again.");
                }
              });
    }
  }

  /** Returns the total number of players on all reachable external servers. */
  public int getTotalPlayerCount() {
    return playerCounts.values().stream().mapToInt(Integer::intValue).sum();
  }

  private static InetSocketAddress parseAddress(String address) {
    String host = address.trim();
    int port = 25565;
    int colon = host.lastIndexOf(':');
    if (colon > 0 && host.indexOf(':') == colon) {
      port = Integer.parseInt(host.substring(colon + 1));
      host = host.substring(0, colon);
    }
    return InetSocketAddress.createUnresolved(host, port);
  }
}
