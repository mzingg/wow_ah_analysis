package mrwolf.dbimport.executors;

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
      dispatcher.auctionRepository().save(dispatcher.pollAuctions(batchSize));
    }
  }

}
