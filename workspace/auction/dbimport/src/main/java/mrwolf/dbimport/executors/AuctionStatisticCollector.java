package mrwolf.dbimport.executors;

import mrwolf.dbimport.export.AuctionHouseExportException;
import mrwolf.dbimport.export.AuctionHouseExportRecord;
import mrwolf.dbimport.model.AuctionRecord;

import java.util.List;

public class AuctionStatisticCollector implements Runnable {

  private AuctionProcessDispatcher dispatcher;

  public AuctionStatisticCollector(AuctionProcessDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  public void run() {
    while (!dispatcher.collectorsShouldTerminate()) {
      List<AuctionHouseExportRecord> records = dispatcher.popIncoming();
      if (records.size() > 0) {
        int auctionId = records.get(0).auctionId(); // all records have same auctionId
        AuctionRecord target = dispatcher.auctionRepository().findByAuctionId(auctionId);
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
