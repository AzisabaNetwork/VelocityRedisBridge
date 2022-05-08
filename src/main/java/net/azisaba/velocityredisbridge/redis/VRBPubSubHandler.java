package net.azisaba.velocityredisbridge.redis;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.util.PubSubMessageData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class VRBPubSubHandler {

  private final JedisPool jedisPool;
  private final String velocityId;

  private final Gson gson = new Gson();
  private final HashMap<String, List<Consumer<String>>> registeredFunctionList = new HashMap<>();

  /**
   * Registers the specified function to the specified channel.
   *
   * @param key      the channel to register to
   * @param consumer the function to register
   */
  public void register(String key, Consumer<String> consumer) {
    registeredFunctionList.computeIfAbsent(key, k -> new ArrayList<>()).add(consumer);
  }

  /**
   * Unregisters the specified function to the specified channel.
   *
   * @param key the channel to unregister to
   */
  public void unregisterAll(String key) {
    registeredFunctionList.remove(key);
  }

  /**
   * Gets the registered function list.
   *
   * @return Returns the registered function list
   */
  public List<Consumer<String>> getRegisteredFunctionList(String key) {
    return registeredFunctionList.getOrDefault(key, Collections.emptyList());
  }

  /**
   * Publishes the specified message to the specified channel.
   *
   * @param key        the channel to publish to
   * @param message    the message to publish
   * @param ignoreSelf if true, the message will be ignored in the proxy instance that calls this
   */
  public void publish(String key, String message, boolean ignoreSelf) {
    PubSubMessageData data = new PubSubMessageData(key, message, velocityId);
    if (!ignoreSelf) {
      data.setPublisherVelocityId(null);
    }

    String jsonData = gson.toJson(data);

    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(RedisKeys.PUB_SUB_KEY.getKey(), jsonData);
    }
  }

  /**
   * Publishes the specified message to the specified channel.
   *
   * @param key     the channel to publish to
   * @param message the message to publish
   */
  public void publish(String key, String message) {
    publish(key, message, false);
  }

  protected void execute(PubSubMessageData data) {
    if (!registeredFunctionList.containsKey(data.getKey())) {
      return;
    }
    if (Objects.equals(data.getPublisherVelocityId(), velocityId)) {
      return;
    }

    registeredFunctionList.get(data.getKey()).forEach(consumer -> {
      try {
        consumer.accept(data.getMessage());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
