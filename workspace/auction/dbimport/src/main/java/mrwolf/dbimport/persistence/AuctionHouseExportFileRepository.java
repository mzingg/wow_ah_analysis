package mrwolf.dbimport.persistence;

import mrwolf.dbimport.export.AuctionHouseExportFile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuctionHouseExportFileRepository extends MongoRepository<AuctionHouseExportFile, String> {

}
