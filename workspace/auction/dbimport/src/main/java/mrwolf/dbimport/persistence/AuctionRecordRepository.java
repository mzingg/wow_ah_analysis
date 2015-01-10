package mrwolf.dbimport.persistence;

import mrwolf.dbimport.model.AuctionRecord;

import java.util.List;

public interface AuctionRecordRepository {

  AuctionRecord findByAuctionId(int auctionId) throws PersistenceException;

  void save(List<AuctionRecord> auctionRecords) throws PersistenceException;
}
