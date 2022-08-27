package net.azisaba.velocityredisbridge;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import lombok.Getter;
import net.azisaba.velocityredisbridge.command.VelocityRedisBridgeCommand;
import net.azisaba.velocityredisbridge.config.VelocityRedisBridgeConfig;
import net.azisaba.velocityredisbridge.listener.BungeeCordPluginMessageReceiveListener;
import net.azisaba.velocityredisbridge.listener.PlayerJoinQuitListener;
import net.azisaba.velocityredisbridge.listener.ServerListPingListener;
import net.azisaba.velocityredisbridge.redis.PlayerInfoHandler;
import net.azisaba.velocityredisbridge.redis.RedisKeys;
import net.azisaba.velocityredisbridge.redis.RedisMessageSubscriber;
import net.azisaba.velocityredisbridge.redis.ServerUniqueIdDefiner;
import net.azisaba.velocityredisbridge.redis.VRBPubSubHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Plugin(
    id = "velocity-redis-bridge",
    name = "VelocityRedisBridge",
    version = "2.0.0",
    url = "https://github.com/AzisabaNetwork/VelocityRedisBridge",
    description = "Plugin to help parallelization of Velocity",
    authors = {"Azisaba Network"})
@Getter
public class VelocityRedisBridge {

  @Getter
  private static VelocityRedisBridgeAPI api;

  private final ProxyServer proxy;
  private final Logger logger;

  private VelocityRedisBridgeConfig velocityRedisBridgeConfig;
  private PlayerInfoHandler playerInfoHandler;

  private RedisMessageSubscriber redisMessageSubscriber;
  private VRBPubSubHandler vrbPubSubHandler;

  private static final LegacyChannelIdentifier LEGACY_BUNGEE_CHANNEL =
      new LegacyChannelIdentifier("BungeeCord");
  private static final MinecraftChannelIdentifier MODERN_BUNGEE_CHANNEL =
      MinecraftChannelIdentifier.create("bungeecord", "main");

  @Inject
  public VelocityRedisBridge(ProxyServer server, Logger logger) {
    this.proxy = server;
    this.logger = logger;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    velocityRedisBridgeConfig = new VelocityRedisBridgeConfig(this);
    try {
      velocityRedisBridgeConfig.load();
    } catch (IOException ex) {
      logger.warning("Failed to load config.yml");
      return;
    }

    JedisPool jedisPool = createJedisPool(velocityRedisBridgeConfig);

    String uniqueId = new ServerUniqueIdDefiner(jedisPool).define();
    runKeepServerIdTask(jedisPool, uniqueId);

    playerInfoHandler = new PlayerInfoHandler(this, jedisPool, uniqueId);
    proxy
        .getEventManager()
        .register(
            this,
            new BungeeCordPluginMessageReceiveListener(
                this, LEGACY_BUNGEE_CHANNEL, MODERN_BUNGEE_CHANNEL));
    proxy.getEventManager().register(this, new PlayerJoinQuitListener(this, uniqueId));
    proxy.getEventManager().register(this, new ServerListPingListener(this));

    proxy
        .getCommandManager()
        .register(
            proxy.getCommandManager().metaBuilder("velocityredisbridge").aliases("vrb").build(),
            new VelocityRedisBridgeCommand(uniqueId));

    proxy.getChannelRegistrar().register(new LegacyChannelIdentifier("BungeeCord"));

    vrbPubSubHandler = new VRBPubSubHandler(jedisPool, uniqueId);

    redisMessageSubscriber = new RedisMessageSubscriber(this, jedisPool);
    redisMessageSubscriber.subscribe();

    proxy
        .getScheduler()
        .buildTask(this, () -> playerInfoHandler.updateAllRedisKeys())
        .repeat(velocityRedisBridgeConfig.getRedisCacheExpireSeconds() / 2, TimeUnit.SECONDS)
        .schedule();

    proxy
        .getScheduler()
        .buildTask(this, () -> playerInfoHandler.fetch())
        .repeat(getVelocityRedisBridgeConfig().getCacheUpdateIntervalSeconds(), TimeUnit.SECONDS)
        .schedule();

    api = new VelocityRedisBridgeAPI(this, jedisPool, vrbPubSubHandler);
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {

  }

  private JedisPool createJedisPool(VelocityRedisBridgeConfig config) {
    String hostName = config.getRedisConnectionInfo().getHost();
    int port = config.getRedisConnectionInfo().getPort();

    if (config.getRedisUserName() != null && config.getRedisPassword() != null) {
      return new JedisPool(hostName, port, config.getRedisUserName(), config.getRedisPassword());
    } else if (config.getRedisPassword() != null) {
      return new JedisPool(new JedisPoolConfig(), hostName, port, 3000, config.getRedisPassword());
    } else if (config.getRedisUserName() != null && config.getRedisPassword() == null) {
      throw new IllegalArgumentException(
          "Redis password cannot be null if redis username is not null");
    } else {
      return new JedisPool(new JedisPoolConfig(), hostName, port);
    }
  }

  private void runKeepServerIdTask(JedisPool jedisPool, String serverId) {
    proxy
        .getScheduler()
        .buildTask(
            this,
            () -> {
              try (Jedis jedis = jedisPool.getResource()) {
                jedis.expire(RedisKeys.SERVER_ID_PREFIX + ":" + serverId, 600);
              }
            })
        .repeat(5, TimeUnit.MINUTES)
        .schedule();
  }
}
