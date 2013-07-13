package ch.mrwolf.wow.dbimport.io.items;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import org.springframework.jdbc.core.JdbcTemplate;

public class ItemImporter {

  @Getter(AccessLevel.PROTECTED)
  private JdbcTemplate jdbcTemplate;

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private String auctionTableName;

  @Setter
  @Getter(AccessLevel.PROTECTED)
  private String itemTableName;

  public void setDataSource(final DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

}
