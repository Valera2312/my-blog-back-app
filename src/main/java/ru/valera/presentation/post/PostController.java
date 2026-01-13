package ru.valera.presentation.post;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.valera.application.PostService;
import ru.valera.application.dto.PageResultDto;
import ru.valera.application.dto.PostDto;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public PostDto createPost(@RequestBody PostDto postDto){
        return postService.createPost(postDto);
    }

    @PostMapping("/posts/{id}")
    public PostDto getPost(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @GetMapping("/posts")
    public PageResultDto<PostDto> getPosts(@RequestParam String search,
                                           @RequestParam int pageNumber,
                                           @RequestParam int pageSize) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    @PutMapping("posts/{id}")
    public PostDto updatePost(@PathVariable Long id, @RequestBody PostDto postDto) {
       return postService.updatePost(id, postDto);
    }
}
