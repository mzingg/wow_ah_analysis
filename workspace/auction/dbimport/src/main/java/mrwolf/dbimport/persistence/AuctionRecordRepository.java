package mrwolf.dbimport.persistence;

import mrwolf.dbimport.model.AuctionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuctionRecordRepository extends MongoRepository<AuctionRecord, String> {

  public final static String COLLECTION_NAME = "auctions";

  AuctionRecord findByAuctionId(int auctionId);

}
