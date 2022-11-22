package net.azisaba.velocityredisbridge.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.util.PlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@RequiredArgsConstructor
public class PlayerJoinQuitListener {

  private final VelocityRedisBridge plugin;
  private final String proxyId;

  @Subscribe(order = PostOrder.LAST)
  public void onJoin(LoginEvent event) {
    if (!event.getResult().isAllowed()) {
      return;
    }

    UUID uuid = event.getPlayer().getUniqueId();
    String userName = event.getPlayer().getUsername();
    String hostName = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
    int port = event.getPlayer().getRemoteAddress().getPort();

    boolean result = plugin.getPlayerInfoHandler()
        .register(new PlayerInfo(uuid, userName, hostName, port, proxyId, null));

    if (!result) {
      event.setResult(
          ComponentResult.denied(
              Component.text("あなたは既にサーバーに参加しています！").color(NamedTextColor.RED)));
    }
  }

  @Subscribe
  public void onQuit(DisconnectEvent event) {
    if (event.getLoginStatus() != LoginStatus.SUCCESSFUL_LOGIN
        && event.getLoginStatus() != LoginStatus.PRE_SERVER_JOIN) {
      return;
    }

    UUID uuid = event.getPlayer().getUniqueId();
    PlayerInfo info = plugin.getPlayerInfoHandler().get(uuid);

    if (info == null) {
      return;
    }
    if (info.getProxyServer() != null && info.getProxyServer().equals(proxyId)) {
      plugin.getPlayerInfoHandler().unregister(uuid);
    }
  }

  @Subscribe
  public void onServerChanged(ServerConnectedEvent event) {
    plugin
        .getProxy()
        .getScheduler()
        .buildTask(
            plugin,
            () -> {
              String serverName = event.getServer().getServerInfo().getName();
              PlayerInfo info = plugin.getPlayerInfoHandler().get(event.getPlayer().getUniqueId());

              if (info == null) {
                UUID uuid = event.getPlayer().getUniqueId();
                String userName = event.getPlayer().getUsername();
                String hostName = event.getPlayer().getRemoteAddress().getHostName();
                int port = event.getPlayer().getRemoteAddress().getPort();

                info = new PlayerInfo(uuid, userName, hostName, port, proxyId, serverName);
              }

              info.setChildServer(serverName);
              plugin.getPlayerInfoHandler().update(info);
            })
        .schedule();
  }
}
