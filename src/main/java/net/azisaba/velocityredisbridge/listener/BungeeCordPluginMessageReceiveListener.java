package net.azisaba.velocityredisbridge.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.util.PlayerInfo;

@RequiredArgsConstructor
public class BungeeCordPluginMessageReceiveListener {

  private final VelocityRedisBridge plugin;

  private final LegacyChannelIdentifier legacyBungeeChannel;
  private final MinecraftChannelIdentifier modernBungeeChannel;

  @Subscribe(order = PostOrder.FIRST)
  public void onPluginMessageReceive(PluginMessageEvent event) {
    if (!event.getIdentifier().equals(legacyBungeeChannel)
        && !event.getIdentifier().equals(modernBungeeChannel)) {
      return;
    }

    if (!(event.getSource() instanceof ServerConnection)) {
      return;
    }

    ServerConnection connection = (ServerConnection) event.getSource();

    ByteArrayDataInput receivedData = ByteStreams.newDataInput(event.getData());
    String subChannel = receivedData.readUTF();

    // No need to respond
    if (subChannel.equals("Connect")) {
      Player player = connection.getPlayer();
      String server = receivedData.readUTF();
      VelocityRedisBridge.getApi().sendPlayer(player, server);
      event.setResult(ForwardResult.handled());
      return;
    } else if (subChannel.equals("ConnectOther")) {
      String playerName = receivedData.readUTF();
      String serverName = receivedData.readUTF();
      VelocityRedisBridge.getApi().sendPlayer(playerName, serverName);
      event.setResult(ForwardResult.handled());
      return;
    } else if (subChannel.equals("Message")) {
      String nameOrAll = receivedData.readUTF();
      String message = receivedData.readUTF();

      if (nameOrAll.equals("ALL")) {
        VelocityRedisBridge.getApi().sendMessageToAll(message);
      } else {
        VelocityRedisBridge.getApi().sendMessageToPlayer(nameOrAll, message);
      }
      event.setResult(ForwardResult.handled());
      return;
    } else if (subChannel.equals("MessageRaw")) {
      String nameOrAllRaw = receivedData.readUTF();
      String messageRaw = receivedData.readUTF();

      if (nameOrAllRaw.equals("ALL")) {
        VelocityRedisBridge.getApi().sendRawMessageToAll(messageRaw);
      } else {
        VelocityRedisBridge.getApi().sendRawMessageToPlayer(nameOrAllRaw, messageRaw);
      }
      event.setResult(ForwardResult.handled());
      return;
    } else if (subChannel.equals("KickPlayer")) {
      String playerName = receivedData.readUTF();
      String reason = receivedData.readUTF();
      VelocityRedisBridge.getApi().kickPlayer(playerName, reason);
      event.setResult(ForwardResult.handled());
      return;
    }

    ByteArrayDataOutput responseData = ByteStreams.newDataOutput();
    responseData.writeUTF(subChannel);

    if (subChannel.equals("IP")) {
      Player player = connection.getPlayer();
      PlayerInfo info = plugin.getPlayerInfoHandler().get(player.getUniqueId());
      String hostName = info.getHostName();
      int port = info.getPort();

      responseData.writeUTF(hostName);
      responseData.writeInt(port);
    } else if (subChannel.equals("IPOther")) {
      String playerName = receivedData.readUTF();
      PlayerInfo info = plugin.getPlayerInfoHandler().get(playerName);
      if (info == null) {
        return;
      }

      responseData.writeUTF(info.getUsername());
      responseData.writeUTF(info.getHostName());
      responseData.writeInt(info.getPort());
    } else if (subChannel.equals("PlayerCount")) {
      String serverNameOrAll = receivedData.readUTF();
      if (serverNameOrAll.equals("ALL")) {
        responseData.writeUTF("ALL");
        responseData.writeInt(plugin.getPlayerInfoHandler().getAllPlayersCount());
      } else {
        responseData.writeUTF(serverNameOrAll);
        responseData.writeInt(plugin.getPlayerInfoHandler().getPlayersCount(serverNameOrAll));
      }
    } else if (subChannel.equals("PlayerList")) {
      String serverNameOrAll = receivedData.readUTF();
      String playerList;
      if (serverNameOrAll.equals("ALL")) {
        playerList =
            plugin.getPlayerInfoHandler().getAllPlayerInfo().values().stream()
                .map(PlayerInfo::getUsername)
                .collect(Collectors.joining(", "));

      } else {
        playerList =
            plugin.getPlayerInfoHandler().getAllPlayerInfoInChildServer(serverNameOrAll).stream()
                .map(PlayerInfo::getUsername)
                .collect(Collectors.joining(", "));
      }
      responseData.writeUTF(serverNameOrAll);
      responseData.writeUTF(playerList);
    } else if (subChannel.equals("GetServers")) {
      String serverListStr =
          plugin.getProxy().getAllServers().stream()
              .map(server -> server.getServerInfo().getName())
              .collect(Collectors.joining(", "));

      responseData.writeUTF(serverListStr);
    } else if (subChannel.equals("GetServer")) {
      Player player = connection.getPlayer();
      Optional<ServerConnection> serverConnection = player.getCurrentServer();
      if (!serverConnection.isPresent()) {
        return;
      }

      responseData.writeUTF(serverConnection.get().getServerInfo().getName());
    } else if (subChannel.equals("Forward")) {
      String target = receivedData.readUTF();
      byte[] toForward = prepareForwardMessage(receivedData);

      if (target.equals("ALL")) {
        for (RegisteredServer rs : plugin.getProxy().getAllServers()) {
          rs.sendPluginMessage(event.getIdentifier(), toForward);
        }
      } else {
        plugin.getProxy().getServer(target)
            .ifPresent(conn -> conn.sendPluginMessage(event.getIdentifier(), toForward));
      }
    } else if (subChannel.equals("ForwardToPlayer")) {
      String playerName = receivedData.readUTF();
      plugin.getProxy().getPlayer(playerName)
          .flatMap(Player::getCurrentServer)
          .ifPresent(server -> server.sendPluginMessage(event.getIdentifier(),
              prepareForwardMessage(receivedData)));
    } else if (subChannel.equals("UUID")) {
      Player player = connection.getPlayer();
      responseData.writeUTF(player.getUniqueId().toString());
    } else if (subChannel.equals("UUIDOther")) {
      String playerName = receivedData.readUTF();
      PlayerInfo info = plugin.getPlayerInfoHandler().get(playerName);
      if (info == null) {
        return;
      }
      responseData.writeUTF(info.getUsername());
      responseData.writeUTF(info.getUuid().toString());
    } else if (subChannel.equals("ServerIP")) {
      String serverName = receivedData.readUTF();
      Optional<RegisteredServer> server = plugin.getProxy().getServer(serverName);

      if (!server.isPresent()) {
        return;
      }

      String hostName = server.get().getServerInfo().getAddress().getHostString();
      int port = server.get().getServerInfo().getAddress().getPort();

      responseData.writeUTF(server.get().getServerInfo().getName());
      responseData.writeUTF(hostName);
      responseData.writeShort(port);
    } else {
      // Unknown sub channel
      return;
    }

    connection.sendPluginMessage(event.getIdentifier(), responseData.toByteArray());
    event.setResult(ForwardResult.handled());
  }

  private byte[] prepareForwardMessage(ByteArrayDataInput in) {
    String channel = in.readUTF();
    short messageLength = in.readShort();
    byte[] message = new byte[messageLength];
    in.readFully(message);

    ByteArrayDataOutput forwarded = ByteStreams.newDataOutput();
    forwarded.writeUTF(channel);
    forwarded.writeShort(messageLength);
    forwarded.write(message);
    return forwarded.toByteArray();
  }
}
