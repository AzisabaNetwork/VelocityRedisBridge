package net.azisaba.velocityredisbridge.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.HostAndPort;

@Getter
@RequiredArgsConstructor
public class VelocityRedisBridgeConfig {

  private final VelocityRedisBridge plugin;

  private static final String CONFIG_FILE_PATH = "./plugins/VelocityRedisBridge/config.yml";

  private HostAndPort redisConnectionInfo;
  @Nullable private String redisUserName;
  @Nullable private String redisPassword;

  private long cacheUpdateIntervalSeconds;
  private long redisCacheExpireSeconds;

  public void load() throws IOException {
    File file = new File(CONFIG_FILE_PATH);
    // save if not exist
    saveDefaultConfig(file);

    Yaml yaml = new Yaml();
    Map<String, Object> data = yaml.load(new FileReader(file));

    Map<String, Object> redis = dig(data, "redis");
    String hostname = (String) redis.get("hostname");
    int port = (Integer) redis.get("port");
    redisConnectionInfo = new HostAndPort(hostname, port);
    redisUserName = (String) redis.get("username");
    redisPassword = (String) redis.get("password");

    if (Objects.equals(redisUserName, "")) {
      redisUserName = null;
    }
    if (Objects.equals(redisPassword, "")) {
      redisPassword = null;
    }

    if (data.get("cache-update-interval-seconds") instanceof Integer) {
      cacheUpdateIntervalSeconds = (Integer) data.get("cache-update-interval-seconds");
    } else if (data.get("cache-update-interval-seconds") instanceof Long) {
      cacheUpdateIntervalSeconds = (Long) data.get("cache-update-interval-seconds");
    }

    if (data.get("redis-cache-expire-seconds") instanceof Integer) {
      redisCacheExpireSeconds = (Integer) data.get("redis-cache-expire-seconds");
    } else if (data.get("redis-cache-expire-seconds") instanceof Long) {
      redisCacheExpireSeconds = (Long) data.get("redis-cache-expire-seconds");
    }
  }

  private void saveDefaultConfig(File configFilePath) throws IOException {
    if (configFilePath.exists()) {
      return;
    }

    InputStream is = plugin.getClass().getClassLoader().getResourceAsStream("config.yml");
    if (is == null) {
      throw new IllegalStateException("Failed to load config.yml from resource.");
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    Files.createDirectories(configFilePath.getParentFile().toPath());

    byte[] byteStr =
        reader
            .lines()
            .collect(Collectors.joining(System.lineSeparator()))
            .getBytes(StandardCharsets.UTF_8);

    Files.write(configFilePath.toPath(), byteStr);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> dig(Map<String, Object> data, String key) throws IOException {
    Object o = data.get(key);
    if (!(o instanceof Map)) {
      throw new IOException("Failed to get new map from key: " + key);
    }

    return (Map<String, Object>) o;
  }
}
