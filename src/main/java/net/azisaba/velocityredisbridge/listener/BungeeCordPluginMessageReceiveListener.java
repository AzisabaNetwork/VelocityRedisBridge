package net.azisaba.velocityredisbridge.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.util.UserHostAndPort;

@RequiredArgsConstructor
public class BungeeCordPluginMessageReceiveListener {

  private final VelocityRedisBridge plugin;

  @Subscribe
  public void onPluginMessageReceive(PluginMessageEvent event) {
    if (!event.getIdentifier().getId().equals("BungeeCord")) {
      return;
    }

    ByteArrayDataInput receivedData = ByteStreams.newDataInput(event.getData());
    String subChannel = receivedData.readUTF();

    // No need to respond
    if (subChannel.equals("Connect")) {
      if (!(event.getSource() instanceof Player)) {
        return;
      }
      Player player = (Player) event.getSource();
      String server = receivedData.readUTF();
      VelocityRedisBridge.getApi().sendPlayer(player, server);
      return;
    } else if (subChannel.equals("ConnectOther")) {
      String playerName = receivedData.readUTF();
      String serverName = receivedData.readUTF();
      VelocityRedisBridge.getApi().sendPlayer(playerName, serverName);
      return;
    } else if (subChannel.equals("Message")) {
      String nameOrAll = receivedData.readUTF();
      String message = receivedData.readUTF();

      if (nameOrAll.equals("ALL")) {
        VelocityRedisBridge.getApi().sendMessageToAll(message);
      } else {
        VelocityRedisBridge.getApi().sendMessageToPlayer(nameOrAll, message);
      }
      return;
    } else if (subChannel.equals("MessageRaw")) {
      String nameOrAllRaw = receivedData.readUTF();
      String messageRaw = receivedData.readUTF();

      if (nameOrAllRaw.equals("ALL")) {
        VelocityRedisBridge.getApi().sendRawMessageToAll(messageRaw);
      } else {
        VelocityRedisBridge.getApi().sendRawMessageToPlayer(nameOrAllRaw, messageRaw);
      }
      return;
    } else if (subChannel.equals("KickPlayer")) {
      String playerName = receivedData.readUTF();
      String reason = receivedData.readUTF();
      VelocityRedisBridge.getApi().kickPlayer(playerName, reason);
      return;
    }

    ByteArrayDataOutput responseData = ByteStreams.newDataOutput();
    responseData.writeUTF(subChannel);

    if (subChannel.equals("IP")) {
      if (!(event.getSource() instanceof Player)) {
        return;
      }
      Player player = (Player) event.getSource();
      UserHostAndPort hostAndPort =
          plugin.getCacheContainer().getUnifiedIPCache().getIp(player.getUniqueId());

      responseData.writeUTF(hostAndPort.getHost());
      responseData.writeInt(hostAndPort.getPort());
    } else if (subChannel.equals("IPOther")) {
      String playerName = receivedData.readUTF();
      UUID uuid =
          plugin.getCacheContainer().getUnifiedPlayerCache().getUUIDFromPlayerName(playerName);
      if (uuid == null) {
        return;
      }
      UserHostAndPort hostAndPort = plugin.getCacheContainer().getUnifiedIPCache().getIp(uuid);

      responseData.writeUTF(hostAndPort.getHost());
      responseData.writeInt(hostAndPort.getPort());
    } else if (subChannel.equals("PlayerCount")) {
      String serverNameOrAll = receivedData.readUTF();
      if (serverNameOrAll.equals("ALL")) {
        responseData.writeInt(plugin.getCacheContainer().getUnifiedPlayerCache().getPlayerCount());
      } else {
        responseData.writeInt(
            plugin.getCacheContainer().getUnifiedPlayerCache().getPlayerCount(serverNameOrAll));
      }
    } else if (subChannel.equals("PlayerList")) {
      String serverNameOrAll = receivedData.readUTF();
      if (serverNameOrAll.equals("ALL")) {
        String playerList =
            String.join(
                ", ", plugin.getCacheContainer().getUnifiedPlayerCache().getAllPlayers().values());

        responseData.writeUTF(playerList);
      } else {
        Set<UUID> playerUUIDs =
            plugin
                .getCacheContainer()
                .getUnifiedPlayerCache()
                .getChildServerMap()
                .getOrDefault(serverNameOrAll, Collections.emptySet());

        Map<UUID, String> nameMap =
            plugin.getCacheContainer().getUnifiedPlayerCache().getAllPlayers();
        String playerList =
            nameMap.keySet().stream()
                .filter(playerUUIDs::contains)
                .map(nameMap::get)
                .collect(Collectors.joining(", "));

        responseData.writeUTF(playerList);
      }
    } else if (subChannel.equals("GetServers")) {
      String serverListStr =
          plugin.getProxy().getAllServers().stream()
              .map(server -> server.getServerInfo().getName())
              .collect(Collectors.joining(", "));

      responseData.writeUTF(serverListStr);
    } else if (subChannel.equals("GetServer")) {
      if (!(event.getSource() instanceof Player)) {
        return;
      }
      Player player = (Player) event.getSource();
      Optional<ServerConnection> serverConnection = player.getCurrentServer();
      if (!serverConnection.isPresent()) {
        return;
      }

      responseData.writeUTF(serverConnection.get().getServerInfo().getName());
    } else if (subChannel.equals("Forward")) {
      // TODO 後回し
    } else if (subChannel.equals("ForwardToPlayer")) {
      // TODO 後回し
    } else if (subChannel.equals("UUID")) {
      if (!(event.getSource() instanceof Player)) {
        return;
      }
      Player player = (Player) event.getSource();

      responseData.writeUTF(player.getUniqueId().toString());
    } else if (subChannel.equals("UUIDOther")) {
      String playerName = receivedData.readUTF();
      UUID uuid =
          plugin.getCacheContainer().getUnifiedPlayerCache().getUUIDFromPlayerName(playerName);

      responseData.writeUTF(uuid.toString());
    } else if (subChannel.equals("ServerIP")) {
      String serverName = receivedData.readUTF();
      Optional<RegisteredServer> server = plugin.getProxy().getServer(serverName);

      if (!server.isPresent()) {
        return;
      }

      String hostName = server.get().getServerInfo().getAddress().getHostString();
      int port = server.get().getServerInfo().getAddress().getPort();

      responseData.writeUTF(hostName);
      responseData.writeShort(port);
    } else {
      // Unknown sub channel
      return;
    }

    event
        .getTarget()
        .sendPluginMessage(new LegacyChannelIdentifier("BungeeCord"), responseData.toByteArray());
  }
}
