package mrwolf.dbimport.executors;

import mrwolf.dbimport.export.AuctionHouseExportException;
import mrwolf.dbimport.export.AuctionHouseExportRecord;
import mrwolf.dbimport.model.AuctionRecord;
import mrwolf.dbimport.persistence.PersistenceException;

import java.util.List;

public class AuctionStatisticCollector implements Runnable {

  private AuctionProcessDispatcher dispatcher;

  public AuctionStatisticCollector(AuctionProcessDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  public void run() {
    while (!dispatcher.collectorsShouldTerminate()) {
      synchronized (this) {
        while (!dispatcher.pushToPersistIsAllowed()) {
          try {
            wait();
          } catch (InterruptedException e) {
            dispatcher.pushError(e);
          }
        }
      }
      List<AuctionHouseExportRecord> records = dispatcher.popIncoming();
      if (records.size() > 0) {
        int auctionId = records.get(0).auctionId(); // all records have same auctionId
        AuctionRecord target = null;
        try {
          target = dispatcher.auctionRepository().findByAuctionId(auctionId);
        } catch (PersistenceException e) {
          dispatcher.pushError(e);
        }
        target = target != null ? target : new AuctionRecord();
        for (AuctionHouseExportRecord record : records) {
          try {
            target.update(record);
          } catch (AuctionHouseExportException e) {
            dispatcher.pushError(e);
          }
        }
        dispatcher.pushToPersist(target);
      }
    }
  }

}
