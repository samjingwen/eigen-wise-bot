package io.samjingwen.eigenwisebot.core;

import io.samjingwen.eigenwisebot.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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

  private Map<Module, List<Quiz>> quizzes = new HashMap<>();

  public String getToken() {
    return appProperties.getToken();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationEvent() {
    this.quizzes = loadQuestionsFromResources();
    int totalQuestions = quizzes.values().stream().mapToInt(List::size).sum();
    log.info("Successfully loaded {} questions from resources.", totalQuestions);
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
        String[] parts = message.split("\\s+", 3);
        String command = (parts.length > 1) ? parts[1].toLowerCase() : "";
        switch (command) {
          case "random" -> {
            Module module = parseModule(chatTopic, parts, "random");
            if (module != null) {
              sendRandomQuiz(chatTopic, module);
            }
          }
          case "register" -> {
            Module module = parseModule(chatTopic, parts, "register");
            if (module != null) {
              chatManager.register(chatTopic, module);
              sendMessage(
                  chatTopic,
                  "Success! Registered for daily " + module.getDisplayName() + " quizzes.");
            }
          }
          case "unregister" -> {
            Module module = parseModule(chatTopic, parts, "unregister");
            if (module != null) {
              chatManager.unregister(chatTopic, module);
              sendMessage(
                  chatTopic,
                  "You have been unregistered from daily " + module.getDisplayName() + " quizzes.");
            }
          }
          case "" -> sendMessage(chatTopic, "Please provide a command. Try '/ewb random'");
          default -> sendMessage(chatTopic, "Unknown command. Try '/ewb random'");
        }
      }
    }
  }

  @Scheduled(cron = "${QUIZ_SCHEDULE}")
  public void sendDailyQuiz() {
    log.info("Starting scheduled daily quiz for all registered chats.");
    chatManager.getChats().forEach(this::sendRandomQuizzes);
  }

  private void sendRandomQuizzes(ChatTopic chatTopic, Set<Module> modules) {
    modules.forEach(module -> sendRandomQuiz(chatTopic, module));
  }

  private void sendRandomQuiz(ChatTopic chatTopic, Module module) {
    List<Quiz> moduleQuizzes = quizzes.get(module);
    if (moduleQuizzes == null || moduleQuizzes.isEmpty()) return;

    int randomIndex = ThreadLocalRandom.current().nextInt(moduleQuizzes.size());
    Quiz quiz = moduleQuizzes.get(randomIndex);

    sendMessage(chatTopic, "Here's a question on " + module.getDisplayName() + ":");
    sendImage(chatTopic, module, quiz.id());
    sendPoll(chatTopic, module, quiz);
  }

  private Map<Module, List<Quiz>> loadQuestionsFromResources() {
    Map<Module, List<Quiz>> questions = new HashMap<>();
    for (Module module : Module.values()) {
      List<Quiz> moduleQuizzes = new ArrayList<>();
      for (int i = 0; i < module.getCount(); i++) {
        String resourcePath = String.format("quiz/%s/%d/poll.json", module.getCode(), i);
        try {
          ClassPathResource resource = new ClassPathResource(resourcePath);
          if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
              Quiz question = objectMapper.readValue(is, Quiz.class);
              moduleQuizzes.add(question);
              log.info("Loaded question from: {}", resourcePath);
            }
          } else {
            log.warn("Question file not found: {}", resourcePath);
          }
        } catch (IOException e) {
          log.error("Failed to load question from: {}", resourcePath, e);
        }
      }
      if (!moduleQuizzes.isEmpty()) {
        questions.put(module, moduleQuizzes);
      }
      if (!moduleQuizzes.isEmpty()) {
        questions.put(module, moduleQuizzes);
      }
    }
    return questions;
  }

  private Module parseModule(ChatTopic chatTopic, String[] parts, String commandAction) {
    if (parts.length <= 2) {
      sendMessage(
          chatTopic,
          String.format(
              "Please specify a module (linalg or advlinalg). Example: /ewb %s linalg",
              commandAction));
      return null;
    }

    String arg = parts[2];
    return Module.fromString(arg)
        .orElseGet(
            () -> {
              sendMessage(chatTopic, "Unknown module '" + arg + "'. Use 'linalg' or 'advlinalg'.");
              return null;
            });
  }

  private void sendPoll(ChatTopic chatTopic, Module module, Quiz quiz) {
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

  private void sendImage(ChatTopic chatTopic, Module module, int id) {
    String resourcePath = String.format("quiz/%s/%d/img.jpg", module.getCode(), id);
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
