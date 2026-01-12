package ru.valera.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.valera.application.dto.PostDto;
import ru.valera.application.mapper.PostMapper;
import ru.valera.domain.post.Post;
import ru.valera.domain.repository.PostQueryRepository;
import ru.valera.domain.repository.PostRepository;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostMapper postMapper;

    @Transactional
    public PostDto createPost(PostDto postDto) {
        Post post = postMapper.createPost(postDto);
        postRepository.save(post);
        return postMapper.toPostDto(post, 0);
    }
}
