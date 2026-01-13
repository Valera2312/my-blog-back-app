package ru.valera.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.valera.application.dto.PageResultDto;
import ru.valera.application.dto.PostDto;
import ru.valera.application.mapper.PostMapper;
import ru.valera.domain.post.Post;
import ru.valera.domain.post.PostId;
import ru.valera.domain.repository.PostQueryRepository;
import ru.valera.domain.repository.PostRepository;
import ru.valera.domain.search.PageRequest;
import ru.valera.domain.search.PostSearchCriteria;
import ru.valera.domain.search.TagName;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostMapper postMapper;

    @Transactional
    public PostDto createPost(final PostDto postDto) {
        Post post = postMapper.createPost(postDto);
        postRepository.save(post);
        return postMapper.toPostDto(post, 0);
    }

    @Transactional(readOnly = true)
    public PostDto getPostById(final Long postId) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        int commentsCount = postQueryRepository.countComments(PostId.of(postId));
        return postMapper.toPostDto(post, commentsCount);
    }

    @Transactional(readOnly = true)
    public PageResultDto<PostDto> getPosts(final String search, final int pageNumber, final int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        PostSearchCriteria criteria = new PostSearchCriteria(extractTitles(search), extractTags(search));

        //List<PostDto> postDtos = postRepository.findBy(criteria, pageRequest);
        //TODO
        return null;
    }

    private Set<String> extractTitles(String search) {
        return Arrays.stream(search.split("\\s+"))
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toSet());
    }

    private static Set<TagName> extractTags(String search) {
        return Arrays.stream(search.split("\\s+"))
                .filter(s -> s.startsWith("#"))
                .map(TagName::new)
                .collect(Collectors.toSet());
    }
}
