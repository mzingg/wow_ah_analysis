package mrwolf.dbimport.export;

import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ExportDirectory {

  @NonNull
  private final File inputDirectory;

  public ExportDirectory(String path) throws AuctionHouseExportException {
    this.inputDirectory = checkInputDirectory(path);
  }

  private File checkInputDirectory(String path) throws AuctionHouseExportException {
    File result = new File(path);
    if (result.exists() && result.canRead() && result.isDirectory()) {
      return result;
    }

    if (!result.exists()) {
      try {
        FileUtils.forceMkdir(result);
        return result;
      } catch (IOException e) {
        // fall through
      }
    }

    throw new AuctionHouseExportException("Could not access (or create) input directory [" + path + "]");
  }

}
