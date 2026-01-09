package io.samjingwen.eigenwisebot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AppProperties {

  @Value("${TELEGRAM_BOT_TOKEN}")
  private String token;

}
