package net.azisaba.velocityredisbridge.command;

import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@RequiredArgsConstructor
public class VelocityRedisBridgeCommand implements RawCommand {

  private final String velocityId;

  @Override
  public void execute(Invocation invocation) {
    Component component = Component.text("ProxyID: " + velocityId).color(NamedTextColor.GREEN);
    if (invocation.source() instanceof Player) {
      String serverName = ((Player) invocation.source()).getCurrentServer()
          .map(s -> s.getServerInfo().getName()).orElse("None");

      component = component.append(Component.newline())
          .append(Component.text("Server: " + serverName).color(NamedTextColor.GREEN));
    }

    invocation.source().sendMessage(component);
  }
}
