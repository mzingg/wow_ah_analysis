package ch.mrwolf.wow.dbimport.model.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import ch.mrwolf.wow.dbimport.model.AuctionRecord;

public interface AuctionsRepository extends MongoRepository<AuctionRecord, String> {

  public final static String COLLECTION_NAME = "auctions";

}
