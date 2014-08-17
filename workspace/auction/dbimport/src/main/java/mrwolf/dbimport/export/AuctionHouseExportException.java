package mrwolf.dbimport.export;

import java.io.IOException;

public class AuctionHouseExportException extends Exception {

  public AuctionHouseExportException(String message) {
    super(message);
  }

  public AuctionHouseExportException(Throwable cause) {
    super(cause);
  }
}
