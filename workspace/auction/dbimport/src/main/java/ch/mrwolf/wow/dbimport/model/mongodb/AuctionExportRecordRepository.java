package ch.mrwolf.wow.dbimport.model.mongodb;

import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuctionExportRecordRepository extends MongoRepository<AuctionExportRecord, String> {

  public final static String COLLECTION_NAME = "auctionExportRecord";

  public AuctionExportRecord findByFaction(final int faction);

}