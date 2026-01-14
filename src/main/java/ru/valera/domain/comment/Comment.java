package ru.valera.domain.comment;

import lombok.Getter;

@Getter
public class Comment {

    private CommentId id;
    private final String text;

    public static Comment create(CommentId id, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Comment text cannot be null or blank");
        }
        return new Comment(id, text);
    }

    public void assignId(CommentId id) {
        if (this.id != null) {
            throw new IllegalStateException("Comment already has id");
        }
        this.id = id;
    }

    private Comment(CommentId id, String text) {
        this.id = id;
        this.text = text;
    }
}
