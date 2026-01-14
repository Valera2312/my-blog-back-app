package ru.valera.application.mapper;

import org.mapstruct.*;
import org.springframework.stereotype.Component;
import ru.valera.application.dto.CommentDto;
import ru.valera.domain.comment.Comment;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
@Component
public interface CommentMapper {

    @Mapping(target = "id", source = "comment.id.value")
    @Mapping(target = "postId", expression = "java(postId)")
    CommentDto toDto(Comment comment, @Context long postId);

    Comment toComment(CommentDto commentDto);

    @ObjectFactory
    default Comment createComment(CommentDto dto) {
        return Comment.create(
                null,
                dto.text()
        );
    }
}
