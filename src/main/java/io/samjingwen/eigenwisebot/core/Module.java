package io.samjingwen.eigenwisebot.core;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Module {
  LINEAR_ALGEBRA("linalg", "Linear Algebra", 1),
  ADVANCED_LINEAR_ALGEBRA("advlinalg", "Advanced Linear Algebra", 6),
  ;

  private final String code;
  private final String displayName;
  private final int count;

  public static Optional<Module> fromString(String text) {
    if (text == null) return Optional.empty();

    String normalized = text.toLowerCase().trim();

    return Arrays.stream(values()).filter(m -> m.code.equals(normalized)).findFirst();
  }
}
