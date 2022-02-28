package net.azisaba.velocityredisbridge.cache;

import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.redis.IpDataFetcher;
import net.azisaba.velocityredisbridge.redis.RedisPlayerDataHandler;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class CacheContainer {

  private final VelocityRedisBridge plugin;
  private final JedisPool jedisPool;
  private final String uniqueId;

  private final ReentrantLock lock = new ReentrantLock();

  private RedisPlayerDataHandler redisPlayerDataHandler;
  private IpDataFetcher ipDataFetcher;

  private UnifiedPlayerCache unifiedPlayerCache;
  private UnifiedIPCache unifiedIPCache;

  public void updateCache() {
    lock.lock();
    try {
      if (redisPlayerDataHandler == null) {
        redisPlayerDataHandler = new RedisPlayerDataHandler(plugin, jedisPool, uniqueId);
      }
      unifiedPlayerCache = redisPlayerDataHandler.fetch();

      if (ipDataFetcher == null) {
        ipDataFetcher = new IpDataFetcher(jedisPool);
      }
      unifiedIPCache = ipDataFetcher.fetch();
    } finally {
      lock.unlock();
    }
  }

  public UnifiedPlayerCache getUnifiedPlayerCache() {
    lock.lock();
    try {
      return unifiedPlayerCache;
    } finally {
      lock.unlock();
    }
  }

  public UnifiedIPCache getUnifiedIPCache() {
    lock.lock();
    try {
      return unifiedIPCache;
    } finally {
      lock.unlock();
    }
  }
}
