package ru.valera.domain.comment;

import java.util.Objects;

public class CommentId {

    private final Long value;

    public Long getValue() {
        return value;
    }

    private CommentId(Long value) {
        this.value = Objects.requireNonNull(value, "Comment id cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("Comment id must be positive");
        }
    }

    public static CommentId of(Long value) {
        return new CommentId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        CommentId commentId = (CommentId) o;
        return value.equals(commentId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
