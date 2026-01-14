package ru.valera.application.dto;

import lombok.Builder;

@Builder
public record CommentDto(long id,
                         String text,
                         long postId) {
}
