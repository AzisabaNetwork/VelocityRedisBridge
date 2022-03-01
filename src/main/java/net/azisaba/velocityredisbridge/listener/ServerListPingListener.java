package net.azisaba.velocityredisbridge.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;

@RequiredArgsConstructor
public class ServerListPingListener {

  private final VelocityRedisBridge plugin;

  @Subscribe
  public void onProxyPing(ProxyPingEvent event) {
    event.setPing(
        event
            .getPing()
            .asBuilder()
            .onlinePlayers(plugin.getPlayerInfoHandler().getAllPlayersCount())
            .build());
  }
}
