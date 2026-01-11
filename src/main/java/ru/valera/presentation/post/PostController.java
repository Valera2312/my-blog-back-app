package ru.valera.presentation.post;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.valera.application.PostService;
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

    @GetMapping("/posts/{id}")
    public PostDto getPost(@PathVariable Integer id) {
        return null;
    }

    @GetMapping("/posts")
    public PostDto getPosts(@RequestParam String search,
                            @RequestParam String pageNumber,
                            @RequestParam String pageSize) {
        return null;
    }
}
