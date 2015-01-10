package mrwolf.dbimport.executors;

import mrwolf.dbimport.persistence.PersistenceException;

public class AuctionPersistor implements Runnable {

  private AuctionProcessDispatcher dispatcher;

  private int batchSize;

  public AuctionPersistor(AuctionProcessDispatcher dispatcher, int batchSize) {
    this.dispatcher = dispatcher;
    this.batchSize = batchSize;
  }

  @Override
  public void run() {
    while (!dispatcher.persistorShouldTerminate()) {
      try {
        dispatcher.auctionRepository().save(dispatcher.pollAuctions(batchSize));
      } catch (PersistenceException e) {
        dispatcher.pushError(e);
      }
    }
  }

}
