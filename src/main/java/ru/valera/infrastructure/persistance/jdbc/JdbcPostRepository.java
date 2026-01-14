package ru.valera.infrastructure.persistance.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import ru.valera.domain.Image.Image;
import ru.valera.domain.comment.Comment;
import ru.valera.domain.comment.CommentId;
import ru.valera.domain.post.Post;
import ru.valera.domain.post.PostId;
import ru.valera.domain.repository.PostRepository;
import ru.valera.domain.search.PageRequest;
import ru.valera.domain.search.PostSearchCriteria;
import ru.valera.domain.tag.Tag;
import ru.valera.domain.tag.TagId;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class JdbcPostRepository implements PostRepository {

    private final DataSource dataSource;

    public JdbcPostRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }

    @Override
    public List<Post> findAll() {
        String sql = """
                SELECT id, title, text, comments_count, likes_count, created_at, updated_at
                FROM posts
                ORDER BY created_at DESC
                """;

        List<Post> posts = new ArrayList<>();
        final Connection conn = getConnection();
        try (
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PostId postId = PostId.of(rs.getLong("id"));
                posts.add(Post.fromDatabase(
                        postId,
                        rs.getString("title"),
                        rs.getString("text"),
                        rs.getInt("comments_count"),
                        rs.getInt("likes_count"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        loadImage(conn, postId.getValue()),
                        loadComments(conn, postId.getValue()),
                        loadTags(conn, postId.getValue())
                ));
            }
            return posts;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Post> findById(PostId id) {
        final Connection conn = getConnection();
        try {
            Post base = loadPost(conn, id.getValue());
            if (base == null) return Optional.empty();

            List<Comment> comments = loadComments(conn, id.getValue());
            Set<Tag> tags = loadTags(conn, id.getValue());
            Image image = loadImage(conn, id.getValue());

            return Optional.of(Post.fromDatabase(
                    id,
                    base.getTitle(),
                    base.getText(),
                    base.getCommentsCount(),
                    base.getLikesCount(),
                    base.getCreatedAt(),
                    base.getUpdatedAt(),
                    image,
                    comments,
                    tags
            ));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Post save(Post post) {
        final Connection conn = getConnection();
        try {
            if (post.getId() == null) {
                long id = insertPost(conn, post);
                Post savedPost = Post.fromDatabase(
                        PostId.of(id),
                        post.getTitle(),
                        post.getText(),
                        post.getCommentsCount(),
                        post.getLikesCount(),
                        post.getCreatedAt(),
                        post.getUpdatedAt(),
                        post.getImage().orElse(null),
                        post.getComments(),
                        post.getTags()
                );
                persistTags(conn, savedPost);
                return savedPost;
            } else {
                updatePost(conn, post);
                persistChildren(conn, post);
                return post;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(PostId id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        final Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id.getValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------- Post CRUD ----------------
    private long insertPost(Connection conn, Post post) throws SQLException {
        String sql = """
                INSERT INTO posts (title, text, comments_count, likes_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getText());
            ps.setInt(3, post.getCommentsCount());
            ps.setInt(4, post.getLikesCount());
            ps.setTimestamp(5, Timestamp.valueOf(post.getCreatedAt()));
            ps.setTimestamp(6, Timestamp.valueOf(post.getUpdatedAt()));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getLong(1);
        }
    }

    private void updatePost(Connection conn, Post post) throws SQLException {
        String sql = """
                UPDATE posts
                SET title = ?, text = ?, likes_count = ?, updated_at = ?, comments_count = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getText());
            ps.setInt(3, post.getLikesCount());
            ps.setTimestamp(4, Timestamp.valueOf(post.getUpdatedAt()));
            ps.setInt(5, post.getCommentsCount());
            ps.setLong(6, post.getId().getValue());
            ps.executeUpdate();
        }
    }

    private Post loadPost(Connection conn, long id) throws SQLException {
        String sql = """
                SELECT title, text, comments_count, likes_count, created_at, updated_at
                FROM posts WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            return Post.fromDatabase(
                    PostId.of(id),
                    rs.getString("title"),
                    rs.getString("text"),
                    rs.getInt("comments_count"),
                    rs.getInt("likes_count"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime(),
                    null,
                    List.of(),
                    Set.of()
            );
        }
    }

    // ---------------- Children (comments, tags, image) ----------------
    private void persistChildren(Connection conn, Post post) throws SQLException {
        persistComments(conn, post);
        persistTags(conn, post);
        persistImage(conn, post);
    }

    private List<Comment> loadComments(Connection conn, long postId) throws SQLException {
        String sql = "SELECT id, text FROM comments WHERE post_id = ?";
        List<Comment> comments = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comments.add(Comment.create(
                        CommentId.of(rs.getLong("id")),
                        rs.getString("text")
                ));
            }
        }
        return comments;
    }

    private void persistComments(Connection conn, Post post) throws SQLException {
        long postId = post.getId().getValue();

        try (PreparedStatement delete = conn.prepareStatement(
                "DELETE FROM comments WHERE post_id = ?")) {
            delete.setLong(1, postId);
            delete.executeUpdate();
        }

        List<Comment> newComments = post.getComments().stream()
                .filter(c -> c.getId() == null)
                .toList();

        List<Comment> existingComments = post.getComments().stream()
                .filter(c -> c.getId() != null)
                .toList();

        if (!existingComments.isEmpty()) {
            try (PreparedStatement insertWithId = conn.prepareStatement(
                    "INSERT INTO comments(id, post_id, text) VALUES (?, ?, ?)")) {
                for (Comment c : existingComments) {
                    insertWithId.setLong(1, c.getId().getValue());
                    insertWithId.setLong(2, postId);
                    insertWithId.setString(3, c.getText());
                    insertWithId.addBatch();
                }
                insertWithId.executeBatch();
            }
        }
        if (!newComments.isEmpty()) {
            StringBuilder sql = new StringBuilder(
                    "INSERT INTO comments(post_id, text) VALUES ");

            for (int i = 0; i < newComments.size(); i++) {
                sql.append("(?, ?)");
                if (i < newComments.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(" RETURNING id");

            try (PreparedStatement insertNew = conn.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                for (Comment c : newComments) {
                    insertNew.setLong(paramIndex++, postId);
                    insertNew.setString(paramIndex++, c.getText());
                }

                try (ResultSet rs = insertNew.executeQuery()) {
                    for (Comment c : newComments) {
                        if (!rs.next()) {
                            throw new SQLException("No generated key returned for comment");
                        }
                        c.assignId(CommentId.of(rs.getLong(1)));
                    }
                }
            }
        }
    }

    private Set<Tag> loadTags(Connection conn, long postId) throws SQLException {
        String sql = """
                SELECT t.id, t.name
                FROM tags t
                JOIN post_tags pt ON pt.tag_id = t.id
                WHERE pt.post_id = ?
                """;
        Set<Tag> tags = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(TagId.of(rs.getLong("id")), rs.getString("name")));
            }
        }
        return tags;
    }

    private void persistTags(Connection conn, Post post) throws SQLException {
        // insert tags if not exist
        try (PreparedStatement insertTag = conn.prepareStatement(
                "INSERT INTO tags(name) VALUES (?) ON CONFLICT(name) DO NOTHING")) {
            for (Tag tag : post.getTags()) {
                insertTag.setString(1, tag.getName());
                insertTag.addBatch();
            }
            insertTag.executeBatch();
        }
        // delete old post_tags
        try (PreparedStatement deleteLinks = conn.prepareStatement(
                "DELETE FROM post_tags WHERE post_id = ?")) {
            deleteLinks.setLong(1, post.getId().getValue());
            deleteLinks.executeUpdate();
        }
        // insert new post_tags links
        try (PreparedStatement insertLinks = conn.prepareStatement(
                "INSERT INTO post_tags(post_id, tag_id) SELECT ?, t.id FROM tags t WHERE t.name = ?")) {
            for (Tag tag : post.getTags()) {
                insertLinks.setLong(1, post.getId().getValue());
                insertLinks.setString(2, tag.getName());
                insertLinks.addBatch();
            }
            insertLinks.executeBatch();
        }
    }

    private Image loadImage(Connection conn, long postId) throws SQLException {
        String sql = "SELECT url FROM images WHERE post_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return Image.create(rs.getString("url"));
        }
    }

    private void persistImage(Connection conn, Post post) throws SQLException {

        try (PreparedStatement delete = conn.prepareStatement("DELETE FROM images WHERE post_id = ?")) {
            delete.setLong(1, post.getId().getValue());
            delete.executeUpdate();
        }
        if (post.getImage().isPresent()) {
            Image image = post.getImage().get();
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO images(post_id, url) VALUES (?, ?)")) {
                insert.setLong(1, post.getId().getValue());
                insert.setString(2, image.getUrl());
                insert.executeUpdate();
            }
        }
    }

    @Override
    public long countBy(PostSearchCriteria criteria) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM posts p
                LEFT JOIN post_tags pt ON pt.post_id = p.id
                LEFT JOIN tags t ON t.id = pt.tag_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        fillParams(params, criteria, sql);
        final Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Post> findBy(PostSearchCriteria criteria, PageRequest page) {

        StringBuilder sql = new StringBuilder("""
               SELECT p.id AS post_id, p.title, p.text, p.likes_count, p.comments_count,
                      p.created_at, p.updated_at,
                      json_agg(json_build_object('id', t.id, 'name', t.name)) AS tags
               FROM posts p
                         LEFT JOIN post_tags pt ON pt.post_id = p.id
                         LEFT JOIN tags t ON t.id = pt.tag_id
               WHERE 1=1""");

        List<Object> params = new ArrayList<>();
        fillParams(params, criteria, sql);
        sql.append(" GROUP BY p.id");
        if (page != null) {
            sql.append(" ORDER BY p.created_at DESC LIMIT ? OFFSET ?");
            params.add(page.size());
            params.add(page.offset());
        }
        final List<Post> posts = new ArrayList<>();
        final Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(String.valueOf(sql))) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PostId postId = PostId.of(rs.getLong("post_id"));
                posts.add(Post.fromDatabase(
                        postId,
                        rs.getString("title"),
                        rs.getString("text"),
                        rs.getInt("comments_count"),
                        rs.getInt("likes_count"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        null,
                        List.of(),
                        extracted(rs.getString("tags"))
                ));
            }
            return posts;

        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<Tag> extracted(String tagsJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> tagList = mapper.readValue(tagsJson, new TypeReference<>() {});
        return tagList.stream()
                .filter(m -> m.get("id") != null)
                .map(m -> new Tag(
                        TagId.of(((Number) m.get("id")).longValue()),
                        (String) m.get("name")
                ))
                .collect(Collectors.toSet());
    }

    private void fillParams(List<Object> params, PostSearchCriteria criteria, StringBuilder sql) {

        if (criteria.title() != null && !criteria.title().isEmpty()) {
                sql.append(" AND (");
                criteria.title().forEach(s -> {
                    if (sql.charAt(sql.length() - 1) != '(') {
                        sql.append(" OR ");
                    }
                    sql.append("p.title LIKE ?");
                    params.add("%" + s + "%");
                });
                sql.append(")");
        }
        if (!criteria.tags().isEmpty()) {
            sql.append(" AND t.name IN (")
                    .append("?, ".repeat(criteria.tags().size() - 1))
                    .append("?)");
            criteria.tags().forEach(t -> params.add(t.value()));
        }
    }
}