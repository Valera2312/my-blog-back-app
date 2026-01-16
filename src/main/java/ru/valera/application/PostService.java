package ru.valera.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.valera.application.dto.CommentDto;
import ru.valera.application.dto.PageResultDto;
import ru.valera.application.dto.PostDto;
import ru.valera.application.mapper.CommentMapper;
import ru.valera.application.mapper.PostMapper;
import ru.valera.domain.Image.Image;
import ru.valera.domain.comment.Comment;
import ru.valera.domain.comment.CommentId;
import ru.valera.domain.post.Post;
import ru.valera.domain.post.PostId;
import ru.valera.domain.repository.PostQueryRepository;
import ru.valera.domain.repository.PostRepository;
import ru.valera.domain.search.PageRequest;
import ru.valera.domain.search.PostSearchCriteria;
import ru.valera.domain.search.TagName;
import ru.valera.domain.tag.Tag;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostQueryRepository  postQueryRepository;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Transactional
    public PostDto createPost(final PostDto postDto) {
        Post post = postRepository.save(postMapper.createPost(postDto));
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
        boolean hasNext = page.page() < lastPage;
        if (lastPage == page.page()) {
            hasPrev = false;
            hasNext = false;
        }
        return PageResultDto.<PostDto>builder()
                .posts(postDtos)
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

    @Transactional
    public void deletePost(final Long postId) {
        postRepository.delete(PostId.of(postId));
    }

    @Transactional
    public long like(final Long postId) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        post.like();
        postRepository.save(post);
        return post.getLikesCount();
    }

    @Transactional
    public void updateImage(final Long postId, final MultipartFile image) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        post.updateImage(image.getOriginalFilename());
        postRepository.save(post);
        try {
            saveBytesToFile(image.getOriginalFilename(), image.getBytes());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] getImage(final Long postId) {
        final Optional<Image> image = postQueryRepository.findImage(PostId.of(postId));
        return image
               .map(image1 -> findFile(image1.getUrl()))
               .orElse(new byte[]{});
    }

    @Transactional(readOnly = true)
    public Collection<CommentDto> getComments(final Long postId) {
        return postQueryRepository.findComments(PostId.of(postId))
                .stream()
                .map(comment -> commentMapper.toCommentDto(comment, postId))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommentDto getComment(final Long postId, final Long commentId) {
        Comment comment = postQueryRepository.findComment(PostId.of(postId), CommentId.of(commentId));
        return commentMapper.toCommentDto(comment, postId);
    }

    @Transactional
    public CommentDto updateComment(final Long postId, final Long commentId, final CommentDto commentDto) {
        Post post = postRepository.findById(PostId.of(commentDto.postId()))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        post.editComment(commentMapper.toComment(commentDto));
        postRepository.save(post);
        return commentDto;
    }

    @Transactional
    public CommentDto addComment(final Long postId, final CommentDto commentDto) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        Comment comment = post.addComment(commentDto.text());
        postRepository.save(post);
        return commentMapper.toCommentDto(comment, postId);
    }

    @Transactional
    public void deleteComment(final Long postId, final Long commentId) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        post.removeComment(CommentId.of(commentId));
        postRepository.save(post);
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
                .map(s -> s.substring(1))
                .map(TagName::new)
                .collect(Collectors.toSet());
    }

    private static byte[] findFile(String fileName) {
        try {
            Resource resource = new ClassPathResource("images/" + fileName);
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path pathTraversalProtection(String fileName) throws URISyntaxException, IOException {
        Path path = Paths.get(
                Objects.requireNonNull(
                        PostService.class.getClassLoader().getResource("images")).toURI());
        Path realBaseDir = Paths.get(path.toUri()).toRealPath();
        Path targetPath = realBaseDir.resolve(Objects.requireNonNull(fileName)).normalize();
        if (!targetPath.startsWith(realBaseDir)) {
            throw new SecurityException("Path traversal attempt: " + fileName);
        }
        return targetPath;
    }

    private static void saveBytesToFile(String fileName, byte[] data) throws IOException, URISyntaxException {
        Path targetPath = pathTraversalProtection(fileName);
        try (FileOutputStream fos = new FileOutputStream(targetPath.toString())) {
            fos.write(data);
            System.out.println("Image successfully saved: " + fileName);
        } catch (IOException e) {
           log.error(e.getMessage());
        }
    }
}
