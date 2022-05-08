package net.azisaba.velocityredisbridge.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PubSubMessageData {

  private String key;
  private String message;
  private String publisherVelocityId;

}
