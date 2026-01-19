package ru.valera.application.dto;

import java.util.Set;

public record PostDto(
        Integer id,
        String title,
        String text,
        Set<String> tags,
        int likesCount,
        int commentsCount) {
}
