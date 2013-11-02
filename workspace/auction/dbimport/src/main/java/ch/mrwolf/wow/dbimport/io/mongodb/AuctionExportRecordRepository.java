package ch.mrwolf.wow.dbimport.io.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import ch.mrwolf.wow.dbimport.model.AuctionExportRecord;

public interface AuctionExportRecordRepository extends MongoRepository<AuctionExportRecord, String> {

  public AuctionExportRecord findByFaction(final int faction);

}
