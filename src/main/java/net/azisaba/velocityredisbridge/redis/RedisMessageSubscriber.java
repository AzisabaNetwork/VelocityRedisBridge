package net.azisaba.velocityredisbridge.redis;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.util.PlayerInfo;
import net.azisaba.velocityredisbridge.util.PubSubMessageData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public class RedisMessageSubscriber {

  private final VelocityRedisBridge plugin;
  private final JedisPool jedisPool;
  private final Gson gson = new Gson();

  @Getter private final ExecutorService executorService = Executors.newFixedThreadPool(1);

  public void subscribe() {
    JedisPubSub subscriber =
        new JedisPubSub() {
          @Override
          public void onPMessage(String pattern, String channel, String message) {
            // Send request
            if (channel.equals(RedisKeys.SEND_SERVER_REQUEST.toString())) {
              String playerName = message.split(":")[0];
              String serverName = message.split(":")[1];

              Optional<Player> player = plugin.getProxy().getPlayer(playerName);
              if (!player.isPresent()) {
                return;
              }
              Optional<RegisteredServer> server = plugin.getProxy().getServer(serverName);
              if (!server.isPresent()) {
                return;
              }

              player.get().createConnectionRequest(server.get()).connect();
            } else if (channel.equals(RedisKeys.SEND_MESSAGE_TO_ALL.getKey())) {
              plugin
                  .getProxy()
                  .getAllPlayers()
                  .forEach(player -> player.sendMessage(Component.text(message)));
            } else if (channel.equals(RedisKeys.SEND_MESSAGE_TO_PLAYER.getKey())) {
              String playerName = message.split(":")[0];
              String messageText = message.substring(playerName.length() + 1);

              plugin
                  .getProxy()
                  .getPlayer(playerName)
                  .ifPresent(player -> player.sendMessage(Component.text(messageText)));
            } else if (channel.equals(RedisKeys.SEND_RAW_MESSAGE_TO_ALL.getKey())) {
              plugin
                  .getProxy()
                  .getAllPlayers()
                  .forEach(
                      player ->
                          player.sendMessage(GsonComponentSerializer.gson().deserialize(message)));
            } else if (channel.equals(RedisKeys.SEND_RAW_MESSAGE_TO_PLAYER.getKey())) {
              String playerName = message.split(":")[0];
              String messageText = message.substring(playerName.length() + 1);

              plugin
                  .getProxy()
                  .getPlayer(playerName)
                  .ifPresent(
                      player ->
                          player.sendMessage(
                              GsonComponentSerializer.gson().deserialize(messageText)));
            } else if (channel.equals(RedisKeys.KICK_PLAYER.getKey())) {
              String playerName = message.split(":")[0];
              String reason = message.substring(playerName.length() + 1);

              plugin
                  .getProxy()
                  .getPlayer(playerName)
                  .ifPresent(player -> player.disconnect(Component.text(reason)));
            } else if (channel.equals(RedisKeys.INFO_UPDATE_NOTIFY.getKey())) {
              PlayerInfo info = gson.fromJson(message, PlayerInfo.class);
              if (info != null) {
                plugin.getPlayerInfoHandler().receivedUpdateNotify(info);
              }
            } else if (channel.equals(RedisKeys.PUB_SUB_KEY.getKey())) {
              try {
                plugin.getVrbPubSubHandler()
                    .execute(gson.fromJson(message, PubSubMessageData.class));
              } catch (JsonSyntaxException e) {
                e.printStackTrace();
              }
            }
          }
        };

    executorService.submit(
        () -> {
          try (Jedis jedis = jedisPool.getResource()) {
            jedis.psubscribe(subscriber, RedisKeys.ALL_KEY_PREFIX + ":*");
          }
        });

    for (int i = 0; i < 10000; i++) {
      executorService.submit(
          () -> {
            try {
              Thread.sleep(3000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }

            try (Jedis jedis = jedisPool.getResource()) {
              jedis.psubscribe(subscriber, RedisKeys.ALL_KEY_PREFIX + ":*");
            }
          });
    }
  }
}
