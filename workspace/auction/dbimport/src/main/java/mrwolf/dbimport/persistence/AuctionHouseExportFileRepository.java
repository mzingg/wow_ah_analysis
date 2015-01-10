package mrwolf.dbimport.persistence;

import mrwolf.dbimport.export.AuctionHouseExportFile;

import java.util.List;

public interface AuctionHouseExportFileRepository {

  List<AuctionHouseExportFile> findAll() throws PersistenceException;

  void save(AuctionHouseExportFile entity)  throws PersistenceException;
}
