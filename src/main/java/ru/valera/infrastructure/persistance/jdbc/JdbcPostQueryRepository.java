package ru.valera.infrastructure.persistance.jdbc;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import ru.valera.domain.Image.Image;
import ru.valera.domain.comment.Comment;
import ru.valera.domain.comment.CommentId;
import ru.valera.domain.post.PostId;
import ru.valera.domain.repository.PostQueryRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPostQueryRepository implements PostQueryRepository {

    private final DataSource dataSource;

    public JdbcPostQueryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }

    @Override
    public Comment findComment(PostId postId, CommentId commentId) {
        String sql = "SELECT id, post_id, text FROM comments WHERE post_id = ? AND id = ?";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId.getValue());
            ps.setLong(2, commentId.getValue());
            ResultSet rs = ps.executeQuery();
            rs.next();
            Long id = rs.getLong("id");
            String text = rs.getString("text");
            return Comment.create(CommentId.of(id), text);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Comment> findComments(PostId postId) {
        String sql = "SELECT id, post_id, text FROM comments WHERE post_id = ?";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId.getValue());
            ResultSet rs = ps.executeQuery();

            List<Comment> comments = new ArrayList<>();
            while (rs.next()) {
                Long id = rs.getLong("id");
                String text = rs.getString("text");
                comments.add(Comment.create(CommentId.of(id), text));
            }
            return comments;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Image> findImage(PostId postId) {
        String sql = "SELECT url FROM images WHERE post_id = ?";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId.getValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(Image.create(rs.getString("url")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
