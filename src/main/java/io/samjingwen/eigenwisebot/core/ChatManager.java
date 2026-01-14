package io.samjingwen.eigenwisebot.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ChatManager {

  private final Map<ChatTopic, Set<Module>> chats = new ConcurrentHashMap<>();

  public Map<ChatTopic, Set<Module>> getChats() {
    return Map.copyOf(chats);
  }

  public void register(ChatTopic chatTopic, Module module) {
    chats.compute(
        chatTopic,
        (key, modules) -> {
          if (modules == null) {
            modules = Collections.synchronizedSet(EnumSet.noneOf(Module.class));
          }
          modules.add(module);
          return modules;
        });
  }

  public void unregister(ChatTopic chatTopic, Module module) {
    chats.computeIfPresent(
        chatTopic,
        (key, modules) -> {
          modules.remove(module);
          return modules.isEmpty() ? null : modules;
        });
  }
}
