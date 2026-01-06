package ru.valera.presentation.post;

import org.springframework.web.bind.annotation.*;
import ru.valera.presentation.post.dto.PostDto;

@RestController
@RequestMapping("/api")
public class PostController {

    @GetMapping("/posts/{id}")
    public PostDto getPost(@PathVariable Integer id) {
        return new PostDto(id, "Заголовок поста", "Текст поста");
    }

    @GetMapping("/posts")
    public PostDto getPosts(@RequestParam String search,
                            @RequestParam String pageNumber,
                            @RequestParam String pageSize) {
        return new PostDto(1, "Заголовок поста", "Текст поста");
    }
}
