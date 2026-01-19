import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.valera.application.PostService;
import ru.valera.infrastructure.persistence.jdbc.config.DataSourceFactory;
import ru.valera.infrastructure.web.WebConfig;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(classes = {DataSourceFactory.class, WebConfig.class})
@WebAppConfiguration
@Testcontainers
public class PostControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    PostService postService;

    @Autowired
    DataSource dataSource;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();

        postService.createPost(
                new ru.valera.application.dto.PostDto(
                        null,
                        "Название поста 1",
                        "Текст поста в формате Markdown...",
                        java.util.Set.of("tag_1", "tag_2"),
                        0,
                        0
                )
        );
        postService.createPost(
                new ru.valera.application.dto.PostDto(
                        0,
                        "Название поста 2",
                        "Текст поста в формате Markdown...",
                        java.util.Set.of("tag_1", "tag_2"),
                        0,
                        0
                )
        );
    }

    @AfterEach
    void teardown() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             var stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE posts, tags, post_tags, images, comments RESTART IDENTITY CASCADE");
        }
    }

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("blog")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("db.url", postgres::getJdbcUrl);
        r.add("db.username", postgres::getUsername);
        r.add("db.password", postgres::getPassword);
        r.add("db.driver", postgres::getDriverClassName);
    }

    @Test
    void getPostById() throws Exception {

        String resultJsonJson = """
                {
                     "id": 1,
                     "title": "Название поста 1",
                     "text": "Текст поста в формате Markdown...",
                     "tags": ["tag_1", "tag_2"],
                     "likesCount": 0,
                     "commentsCount": 0
                }
                """;
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/posts/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(resultJsonJson));
    }

    @Test
    void getAllPosts() throws Exception {
        String expectedJson = """
                {
                   "posts" : [ {
                     "id" : 8,
                     "title" : "Название поста 8",
                     "text" : "Текст поста в формате Markdown...",
                     "tags" : [ "tag_1", "tag_2" ],
                     "likesCount" : 0,
                     "commentsCount" : 0
                   }, {
                     "id" : 7,
                     "title" : "Название поста 7",
                     "text" : "Текст поста в формате Markdown...",
                     "tags" : [ "tag_1", "tag_2" ],
                     "likesCount" : 0,
                     "commentsCount" : 0
                   }, {
                     "id" : 6,
                     "title" : "Название поста 6",
                     "text" : "Текст поста в формате Markdown...",
                     "tags" : [ "tag_1", "tag_2" ],
                     "likesCount" : 0,
                     "commentsCount" : 0
                   }, {
                     "id" : 5,
                     "title" : "Название поста 5",
                     "text" : "Текст поста в формате Markdown...",
                     "tags" : [ "tag_1", "tag_2" ],
                     "likesCount" : 0,
                     "commentsCount" : 0
                   }, {
                     "id" : 4,
                     "title" : "Название поста 4",
                     "text" : "Текст поста в формате Markdown...",
                     "tags" : [ "tag_1", "tag_2" ],
                     "likesCount" : 0,
                     "commentsCount" : 0
                   } ],
                   "hasPrev" : false,
                   "hasNext" : true,
                   "lastPage" : 2
                 }
                """;
        for (int i = 3; i <= 8; i++) {
            postService.createPost(
                    new ru.valera.application.dto.PostDto(
                            null,
                            "Название поста " + i,
                            "Текст поста в формате Markdown...",
                            java.util.Set.of("tag_1", "tag_2"),
                            0,
                            0
                    )
            );
        }
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/posts")
                .param("search", "")
                .param("pageNumber", "1")
                .param("pageSize", "5")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void getAllPostsWithSearch() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/posts")
                .param("search", "Название")
                .param("pageNumber", "1")
                .param("pageSize", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0]").exists());
    }

    @Test
    void getAllPostsWithTagSearch() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/posts")
                .param("search", "#tag_1")
                .param("pageNumber", "1")
                .param("pageSize", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0]").exists());
    }

    @Test
    void createPost() throws Exception {
        String newPostJson = """
                {
                     "title": "Новый пост",
                     "text": "Текст нового поста",
                     "tags": ["new_tag"]
                }
                """;
        
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/posts")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPostJson);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Новый пост"))
                .andExpect(jsonPath("$.text").value("Текст нового поста"))
                .andExpect(jsonPath("$.tags[0]").value("new_tag"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0));
    }

    @Test
    void updatePost() throws Exception {
        String updateJson = """
                {
                     "title": "Обновленное название",
                     "text": "Обновленный текст",
                     "tags": ["updated_tag"]
                }
                """;

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .put("/api/posts/{id}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Обновленное название"))
                .andExpect(jsonPath("$.text").value("Обновленный текст"))
                .andExpect(jsonPath("$.tags[0]").value("updated_tag"));
    }

    @Test
    void deletePost() throws Exception {

        String newPostJson = """
                {
                     "title": "Пост для удаления",
                     "text": "Текст",
                     "tags": ["tag"]
                }
                """;
        
        var createRequest = MockMvcRequestBuilders
                .post("/api/posts")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPostJson);

        mockMvc.perform(createRequest)
                .andExpect(status().isOk());

        RequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete("/api/posts/{id}", 2)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isOk());

        RequestBuilder getRequest = MockMvcRequestBuilders
                .get("/api/posts/{id}", 2)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void likePost() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/posts/{id}/likes", 1)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void updatePostImage() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart(HttpMethod.PUT,"/api/posts/{id}/image", 1)
                .file(imageFile)
                .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk());
    }

    @Test
    void getPostImage() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        var uploadRequest = MockMvcRequestBuilders
                .multipart(HttpMethod.PUT,"/api/posts/{id}/image", 1)
                .file(imageFile)
                .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(uploadRequest)
                .andExpect(status().isOk());

        RequestBuilder getRequest = MockMvcRequestBuilders
                .get("/api/posts/{id}/image", 1)
                .accept(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().bytes("test image content".getBytes()));
    }

    @Test
    void getPostComments() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/posts/{postId}/comments", 1)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getComment() throws Exception {

        String commentJson = """
                {
                     "text": "Тестовый комментарий"
                }
                """;

        var createCommentRequest = MockMvcRequestBuilders
                .post("/api/posts/{postId}/comments", 1)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson);

        mockMvc.perform(createCommentRequest)
                .andExpect(status().isOk());

        RequestBuilder getRequest = MockMvcRequestBuilders
                .get("/api/posts/{postId}/comments/{commentId}", 1, 1)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void addComment() throws Exception {
        String commentJson = """
                {
                     "text": "Новый комментарий для поста"
                }
                """;

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/posts/{postId}/comments", 1)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("Новый комментарий для поста"))
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void updateComment() throws Exception {
        String createCommentJson = """
                {
                     "text": "Исходный комментарий"
                }
                """;

        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/posts/{postId}/comments", 1)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCommentJson)
        ).andExpect(status().isOk());

        String updateCommentJson = """
                {
                     "id": 1,
                     "text": "Обновленный комментарий",
                     "postId": 1
                }
                """;

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .put("/api/posts/{postId}/comments/{commentId}", 1, 1)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateCommentJson);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("Обновленный комментарий"));
    }

    @Test
    void deleteComment() throws Exception {
        String commentJson = """
                {
                     "text": "Комментарий для удаления"
                }
                """;

        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/posts/{postId}/comments", 1)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson)
        ).andExpect(status().isOk());

        RequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete("/api/posts/{postId}/comments/{commentId}", 1, 1)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isOk());
    }
}
