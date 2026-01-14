package ru.valera.presentation.post;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.valera.application.PostService;
import ru.valera.application.dto.CommentDto;
import ru.valera.application.dto.PageResultDto;
import ru.valera.application.dto.PostDto;

import java.util.Collection;

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

    @DeleteMapping("posts/{id}")
    public void deletePost(@PathVariable Long id) {
        postService.deletePost(id);
    }

    @PostMapping("posts/{id}/likes")
    public long like(@PathVariable Long id) {
       return postService.like(id);
    }

    @PutMapping(value = "/posts/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> updatePostImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) {
        postService.updateImage(id, image);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/posts/{id}/image")
    public ResponseEntity<byte[]> getPostImage(@PathVariable Long id) {
        byte[] image = postService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .contentLength(image.length)
                .body(image);
    }

    @GetMapping(value = "/posts/{id}/comments")
    public Collection<CommentDto> getComments(@PathVariable Long id) {
        return null;
    }

}
