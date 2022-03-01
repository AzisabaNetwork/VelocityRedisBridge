package net.azisaba.velocityredisbridge;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import lombok.Getter;
import net.azisaba.velocityredisbridge.cache.CacheContainer;
import net.azisaba.velocityredisbridge.config.VelocityRedisBridgeConfig;
import net.azisaba.velocityredisbridge.listener.BungeeCordPluginMessageReceiveListener;
import net.azisaba.velocityredisbridge.redis.IpDataHandler;
import net.azisaba.velocityredisbridge.redis.RedisMessageSubscriber;
import net.azisaba.velocityredisbridge.redis.RedisPlayerDataHandler;
import net.azisaba.velocityredisbridge.redis.ServerUniqueIdDefiner;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Plugin(
    id = "velocity-redis-bridge",
    name = "VelocityRedisBridge",
    version = "0.0.1-SNAPSHOT",
    url = "https://github.com/AzisabaNetwork/VelocityRedisBridge",
    description = "Plugin to help parallelization of Velocity",
    authors = {"Azisaba Network"})
@Getter
public class VelocityRedisBridge {

  @Getter private static VelocityRedisBridgeAPI api;

  private final ProxyServer proxy;
  private final Logger logger;

  private VelocityRedisBridgeConfig velocityRedisBridgeConfig;
  private CacheContainer cacheContainer;

  private RedisMessageSubscriber redisMessageSubscriber;

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

    RedisPlayerDataHandler playerHandler = new RedisPlayerDataHandler(this, jedisPool, uniqueId);
    IpDataHandler ipDataHandler = new IpDataHandler(this, jedisPool, uniqueId);

    cacheContainer = new CacheContainer(this, playerHandler, ipDataHandler);
    proxy.getEventManager().register(this, new BungeeCordPluginMessageReceiveListener(this));

    redisMessageSubscriber = new RedisMessageSubscriber(this, jedisPool);
    redisMessageSubscriber.subscribe();

    proxy
        .getScheduler()
        .buildTask(this, () -> cacheContainer.updateCache())
        .repeat(getVelocityRedisBridgeConfig().getCacheUpdateIntervalSeconds(), TimeUnit.SECONDS)
        .schedule();

    proxy
        .getScheduler()
        .buildTask(
            this,
            () -> {
              playerHandler.update();
              ipDataHandler.update();
            })
        .repeat(getVelocityRedisBridgeConfig().getCacheUpdateIntervalSeconds(), TimeUnit.SECONDS)
        .schedule();

    api = new VelocityRedisBridgeAPI(this, jedisPool);
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {}

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
}
