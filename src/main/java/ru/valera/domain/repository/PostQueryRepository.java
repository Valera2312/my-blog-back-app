package ru.valera.domain.repository;

import ru.valera.domain.Image.Image;
import ru.valera.domain.comment.Comment;
import ru.valera.domain.comment.CommentId;
import ru.valera.domain.post.PostId;

import java.util.List;

public interface PostQueryRepository {
    int countComments(PostId postId);
    Comment findComment(PostId postId, CommentId page);
    List<Comment> findComments(PostId postId);
    Image findImage(PostId postId);
}
