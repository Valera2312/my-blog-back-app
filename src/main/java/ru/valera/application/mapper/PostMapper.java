package ru.valera.application.mapper;

import org.mapstruct.*;
import org.springframework.stereotype.Component;
import ru.valera.application.dto.PostDto;
import ru.valera.domain.post.Post;
import ru.valera.domain.tag.Tag;

import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
@Component
public interface PostMapper {


    @Mapping(target = "id", source = "post.id.value")
    @Mapping(target = "tags", expression = "java(mapTagsToStrings(post.getTags()))")
    PostDto toPostDto(Post post);

    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "tags", expression = "java(mapStringsToTags(postDto.tags()))")
    Post toPost(PostDto postDto);

    @ObjectFactory
    default Post createPost(PostDto dto) {
        return Post.create(
                dto.title(),
                dto.text(),
                mapStringsToTags(dto.tags())
        );
    }

    default Set<Tag> mapStringsToTags(Set<String> tagNames) {
        return tagNames.stream()
                .map(name -> Tag.create(null, name))
                .collect(java.util.stream.Collectors.toSet());
    }

    default Set<String> mapTagsToStrings(Set<Tag> tags) {
        return tags.stream()
                .map(Tag::getName)
                .collect(java.util.stream.Collectors.toSet());
    }
}
