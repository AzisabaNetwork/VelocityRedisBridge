package net.azisaba.velocityredisbridge.command;

import com.velocitypowered.api.command.RawCommand;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@RequiredArgsConstructor
public class VelocityRedisBridgeCommand implements RawCommand {

  private final String velocityId;

  @Override
  public void execute(Invocation invocation) {
    invocation
        .source()
        .sendMessage(
            Component.text("You are currently connected to Velocity whose ID is " + velocityId)
                .color(NamedTextColor.GREEN));
  }
}
