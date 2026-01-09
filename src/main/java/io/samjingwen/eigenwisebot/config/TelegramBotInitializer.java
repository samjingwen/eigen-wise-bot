package io.samjingwen.eigenwisebot.config;


import io.samjingwen.eigenwisebot.core.EigenWiseBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@RequiredArgsConstructor
public class TelegramBotInitializer implements InitializingBean {

  private final TelegramBotsLongPollingApplication telegramBotsApplication;
  private final EigenWiseBot eigenWiseBot;

  @Override
  public void afterPropertiesSet() {
    try {
      telegramBotsApplication.registerBot(eigenWiseBot.getToken(), eigenWiseBot);
    } catch (TelegramApiException e) {
      throw new RuntimeException(e);
    }
  }
}
