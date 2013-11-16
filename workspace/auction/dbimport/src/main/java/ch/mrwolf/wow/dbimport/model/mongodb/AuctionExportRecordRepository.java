package ch.mrwolf.wow.dbimport.model.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;

public interface AuctionExportRecordRepository extends MongoRepository<AuctionExportRecord, String> {

  public final static String COLLECTION_NAME = "auctionExportRecord";

  public AuctionExportRecord findByFaction(final int faction);

}
