package mrwolf.dbimport.persistence;

import mrwolf.dbimport.model.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class AuctionRecordRepositoryDao implements AuctionRecordRepository {

  private JdbcTemplate jdbcTemplate;

  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Override
  public AuctionRecord findByAuctionId(int auctionId) throws PersistenceException {
    try {
      AuctionRecord result = jdbcTemplate.queryForObject(
          "SELECT * FROM auction_record WHERE \"auctionId\"=?",
          (rs, rowNum) -> {
            AuctionRecord record = new AuctionRecord();
            record.auctionId(rs.getInt("auctionId")).faction(Faction.byId(rs.getByte("faction"))).realm(rs.getString("realm"));
            record.itemId(rs.getInt("itemId")).buyoutAmount(rs.getInt("buyoutAmount")).quantity(rs.getInt("quantity"));
            record.petSpeciesId(rs.getInt("petSpeciesId")).petBreedId(rs.getInt("petBreedId")).petLevel(rs.getInt("petLevel")).petQualityId(rs.getInt("petQualityId"));
            record.lastDuration(AuctionDuration.byId(rs.getByte("lastDuration"))).status(AuctionStatus.byId(rs.getByte("status"))).lastOccurence(rs.getLong("lastOccurence"));

            return record;
          },
          auctionId
      );

      result.update(jdbcTemplate.query(
          "SELECT * FROM bid_history WHERE \"auctionId\"=?",
          (rs, rowNum) -> new BidHistoryEntry(auctionId, rs.getLong("amount"), rs.getLong("timestamp"), AuctionDuration.byId(rs.getByte("duration"))),
          auctionId));

      return result;
    } catch (EmptyResultDataAccessException doesNotExist) {
      return null;
    }
  }

  @Override
  public void save(List<AuctionRecord> auctionRecords) throws PersistenceException {
    if (auctionRecords.size() == 0) {
      return;
    }

    Connection connection;
    try {
        connection = jdbcTemplate.getDataSource().getConnection();
    } catch (SQLException e) {
      throw new PersistenceException(e);
    }
    try {
      connection.setAutoCommit(false);

      PreparedStatement auctionStatement = connection.prepareStatement(
          "INSERT INTO auction_record SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM auction_record WHERE \"auctionId\"=?)"
      );
      for (AuctionRecord record : auctionRecords) {
        auctionStatement.setInt(1, record.auctionId());
        auctionStatement.setInt(2, record.itemId());
        auctionStatement.setLong(3, record.buyoutAmount());
        auctionStatement.setInt(4, record.quantity());
        auctionStatement.setInt(5, record.petSpeciesId());
        auctionStatement.setInt(6, record.petBreedId());
        auctionStatement.setInt(7, record.petLevel());
        auctionStatement.setInt(8, record.petQualityId());
        auctionStatement.setByte(9, record.lastDuration().getDatabaseId());
        auctionStatement.setByte(10, record.status().getDatabaseId());
        auctionStatement.setLong(11, record.lastOccurence());
        auctionStatement.setByte(12, record.faction().getDatabaseId());
        auctionStatement.setString(13, record.realm());
        auctionStatement.setInt(14, record.auctionId());

        auctionStatement.addBatch();
      }

      auctionStatement.executeBatch();
      auctionStatement.clearBatch();
      auctionStatement.close();

      PreparedStatement historyStatement = connection.prepareStatement(
          "INSERT INTO bid_history SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM bid_history WHERE \"key\"=?)"
      );
      for (AuctionRecord record : auctionRecords) {
        for (BidHistoryEntry entry : record.getBidHistoryList()) {
          historyStatement.setString(1, entry.key());
          historyStatement.setInt(2, record.auctionId());
          historyStatement.setLong(3, entry.amount());
          historyStatement.setLong(4, entry.timestamp());
          historyStatement.setByte(5, entry.duration().getDatabaseId());
          historyStatement.setString(6, entry.key());

          historyStatement.addBatch();
        }
      }

      historyStatement.executeBatch();
      historyStatement.clearBatch();
      historyStatement.close();

      connection.commit();
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException ignored) {
      }
      throw new PersistenceException(e);
    } finally {
      try {
        connection.setAutoCommit(true);
      } catch (SQLException ignored) {
      }
    }
  }
}
