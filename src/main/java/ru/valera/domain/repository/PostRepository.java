package ru.valera.domain.repository;

import ru.valera.domain.post.Post;
import ru.valera.domain.post.PostId;
import ru.valera.domain.search.PageRequest;
import ru.valera.domain.search.PostSearchCriteria;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    List<Post> findAll();
    Optional<Post> findById(PostId id);

    Post save(Post post);
    void delete(PostId id);

    List<Post> findBy(PostSearchCriteria criteria, PageRequest page);
    long countBy(PostSearchCriteria criteria);
}
