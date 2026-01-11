package ru.valera.infrastructure.persistance.jdbc;

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
                SELECT id, title, text, likes_count, created_at, updated_at
                FROM posts
                ORDER BY created_at DESC
                """;

        List<Post> posts = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PostId postId = PostId.of(rs.getLong("id"));
                posts.add(Post.fromDatabase(
                        postId,
                        rs.getString("title"),
                        rs.getString("text"),
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
        try (Connection conn = getConnection()) {

            Post base = loadPost(conn, id.getValue());
            if (base == null) return Optional.empty();

            List<Comment> comments = loadComments(conn, id.getValue());
            Set<Tag> tags = loadTags(conn, id.getValue());
            //Image image = loadImage(conn, id.getValue());

            return Optional.of(Post.fromDatabase(
                    id,
                    base.getTitle(),
                    base.getText(),
                    base.getLikesCount(),
                    base.getCreatedAt(),
                    base.getUpdatedAt(),
                    null,
                    List.of(),
                    tags
            ));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Post save(Post post) {
        try (Connection conn = getConnection()) {
            if (post.getId() == null) {
                long id = insertPost(conn, post);
                return findById(PostId.of(id)).orElseThrow();
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
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id.getValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------- Post CRUD ----------------
    private long insertPost(Connection conn, Post post) throws SQLException {
        String sql = """
                INSERT INTO posts (title, text, likes_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getText());
            ps.setInt(3, post.getLikesCount());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreatedAt()));
            ps.setTimestamp(5, Timestamp.valueOf(post.getUpdatedAt()));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            return keys.getLong(1);
        }
    }

    private void updatePost(Connection conn, Post post) throws SQLException {
        String sql = """
                UPDATE posts
                SET title = ?, text = ?, likes_count = ?, updated_at = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getText());
            ps.setInt(3, post.getLikesCount());
            ps.setTimestamp(4, Timestamp.valueOf(post.getUpdatedAt()));
            ps.setLong(5, post.getId().getValue());
            ps.executeUpdate();
        }
    }

    private Post loadPost(Connection conn, long id) throws SQLException {
        String sql = """
                SELECT title, text, likes_count, created_at, updated_at
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
        try (PreparedStatement delete = conn.prepareStatement("DELETE FROM comments WHERE post_id = ?")) {
            delete.setLong(1, post.getId().getValue());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO comments(id, post_id, text) VALUES (?, ?, ?)")) {
            for (Comment c : post.getComments()) {
                insert.setLong(1, c.getId().getValue());
                insert.setLong(2, post.getId().getValue());
                insert.setString(3, c.getText());
                insert.addBatch();
            }
            insert.executeBatch();
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
        if (post.getCoverImage().isPresent()) {
            Image image = post.getCoverImage().get();
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
                SELECT COUNT(*) FROM comments p
                LEFT JOIN post_tags pt ON pt.post_id = p.id
                LEFT JOIN tags t ON t.id = pt.tag_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        fillParams(params, criteria, sql);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

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
            SELECT DISTINCT p.id, p.title, p.text, p.likes_count, p.created_at, p.updated_at
            FROM posts p
            LEFT JOIN post_tags pt ON pt.post_id = p.id
            LEFT JOIN tags t ON t.id = pt.tag_id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        fillParams(params, criteria, sql);

        if (page != null) {
            sql.append(" ORDER BY p.created_at DESC LIMIT ? OFFSET ?");
            params.add(page.page());
            params.add(page.size());
        }

        final List<Post> posts = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PostId postId = PostId.of(rs.getLong("id"));
                posts.add(Post.fromDatabase(
                        postId,
                        rs.getString("title"),
                        rs.getString("text"),
                        rs.getInt("likes_count"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        null,
                        List.of(),
                        Set.of()
                ));
            }
            return posts;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillParams(List<Object> params, PostSearchCriteria criteria, StringBuilder sql) {

        if (!criteria.title().isEmpty()) {
            sql.append(" AND t.name IN (")
                    .append("?, ".repeat(criteria.tags().size() - 1))
                    .append("?)");
            criteria.tags().forEach(t -> params.add(t.value()));
        }

        if (!criteria.tags().isEmpty()) {
            sql.append(" AND t.name IN (")
                    .append("?, ".repeat(criteria.tags().size() - 1))
                    .append("?)");
            criteria.tags().forEach(t -> params.add(t.value()));
        }
    }
}