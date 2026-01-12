package io.samjingwen.eigenwisebot.core;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ChatManager {

  private final Set<ChatTopic> chats = new HashSet<>();

  public void addChat(ChatTopic chatTopic) {
    chats.add(chatTopic);
  }

  public void removeChat(ChatTopic chatTopic) {
    chats.remove(chatTopic);
  }
}
