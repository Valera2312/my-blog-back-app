package ru.valera.infrastructure.persistance.jdbc;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.valera.domain.post.PostId;
import ru.valera.domain.repository.PostQueryRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
    public int countComments(PostId postId) {
        String sql = "SELECT COUNT(*) FROM comments WHERE post_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, postId.getValue());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
