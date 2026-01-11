package io.samjingwen.eigenwisebot.core;

import io.samjingwen.eigenwisebot.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.input.InputPollOption;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EigenWiseBot implements LongPollingSingleThreadUpdateConsumer {

  private final AppProperties appProperties;
  private final TelegramClient telegramClient;
  private final ObjectMapper objectMapper;

  private List<Quiz> quizzes = new ArrayList<>();

  public String getToken() {
    return appProperties.getToken();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationEvent() {
    this.quizzes = loadQuestionsFromResources();
    log.info("Successfully loaded {} questions from resources.", quizzes.size());
  }

  @Override
  public void consume(Update update) {
    if (quizzes.isEmpty()) {
      return;
    }

    if (update.hasMessage() && update.getMessage().hasText()) {
      long chatId = update.getMessage().getChatId();
      String message = update.getMessage().getText();

      if (message.equalsIgnoreCase("/start")) {
        Quiz quiz = quizzes.getFirst();
        sendImage(chatId, quiz.id());
        sendPoll(chatId, quiz);
      }
    }
  }

  private List<Quiz> loadQuestionsFromResources() {
    List<Quiz> questions = new ArrayList<>();
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath:quiz/*.json");

      for (Resource resource : resources) {
        try {
          Quiz question = objectMapper.readValue(resource.getInputStream(), Quiz.class);
          questions.add(question);
          log.info("Loaded question from: {}", resource.getFilename());
        } catch (IOException e) {
          log.error("Failed to load question from: {}", resource.getFilename(), e);
        }
      }
    } catch (IOException e) {
      log.error("Failed to load questions from resources", e);
    }
    return questions;
  }

  private void sendPoll(Long chatId, Quiz quiz) {
    SendPoll sendPoll =
        SendPoll.builder()
            .chatId(chatId)
            .question(quiz.question())
            .options(
                quiz.options().stream()
                    .map(opt -> InputPollOption.builder().text(opt).build())
                    .collect(Collectors.toList()))
            .isAnonymous(false)
            .type("quiz")
            .correctOptionId(quiz.correctOptionId())
            .explanation(quiz.explanation())
            .build();

    try {
      telegramClient.execute(sendPoll);
      log.info("Sent poll to chat ID: {}", chatId);
    } catch (TelegramApiException e) {
      log.error("Failed to send poll to chat ID: {}", chatId, e);
    }
  }

  private void sendImage(Long chatId, int id) {
    String resourcePath = String.format("quiz/img_%d.jpg", id);
    try {
      ClassPathResource imgFile = new ClassPathResource(resourcePath);

      try (InputStream is = imgFile.getInputStream()) {
        SendPhoto photo =
            SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(is, imgFile.getFilename()))
                .caption(String.format("Question #%d", id))
                .build();

        telegramClient.execute(photo);
        log.info("Sent photo from classpath: {} to chat ID: {}", resourcePath, chatId);
      }
    } catch (IOException | TelegramApiException e) {
      log.error("Failed to send photo from classpath: {}", resourcePath, e);
    }
  }
}
