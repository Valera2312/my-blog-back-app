package ru.valera.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.valera.application.dto.CommentDto;
import ru.valera.application.dto.PageResultDto;
import ru.valera.application.dto.PostDto;
import ru.valera.application.mapper.CommentMapper;
import ru.valera.application.mapper.PostMapper;
import ru.valera.application.pagination.PaginationService;
import ru.valera.application.search.SearchQueryParser;
import ru.valera.domain.Image.Image;
import ru.valera.domain.Image.ImageNotFoundException;
import ru.valera.domain.comment.Comment;
import ru.valera.domain.comment.CommentId;
import ru.valera.domain.post.Post;
import ru.valera.domain.post.PostId;
import ru.valera.domain.post.PostNotFoundException;
import ru.valera.domain.repository.PostQueryRepository;
import ru.valera.domain.repository.PostRepository;
import ru.valera.domain.search.PageRequest;
import ru.valera.domain.search.PostSearchCriteria;
import ru.valera.domain.storage.ImageStorage;
import ru.valera.domain.tag.Tag;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final ImageStorage imageStorage;
    private final SearchQueryParser searchQueryParser;
    private final PaginationService paginationService;

    @Transactional
    public PostDto createPost(final PostDto postDto) {
        Post post = postRepository.save(postMapper.createPost(postDto));
        return postMapper.toPostDto(post);
    }

    @Transactional(readOnly = true)
    public PostDto getPostById(final Long postId) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new PostNotFoundException(postId));
        return postMapper.toPostDto(post);
    }

    @Transactional(readOnly = true)
    public PageResultDto<PostDto> getPosts(final String search, final int pageNumber, final int pageSize) {
        PageRequest page = PageRequest.of(pageNumber, pageSize);
        PostSearchCriteria criteria = searchQueryParser.parse(search);
        
        List<PostDto> postDtos = postRepository
                .findBy(criteria, page).stream()
                .map(postMapper::toPostDto)
                .toList();
        
        long total = postRepository.countBy(criteria);
        return paginationService.createPageResult(postDtos, total, page);
    }

    @Transactional
    public PostDto updatePost(final Long postId, final PostDto postDto) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new PostNotFoundException(postId));
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
                .orElseThrow(() -> new PostNotFoundException(postId));
        post.like();
        postRepository.save(post);
        return post.getLikesCount();
    }

    @Transactional
    public void updateImage(final Long postId, final MultipartFile image) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new PostNotFoundException(postId));
        post.updateImage(image.getOriginalFilename());
        postRepository.save(post);
        try {
            imageStorage.save(image.getOriginalFilename(), image.getBytes());
        } catch (IOException e) {
            throw new ImageStorage.StorageException("Failed to save image", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] getImage(final Long postId) {
        final Optional<Image> image = postQueryRepository.findImage(PostId.of(postId));
        return image
                .map(Image::getUrl)
                .flatMap(imageStorage::load)
                .orElseThrow(() -> new ImageNotFoundException("Image not found for post " + postId));
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
                .orElseThrow(() -> new PostNotFoundException(postId));
        post.editComment(commentMapper.toComment(commentDto));
        postRepository.save(post);
        return commentDto;
    }

    @Transactional
    public CommentDto addComment(final Long postId, final CommentDto commentDto) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new PostNotFoundException(postId));
        Comment comment = post.addComment(commentDto.text());
        postRepository.save(post);
        return commentMapper.toCommentDto(comment, postId);
    }

    @Transactional
    public void deleteComment(final Long postId, final Long commentId) {
        Post post = postRepository.findById(PostId.of(postId))
                .orElseThrow(() -> new PostNotFoundException(postId));
        post.removeComment(CommentId.of(commentId));
        postRepository.save(post);
    }
}
