package ru.valera.presentation.post;

import org.springframework.web.bind.annotation.*;
import ru.valera.application.dto.PostDto;

@RestController
@RequestMapping("/api")
public class PostController {

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
