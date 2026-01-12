package ru.valera.domain.post;

import ru.valera.domain.Image.Image;
import ru.valera.domain.comment.Comment;
import ru.valera.domain.comment.CommentId;
import ru.valera.domain.tag.Tag;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Post {

    private final PostId id;
    private String title;
    private String text;
    private int likesCount;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Image image;

    private final List<Comment> comments;
    private final Set<Tag> tags;
    
    public static Post create(String title, String text, Set<Tag> tags) {

        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(text, "text cannot be null");
        Objects.requireNonNull(tags, "tags cannot be null");

        return new Post(
                null,
                title,
                text,
                0,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                new ArrayList<>(),
                tags);
    }

    public static Post fromDatabase(
            PostId id,
            String title,
            String text,
            int likesCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Image image,
            List<Comment> comments,
            Set<Tag> tags) {

        Objects.requireNonNull(id, "id cannot be null when loading from database");
        Objects.requireNonNull(comments, "comments list cannot be null");
        Objects.requireNonNull(tags, "tags set cannot be null");

        return new Post(
                id,
                title,
                text,
                likesCount,
                createdAt,
                updatedAt,
                image,
                new ArrayList<>(comments),
                new HashSet<>(tags)
        );
    }

    private Post(
            PostId id,
            String title,
            String text,
            int likesCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Image image,
            List<Comment> comments,
            Set<Tag> tags) {

        this.id = id;
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.text = Objects.requireNonNull(text, "text cannot be null");
        this.likesCount = Math.max(0, likesCount);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.image = image;
        this.comments = comments;
        this.tags = Objects.requireNonNull(tags);;
    }

    // ── Update Content ─────────────────────────────────────────────────
    public void updateContent(String newTitle, String newText, Set<Tag> newTags) {
        this.title = Objects.requireNonNull(newTitle);
        this.text = Objects.requireNonNull(newText);
        this.tags.clear();
        this.tags.addAll(Objects.requireNonNull(newTags));
        this.touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Likes ──────────────────────────────────────────────────────────
    public void like() {
        this.likesCount++;
        this.touch();
    }

    public void unlike() {
        if (this.likesCount > 0) {
            this.likesCount--;
            this.touch();
        }
    }

    // ── Comments ────────────────────────────────────────────────────────
    public Comment addComment(String text) {
        Comment comment = Comment.create(null, text);
        comments.add(comment);
        touch();
        return comment;
    }

    public void editComment(Comment updatedComment) {
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i).getId().equals(updatedComment.getId())) {
                comments.set(i, updatedComment);
                touch();
                return;
            }
        }
        throw new NoSuchElementException("Comment not found: " + updatedComment.getId());
    }

    public void removeComment(CommentId commentId) {
        boolean removed = comments.removeIf(c -> c.getId().equals(commentId));
        if (removed) {
            touch();
        }
    }

    // ── Image ──────────────────────────────────────────────────────────
    public void updateImage(String newUrl) {
        this.image = Image.create(newUrl);
        touch();
    }

    // ── Tags ───────────────────────────────────────────────────────────
    public void addTag(Tag tag) {
        tags.add(tag);
        touch();
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        touch();
    }

    public Set<String> getTagNames() {
        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(id, post.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    // ── Getters ─────────────────────────────────────────────────────────
    public PostId getId() { return id; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public int getLikesCount() { return likesCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Optional<Image> getImage() { return Optional.ofNullable(image); }
    public List<Comment> getComments() { return Collections.unmodifiableList(comments); }
    public Set<Tag> getTags() { return Collections.unmodifiableSet(tags); }

    private Comment findCommentById(CommentId commentId) {
        return comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Comment not found: " + commentId));
    }
}
