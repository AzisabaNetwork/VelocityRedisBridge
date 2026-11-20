package net.azisaba.velocityredisbridge.command;

import com.velocitypowered.api.command.RawCommand;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.util.PlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Admin command that shows the player count tracked in Redis and the player counts obtained by
 * pinging external servers.
 */
@RequiredArgsConstructor
public class PlayerCountCommand implements RawCommand {

  public static final String PERMISSION = "velocityredisbridge.command.playercount";

  private final VelocityRedisBridge plugin;

  @Override
  public void execute(Invocation invocation) {
    int redisCount = plugin.getPlayerInfoHandler().getAllPlayersCount();
    Map<String, Integer> proxyCounts = new TreeMap<>();
    for (PlayerInfo info : plugin.getPlayerInfoHandler().getAllPlayerInfo().values()) {
      proxyCounts.merge(String.valueOf(info.getProxyServer()), 1, Integer::sum);
    }

    Map<String, Integer> externalCounts =
        plugin.getExternalServerPingHandler().getPlayerCountsByServer();
    int externalTotal = plugin.getExternalServerPingHandler().getTotalPlayerCount();

    TextComponent.Builder builder = Component.text();
    builder.append(header("Redis: " + redisCount));
    proxyCounts.forEach((proxy, count) -> builder.append(entry(proxy, count)));

    builder.append(Component.newline()).append(header("External (ping): " + externalTotal));
    if (externalCounts.isEmpty()) {
      builder.append(
          Component.newline()
              .append(Component.text("  (no external servers configured)", NamedTextColor.GRAY)));
    }
    externalCounts.forEach((server, count) -> builder.append(entry(server, count)));

    builder
        .append(Component.newline())
        .append(header("Total: " + (redisCount + externalTotal)));

    invocation.source().sendMessage(builder.build());
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().hasPermission(PERMISSION);
  }

  private static Component header(String text) {
    return Component.text(text, NamedTextColor.GREEN);
  }

  private static Component entry(String name, Integer count) {
    Component value =
        count == null
            ? Component.text("unreachable", NamedTextColor.RED)
            : Component.text(count, NamedTextColor.WHITE);
    return Component.newline()
        .append(Component.text("  " + name + ": ", NamedTextColor.GRAY))
        .append(value);
  }
}
