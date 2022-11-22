package net.azisaba.velocityredisbridge;

import com.velocitypowered.api.proxy.Player;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.redis.RedisKeys;
import net.azisaba.velocityredisbridge.redis.VRBPubSubHandler;
import net.azisaba.velocityredisbridge.util.PlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class VelocityRedisBridgeAPI {

  private final VelocityRedisBridge plugin;
  private final JedisPool jedisPool;

  private final VRBPubSubHandler pubSubHandler;

  /**
   * Transfers the specified player to the specified server. Even if the player is connected to a
   * different proxy, if the proxy is connected to the same redis server, it will be forwarded
   *
   * @param playerName the specified player name to transfer
   * @param serverName the server to transfer to
   */
  public void sendPlayer(@NotNull String playerName, @NotNull String serverName) {
    Optional<Player> player = plugin.getProxy().getPlayer(playerName);
    if (player.isPresent()) {
      plugin
          .getProxy()
          .getServer(serverName)
          .ifPresent(server -> player.get().createConnectionRequest(server).connect());
      return;
    }

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.SEND_SERVER_REQUEST.getKey(), playerName + ":" + serverName);
    }
  }

  /**
   * Transfers the specified player to the specified server. Even if the player is connected to a
   * different proxy, if the proxy is connected to the same redis server, it will be forwarded
   *
   * @param player     the specified player to transfer
   * @param serverName the server to transfer to
   */
  public void sendPlayer(@NotNull Player player, @NotNull String serverName) {
    if (player.getCurrentServer().isEmpty()) {
      // The player is offline.
      return;
    }

    plugin
        .getProxy()
        .getServer(serverName)
        .ifPresent(server -> player.createConnectionRequest(server).connect());
  }

  /**
   * Sends a specified message to all players in the velocity server network.
   *
   * @param message the message to send
   */
  public void sendMessageToAll(String message) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.SEND_MESSAGE_TO_ALL.getKey(), message);
    }
  }

  /**
   * Sends the specified message to the specified player. Even if the player is connected to a
   * different proxy, if the proxy is connected to the same redis server, it will be sent to the
   * player
   *
   * @param playerName the specified player name to send the message
   * @param message    the message to send
   */
  public void sendMessageToPlayer(String playerName, String message) {
    Optional<Player> player = plugin.getProxy().getPlayer(playerName);
    if (player.isPresent()) {
      player.get().sendMessage(Component.text(message));
      return;
    }

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.SEND_MESSAGE_TO_PLAYER.getKey(), playerName + ":" + message);
    }
  }

  /**
   * Sends a raw message to all players connected to all proxy servers connected to the same redis
   * server.
   *
   * @param messageRaw the raw message to send
   */
  public void sendRawMessageToAll(String messageRaw) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.SEND_RAW_MESSAGE_TO_ALL.getKey(), messageRaw);
    }
  }

  /**
   * Sends the specified raw message to the specified player. Even if the player is connected to a
   * different proxy, if the proxy is connected to the same redis server, it will be sent to the
   * player
   *
   * @param playerName the specified player name to send the message
   * @param messageRaw the raw message to send
   */
  public void sendRawMessageToPlayer(String playerName, String messageRaw) {
    Optional<Player> player = plugin.getProxy().getPlayer(playerName);
    if (player.isPresent()) {
      player.get().sendMessage(GsonComponentSerializer.gson().deserialize(messageRaw));
      return;
    }

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.SEND_RAW_MESSAGE_TO_PLAYER.getKey(), playerName + ":" + messageRaw);
    }
  }

  /**
   * Kick the player with the specified name. Even if the player is connected to a different proxy,
   * if the proxy is connected to the same redis server, it will be executed on the player
   *
   * @param playerName the specified player name to kick
   * @param reason     the reason to kick the player
   */
  public void kickPlayer(String playerName, String reason) {
    Optional<Player> player = plugin.getProxy().getPlayer(playerName);
    if (player.isPresent()) {
      player.get().disconnect(Component.text(reason));
      return;
    }

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.KICK_PLAYER.getKey(), playerName + ":" + reason);
    }
  }

  /**
   * Gets PlayerInfo of the player with the specified name
   *
   * @param playerName the specified player name
   * @return Returns the PlayerInfo of the player wrapped in an Optional
   */
  public Optional<PlayerInfo> getPlayerInfo(String playerName) {
    return Optional.ofNullable(plugin.getPlayerInfoHandler().get(playerName));
  }

  /**
   * Gets PlayerInfo of the player with the specified UUID
   *
   * @param uuid the specified player UUID
   * @return Returns the PlayerInfo of the player wrapped in an Optional
   */
  public Optional<PlayerInfo> getPlayerInfo(UUID uuid) {
    return Optional.ofNullable(plugin.getPlayerInfoHandler().get(uuid));
  }

  /**
   * Gets PlayerInfo of all players connected to the network
   *
   * @return Returns a Collection of the PlayerInfo of all players connected to the network
   */
  public Collection<PlayerInfo> getAllPlayerInfo() {
    return plugin.getPlayerInfoHandler().getAllPlayerInfo().values();
  }

  /**
   * Gets PlayerInfo of all players connected to the specified proxy
   *
   * @param proxyId the specified proxy id
   * @return Returns a Collection of the PlayerInfo of all players connected to the specified proxy
   */
  public Collection<PlayerInfo> getAllPlayerInfoInProxy(String proxyId) {
    return plugin.getPlayerInfoHandler().getAllPlayerInfoInProxy(proxyId);
  }

  /**
   * Gets PlayerInfo of all players connected to the specified server
   *
   * @param childServerName the specified server name
   * @return Returns a Collection of the PlayerInfo of all players connected to the specified server
   */
  public Collection<PlayerInfo> getAllPlayerInfoInChildServer(String childServerName) {
    return plugin.getPlayerInfoHandler().getAllPlayerInfoInChildServer(childServerName);
  }

  /**
   * Returns a handler instance for PubSub function that can send messages to other Velocity
   * instances
   *
   * @return Returns a handler instance for PubSub function that can send messages to other Velocity
   * instances
   */
  public VRBPubSubHandler getPubSubHandler() {
    return pubSubHandler;
  }
}
