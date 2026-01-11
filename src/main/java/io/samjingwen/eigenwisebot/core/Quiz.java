package io.samjingwen.eigenwisebot.core;

import java.util.List;

public record Quiz(
    int id, String question, List<String> options, int correctOptionId, String explanation) {}
