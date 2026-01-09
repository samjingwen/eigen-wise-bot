package io.samjingwen.eigenwisebot.config;

import io.samjingwen.eigenwisebot.core.EigenWiseBot;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class AppConfig {

  @Bean
  public TelegramClient telegramClient(AppProperties appProperties) {
    return new OkHttpTelegramClient(appProperties.getToken());
  }

  @Bean
  @ConditionalOnMissingBean(TelegramBotsLongPollingApplication.class)
  public TelegramBotsLongPollingApplication telegramBotsApplication() {
    return new TelegramBotsLongPollingApplication();
  }

  @Bean
  @ConditionalOnMissingBean
  public TelegramBotInitializer telegramBotInitializer(
      TelegramClient telegramClient,
      AppProperties appProperties,
      TelegramBotsLongPollingApplication telegramBotsApplication,
      ObjectProvider<EigenWiseBot> eigenWiseBot) {
    return new TelegramBotInitializer(
        telegramBotsApplication,
        eigenWiseBot.getIfAvailable(() -> new EigenWiseBot(appProperties, telegramClient)));
  }
}
