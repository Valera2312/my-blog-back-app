package ru.valera.domain.comment;

import lombok.Getter;

@Getter
public class Comment {

    private final CommentId id;
    private final String text;

    public static Comment create(CommentId id, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Comment text cannot be null or blank");
        }
        return new Comment(id, text);
    }

    private Comment(CommentId id, String text) {

        this.id = id;
        this.text = text;
    }
}
