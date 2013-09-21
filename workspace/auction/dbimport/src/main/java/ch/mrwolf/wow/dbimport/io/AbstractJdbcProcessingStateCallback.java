package ch.mrwolf.wow.dbimport.io;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractJdbcProcessingStateCallback extends NopProcessingStateCallback {

  @Getter(AccessLevel.PROTECTED)
  private JdbcTemplate jdbcTemplate; // NOPMD

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private String snapshotTableName; // NOPMD

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private String fileLogTableName; // NOPMD

  public void setDataSource(final DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

}
