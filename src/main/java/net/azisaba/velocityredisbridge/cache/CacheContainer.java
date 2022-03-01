package net.azisaba.velocityredisbridge.cache;

import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.azisaba.velocityredisbridge.redis.IpDataHandler;
import net.azisaba.velocityredisbridge.redis.RedisPlayerDataHandler;

@RequiredArgsConstructor
public class CacheContainer {

  private final VelocityRedisBridge plugin;
  private final RedisPlayerDataHandler redisPlayerDataHandler;
  private final IpDataHandler ipDataHandler;

  private final ReentrantLock lock = new ReentrantLock();

  private UnifiedPlayerCache unifiedPlayerCache;
  private UnifiedIPCache unifiedIPCache;

  public void updateCache() {
    lock.lock();
    try {
      unifiedPlayerCache = redisPlayerDataHandler.fetch();
      unifiedIPCache = ipDataHandler.fetch();
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
