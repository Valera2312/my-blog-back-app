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
import ru.valera.domain.tag.Tag;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    @Transactional
    public PostDto createPost(final PostDto postDto) {
        Post post = postMapper.createPost(postDto);
        postRepository.save(post);
        return postMapper.toPostDto(post);
    }

    @Transactional(readOnly = true)
    public PostDto getPostById(final Long postId) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        return postMapper.toPostDto(post);
    }

    @Transactional(readOnly = true)
    public PageResultDto<PostDto> getPosts(final String search, final int pageNumber, final int pageSize) {
        PageRequest page = PageRequest.of(pageNumber, pageSize);
        PostSearchCriteria criteria = new PostSearchCriteria(extractTitles(search), extractTags(search));

        List<PostDto> postDtos = postRepository
                .findBy(criteria, page).stream()
                .map(postMapper::toPostDto)
                .toList();
        long total = postRepository.countBy(criteria);
        long lastPage = (long) Math.ceil((double) total / page.size());
        boolean hasPrev = page.page() > 1;
        boolean hasNext = page.page() + 1 < lastPage;

        return PageResultDto.<PostDto>builder()
                .items(postDtos)
                .lastPage(lastPage)
                .hasPrev(hasPrev)
                .hasNext(hasNext)
                .build();
    }

    @Transactional
    public PostDto updatePost(final Long postId, final PostDto postDto) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        Set<Tag> tags = postDto.tags()
                .stream()
                .map(tag -> Tag.create(null, tag))
                .collect(Collectors.toSet());
        post.updateContent(postDto.title(), postDto.text(), tags);
        return postMapper.toPostDto(postRepository.save(post));
    }

    private Set<String> extractTitles(String search) {
        return Arrays.stream(search.split("\\s+"))
                .filter(s -> !s.startsWith("#"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static Set<TagName> extractTags(String search) {
        return Arrays.stream(search.split("\\s+"))
                .filter(s -> s.startsWith("#"))
                .map(TagName::new)
                .collect(Collectors.toSet());
    }
}
