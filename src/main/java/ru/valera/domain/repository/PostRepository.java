package ru.valera.domain.repository;

import ru.valera.domain.post.Post;
import ru.valera.domain.post.PostId;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    List<Post> findAll();
    Optional<Post> findById(PostId id);

    Post save(Post post);
    void delete(PostId id);
}
