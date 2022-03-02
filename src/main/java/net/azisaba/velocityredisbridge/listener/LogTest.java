package net.azisaba.velocityredisbridge.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public class LogTest {

  private final String uniqueId;

  @Subscribe
  public void onJoin(LoginEvent event) {
    Player p = event.getPlayer();
    p.sendMessage(Component.text("あなたは現在 VelocityID " + uniqueId + " に参加しています"));
  }
}
