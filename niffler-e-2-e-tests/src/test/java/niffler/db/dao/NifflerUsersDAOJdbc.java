package niffler.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;

import niffler.db.DataSourceProvider;
import niffler.db.ServiceDB;
import niffler.db.entity.AuthorityEntity;
import niffler.db.entity.UserEntity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class NifflerUsersDAOJdbc implements NifflerUsersDAO {

    private static final DataSource ds = DataSourceProvider.INSTANCE.getDataSource(ServiceDB.NIFFLER_AUTH);

    @Override
    public int createUser(UserEntity user) {
        int executeUpdate;

        final String CREATE_USER_QUERY = "INSERT INTO users "
                                   + "(username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired) "
                                   + " VALUES (?, ?, ?, ?, ?, ?)";

        final String CREATE_AUTHORITY_QUERY = "INSERT INTO authorities (user_id, authority) VALUES (?, ?)";

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement insertUserSt = conn.prepareStatement(CREATE_USER_QUERY, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement insertAuthoritySt = conn.prepareStatement(CREATE_AUTHORITY_QUERY)) {
                insertUserSt.setString(1, user.getUsername());
                insertUserSt.setString(2, pe.encode(user.getPassword()));
                insertUserSt.setBoolean(3, user.getEnabled());
                insertUserSt.setBoolean(4, user.getAccountNonExpired());
                insertUserSt.setBoolean(5, user.getAccountNonLocked());
                insertUserSt.setBoolean(6, user.getCredentialsNonExpired());
                executeUpdate = insertUserSt.executeUpdate();

                final UUID finalUserId;

                try (ResultSet generatedKeys = insertUserSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        finalUserId = UUID.fromString(generatedKeys.getString(1));
                        user.setId(finalUserId);
                    } else {
                        throw new SQLException("Creating user failed, no ID present");
                    }
                }

                for (AuthorityEntity authority : user.getAuthorities()) {
                    insertAuthoritySt.setObject(1, finalUserId);
                    insertAuthoritySt.setString(2, authority.getAuthority().name());
                    insertAuthoritySt.addBatch();
                    insertAuthoritySt.clearParameters();
                }
                insertAuthoritySt.executeBatch();
            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw new RuntimeException(e);
            }

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return executeUpdate;
    }

    @Override
    public String getUserId(String userName) {
        String selectUserId = "SELECT * from users where username = ?";
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectUserId)) {
            statement.setString(1, userName);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            } else {
                throw new IllegalArgumentException("Can`t find user by given username: " + userName);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int removeUser(UserEntity user) {
        int executeUpdate;

        try (Connection conn = ds.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement deleteUserSt = conn.prepareStatement("DELETE FROM users WHERE id = ?");
                 PreparedStatement deleteAuthoritySt = conn.prepareStatement(
                         "DELETE FROM authorities WHERE user_id = ?")) {
                deleteUserSt.setObject(1, user.getId());
                deleteAuthoritySt.setObject(1, user.getId());

                deleteAuthoritySt.executeUpdate();
                executeUpdate = deleteUserSt.executeUpdate();

            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw new RuntimeException(e);
            }

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return executeUpdate;
    }
    /*
    @Override
    public void removeUser(UserEntity user) {
        String userId = getUserId(user.getUsername());

        String deleteUser = "DELETE FROM users WHERE id =?";
        String deleteAuthority = "DELETE FROM authorities WHERE user_id = ?";

        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement usersSt = conn.prepareStatement(deleteUser);
                 PreparedStatement authoritiesSt = conn.prepareStatement(deleteAuthority)) {

                authoritiesSt.setObject(1, UUID.fromString(userId));
                authoritiesSt.executeUpdate();

                usersSt.setObject(1, UUID.fromString(userId));
                usersSt.executeUpdate();

                //conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    throw new RuntimeException(e);
                }
                catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    */


    @Override
    public int updateUser(UserEntity user) {
        int executeUpdate;

        try (Connection conn = ds.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement updateUserSt = conn.prepareStatement("UPDATE users SET"
                    + "(username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired) "
                    + " VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement updateAuthoritySt = conn.prepareStatement(
                         "INSERT INTO authorities (user_id, authority) VALUES (?, ?)")) {
                updateUserSt.setString(1, user.getUsername());
                updateUserSt.setString(2, pe.encode(user.getPassword()));
                updateUserSt.setBoolean(3, user.getEnabled());
                updateUserSt.setBoolean(4, user.getAccountNonExpired());
                updateUserSt.setBoolean(5, user.getAccountNonLocked());
                updateUserSt.setBoolean(6, user.getCredentialsNonExpired());
                executeUpdate = updateUserSt.executeUpdate();

                final UUID finalUserId;

                try (ResultSet generatedKeys = updateUserSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        finalUserId = UUID.fromString(generatedKeys.getString(1));
                        user.setId(finalUserId);
                    } else {
                        throw new SQLException("Creating user failed, no ID present");
                    }
                }

                for (AuthorityEntity authority : user.getAuthorities()) {
                    updateAuthoritySt.setObject(1, finalUserId);
                    updateAuthoritySt.setString(2, authority.getAuthority().name());
                    updateAuthoritySt.addBatch();
                    updateAuthoritySt.clearParameters();
                }
                updateAuthoritySt.executeBatch();
            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw new RuntimeException(e);
            }

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return executeUpdate;
    }
}
