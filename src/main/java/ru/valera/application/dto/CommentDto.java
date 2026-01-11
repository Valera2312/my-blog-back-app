package ru.valera.application.dto;

public record CommentDto(long id,
                         String text,
                         String postId) {
}
