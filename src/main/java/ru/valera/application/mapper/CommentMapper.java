package ru.valera.application.mapper;

import org.mapstruct.*;
import org.springframework.stereotype.Component;
import ru.valera.application.dto.CommentDto;
import ru.valera.domain.comment.Comment;
import ru.valera.domain.comment.CommentId;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
@Component
public interface CommentMapper {

    @Mapping(target = "id", expression = "java(comment.getId().getValue())")
    @Mapping(target = "postId", expression = "java(postId)")
    @Mapping(target = "text", expression = "java(comment.getText())")
    CommentDto toCommentDto(Comment comment, @Context long postId);

    Comment toComment(CommentDto commentDto);

    @ObjectFactory
    default Comment createComment(CommentDto dto) {
        return Comment.create(
                CommentId.of(dto.id()),
                dto.text()
        );
    }
}
