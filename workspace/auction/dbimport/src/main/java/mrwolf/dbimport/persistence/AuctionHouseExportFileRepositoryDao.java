package mrwolf.dbimport.persistence;

import mrwolf.dbimport.export.AuctionHouseExportFile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

public class AuctionHouseExportFileRepositoryDao implements AuctionHouseExportFileRepository {

  private JdbcTemplate jdbcTemplate;

  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Override
  public List<AuctionHouseExportFile> findAll() throws PersistenceException {
    return jdbcTemplate.query("SELECT * FROM auction_export_file", (rs, rowNum) -> new AuctionHouseExportFile(rs.getLong("snapshotHash")).snapshotTime(rs.getLong("snapshotTime")));
  }

  @Override
  public void save(AuctionHouseExportFile entity) throws PersistenceException {
    jdbcTemplate.update("INSERT INTO auction_export_file VALUES (?, ?)", entity.snapshotHash(), entity.snapshotTime());
  }
}
