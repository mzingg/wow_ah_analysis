package ch.mrwolf.wow.dbimport.io;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

public class AbstractJdbcProcessingStateCallback extends AbstractProcessingStateCallback {

  @Getter(AccessLevel.PROTECTED)
  private JdbcTemplate jdbcTemplate;

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private String tableName;

  public void setDataSource(final DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

}
