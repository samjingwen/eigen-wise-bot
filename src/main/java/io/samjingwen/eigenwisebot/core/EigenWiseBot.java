package io.samjingwen.eigenwisebot.core;

import io.samjingwen.eigenwisebot.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
  private final ChatManager chatManager;
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
      Long chatId = update.getMessage().getChatId();
      Integer threadId = update.getMessage().getMessageThreadId();
      ChatTopic chatTopic = new ChatTopic(chatId, threadId);

      String message = update.getMessage().getText().trim();

      if (message.startsWith("/ewb")) {
        String[] parts = message.split("\\s+", 2);
        String command = (parts.length > 1) ? parts[1].toLowerCase() : "";
        switch (command) {
          case "random" -> {
            sendRandomQuiz(chatTopic, false);
          }
          case "register" -> {
            chatManager.addChat(chatTopic);
            sendMessage(chatTopic, "You are now registered for daily quizzes.");
          }
          case "unregister" -> {
            chatManager.removeChat(chatTopic);
            sendMessage(chatTopic, "You are now unregistered for daily quizzes.");
          }
          case "" -> sendMessage(chatTopic, "Please provide a command. Try '/eiw random'");
          default -> sendMessage(chatTopic, "Unknown command. Try '/eiw random'");
        }
      }
    }
  }

  @Scheduled(cron = "0 0 21 * * *")
  public void sendDailyQuiz() {
    log.info("Starting scheduled daily quiz for all registered chats.");
    for (ChatTopic chatTopic : chatManager.getChats()) {
      sendRandomQuiz(chatTopic, true);
    }
  }

  private void sendRandomQuiz(ChatTopic chatTopic, boolean isDaily) {
    if (quizzes.isEmpty()) return;

    int randomIndex = ThreadLocalRandom.current().nextInt(quizzes.size());
    Quiz quiz = quizzes.get(randomIndex);

    String message =
        isDaily
            ? "Here's your daily quiz on Advanced Linear Algebra:"
            : "Here's your random quiz on Advanced Linear Algebra:";

    sendMessage(chatTopic, message);
    sendImage(chatTopic, quiz.id());
    sendPoll(chatTopic, quiz);
  }

  private List<Quiz> loadQuestionsFromResources() {
    List<Quiz> questions = new ArrayList<>();
    for (int i = 0; i <= 5; i++) {
      String resourcePath = String.format("quiz/%d/poll.json", i);
      try {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (resource.exists()) {
          try (InputStream is = resource.getInputStream()) {
            Quiz question = objectMapper.readValue(is, Quiz.class);
            questions.add(question);
            log.info("Loaded question from: {}", resourcePath);
          }
        } else {
          log.warn("Question file not found: {}", resourcePath);
        }
      } catch (IOException e) {
        log.error("Failed to load question from: {}", resourcePath, e);
      }
    }
    return questions;
  }

  private void sendPoll(ChatTopic chatTopic, Quiz quiz) {
    SendPoll sendPoll =
        SendPoll.builder()
            .chatId(chatTopic.chatId())
            .messageThreadId(chatTopic.threadId())
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
      log.info("Sent poll to chat ID: {}, thread ID: {}", chatTopic.chatId(), chatTopic.threadId());
    } catch (TelegramApiException e) {
      log.error(
          "Failed to send poll to chat ID: {}, thread ID: {}",
          chatTopic.chatId(),
          chatTopic.threadId(),
          e);
    }
  }

  private void sendImage(ChatTopic chatTopic, int id) {
    String resourcePath = String.format("quiz/%d/img.jpg", id);
    try {
      ClassPathResource imgFile = new ClassPathResource(resourcePath);

      try (InputStream is = imgFile.getInputStream()) {
        SendPhoto photo =
            SendPhoto.builder()
                .chatId(chatTopic.chatId())
                .messageThreadId(chatTopic.threadId())
                .photo(new InputFile(is, imgFile.getFilename()))
                .caption(String.format("Question #%d", id))
                .build();

        telegramClient.execute(photo);
        log.info(
            "Sent photo from classpath: {} to chat ID: {}, thread ID: {}",
            resourcePath,
            chatTopic.chatId(),
            chatTopic.threadId());
      }
    } catch (IOException | TelegramApiException e) {
      log.error("Failed to send photo from classpath: {}", resourcePath, e);
    }
  }

  private void sendMessage(ChatTopic chatTopic, String text) {
    SendMessage sendMessage =
        SendMessage.builder()
            .chatId(chatTopic.chatId())
            .messageThreadId(chatTopic.threadId())
            .text(text)
            .build();

    try {
      telegramClient.execute(sendMessage);
      log.info(
          "Sent message to chat ID: {}, thread ID: {}", chatTopic.chatId(), chatTopic.threadId());
    } catch (TelegramApiException e) {
      log.error(
          "Failed to send message to chat ID: {}, thread ID: {}",
          chatTopic.chatId(),
          chatTopic.threadId(),
          e);
    }
  }
}
