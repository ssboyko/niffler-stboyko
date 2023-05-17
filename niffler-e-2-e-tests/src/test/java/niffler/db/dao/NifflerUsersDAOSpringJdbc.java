package niffler.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import javax.sql.DataSource;
import niffler.db.DataSourceProvider;
import niffler.db.ServiceDB;
import niffler.db.entity.AuthorityEntity;
import niffler.db.entity.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

public class NifflerUsersDAOSpringJdbc implements NifflerUsersDAO {

  private final TransactionTemplate transactionTemplate;
  private final JdbcTemplate jdbcTemplate;

  public NifflerUsersDAOSpringJdbc() {
    DataSourceTransactionManager transactionManager = new JdbcTransactionManager(
        DataSourceProvider.INSTANCE.getDataSource(ServiceDB.NIFFLER_AUTH));
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(transactionManager.getDataSource()));
  }

  @Override
  public int createUser(UserEntity user) {
    SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
    Map<String, Object> newUser = new HashMap<>();
    newUser.put("username", user.getUsername());
    newUser.put("password", pe.encode(user.getPassword()));
    newUser.put("enabled", user.getEnabled());
    newUser.put("account_non_expired", user.getAccountNonExpired());
    newUser.put("account_non_locked", user.getAccountNonLocked());
    newUser.put("credentials_non_expired", user.getCredentialsNonExpired());

    KeyHolder keyHolder = simpleJdbcInsert.withTableName("users")
            .usingColumns("username", "password", "enabled", "account_non_expired", "account_non_locked", "credentials_non_expired")
            .usingGeneratedKeyColumns("id")
            .withoutTableColumnMetaDataAccess()
            .executeAndReturnKeyHolder(newUser);
    UUID id = (UUID)keyHolder.getKeys().get("id");
    user.setId(id);

    SimpleJdbcInsert simpleJdbcInsert2 = new SimpleJdbcInsert(jdbcTemplate)
            .withTableName("authorities")
            .usingColumns("user_id", "authority")
            .usingGeneratedKeyColumns("id");


    MapSqlParameterSource entry = new MapSqlParameterSource();
    for (AuthorityEntity authority : user.getAuthorities()) {
      entry.addValue("user_id", id)
              .addValue("authority", authority.getAuthority().name());
    }

    simpleJdbcInsert2.executeBatch(entry);

    return 0;
  }

  @Override
  public int updateUser(UserEntity user) {
    return jdbcTemplate.update("UPDATE users SET username = ?, password = ?, enabled = ?, account_non_expired =?, " +
                    "account_non_locked =?, credentials_non_expired = ? where username = ?",
            user.getUsername(),
            user.getPassword(),
            user.getEnabled(),
            user.getAccountNonExpired(),
            user.getAccountNonLocked(),
            user.getCredentialsNonExpired(),
            user.getUsername());
  }

  @Override
  public String getUserId(String userName) {
    return jdbcTemplate.query("SELECT * FROM users WHERE username = ?",
        rs -> {return rs.getString(1);},
        userName
    );
  }

  @Override
  public int removeUser(UserEntity user) {
    return transactionTemplate.execute(st -> {
      jdbcTemplate.update("DELETE FROM authorities WHERE user_id = ?", user.getId());
      return jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId());
    });
  }
}
