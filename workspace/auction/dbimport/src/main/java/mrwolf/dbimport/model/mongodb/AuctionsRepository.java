package mrwolf.dbimport.model.mongodb;

import mrwolf.dbimport.model.AuctionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuctionsRepository extends MongoRepository<AuctionRecord, String> {

  public final static String COLLECTION_NAME = "auctions";

}
