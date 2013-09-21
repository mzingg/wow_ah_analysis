package ch.mrwolf.wow.dbimport.io;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractJdbcProcessingStateCallback extends AbstractProcessingStateCallback {

  @Getter(AccessLevel.PROTECTED)
  private JdbcTemplate jdbcTemplate; // NOPMD

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private String tableName; // NOPMD

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private String consolidatedTableName; // NOPMD

  public void setDataSource(final DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Override
  public void close() {
    super.close();
    recreateConsolidatedTabele();
  }

  private void recreateConsolidatedTabele() {
    if (dropConsolidatedTable()) {
      createConsolidatedTable();
    }
  }

  protected abstract boolean dropConsolidatedTable();

  protected abstract void createConsolidatedTable();

}
