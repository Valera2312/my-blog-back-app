package ru.valera.domain.repository;

import ru.valera.domain.post.PostId;

public interface PostQueryRepository {
    int countComments(PostId postId);
}
