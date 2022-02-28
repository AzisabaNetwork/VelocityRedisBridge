package net.azisaba.velocityredisbridge.redis;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public class MessagePubSubHandler {

  private final VelocityRedisBridge plugin;
  private final JedisPool jedisPool;

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
            } else if (channel.equals(RedisKeys.SEND_MESSAGE_TO_ALL.toString())) {
              plugin
                  .getProxy()
                  .getAllPlayers()
                  .forEach(player -> player.sendMessage(Component.text(message)));
            } else if (channel.equals(RedisKeys.SEND_MESSAGE_TO_PLAYER.toString())) {
              String playerName = message.split(":")[0];
              String messageText = message.substring(playerName.length() + 1);

              plugin
                  .getProxy()
                  .getPlayer(playerName)
                  .ifPresent(player -> player.sendMessage(Component.text(messageText)));
            } else if (channel.equals(RedisKeys.SEND_RAW_MESSAGE_TO_ALL.toString())) {
              plugin
                  .getProxy()
                  .getAllPlayers()
                  .forEach(
                      player ->
                          player.sendMessage(GsonComponentSerializer.gson().deserialize(message)));
            } else if (channel.equals(RedisKeys.SEND_RAW_MESSAGE_TO_PLAYER.toString())) {
              String playerName = message.split(":")[0];
              String messageText = message.substring(playerName.length() + 1);

              plugin
                  .getProxy()
                  .getPlayer(playerName)
                  .ifPresent(
                      player ->
                          player.sendMessage(GsonComponentSerializer.gson().deserialize(messageText))
                  );
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

  public void publishSendServerRequest(String playerName, String serverName) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.SEND_SERVER_REQUEST.getKey(), playerName + ":" + serverName);
    }
  }
}
