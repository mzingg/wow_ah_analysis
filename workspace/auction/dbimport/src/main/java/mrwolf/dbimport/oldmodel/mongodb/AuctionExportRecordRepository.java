package mrwolf.dbimport.oldmodel.mongodb;

import mrwolf.dbimport.export.AuctionHouseExportRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuctionExportRecordRepository extends MongoRepository<AuctionHouseExportRecord, String> {

  public final static String COLLECTION_NAME = "auctionExportRecord";

  public AuctionHouseExportRecord findByFaction(final int faction);

}