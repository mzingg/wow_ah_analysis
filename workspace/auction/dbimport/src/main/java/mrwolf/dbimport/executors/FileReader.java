package mrwolf.dbimport.executors;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import mrwolf.dbimport.export.AuctionHouseExportDirectory;
import mrwolf.dbimport.export.AuctionHouseExportException;
import mrwolf.dbimport.export.AuctionHouseExportFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Accessors(fluent = true)
public class FileReader implements Runnable {

  @NonNull
  private final String directory;

  @NonNull
  private final AuctionProcessDispatcher dispatcher;
  private final Map<Integer, AuctionHouseExportFile> processedFiles;
  @Getter
  private int fileCount;
  @Getter
  private int fileProcessed;

  public FileReader(AuctionProcessDispatcher dispatcher, String directory) {
    this.dispatcher = dispatcher;
    this.directory = directory;
    this.fileCount = -1;
    this.fileProcessed = 0;
    this.processedFiles = new HashMap<>();
  }

  @Override
  public void run() {
    List<AuctionHouseExportFile> existingFiles = dispatcher.fileRepository().findAll();
    try {
      AuctionHouseExportDirectory exportDirectory = new AuctionHouseExportDirectory(directory);
      List<AuctionHouseExportFile> files = exportDirectory.fileList();
      fileCount = files.size() - existingFiles.size();
      int fileId = 1;
      for (AuctionHouseExportFile file : files) {
        if (existingFiles.contains(file)) {
          continue;
        }
        file.read(fileId);
        dispatcher.pushAsIncoming(file.records());
        synchronized (processedFiles) {
          processedFiles.put(fileId, file);
        }
        fileId++;
      }
    } catch (AuctionHouseExportException e) {
      dispatcher.pushError(e);
    }
  }

  public boolean delivered() {
    return processedFiles.size() == 0 && fileCount == fileProcessed;
  }

  public void triggerFileEnd(int fileId) {
    AuctionHouseExportFile entity;
    if (processedFiles.containsKey(fileId)) {
      synchronized (processedFiles) {
        entity = processedFiles.remove(fileId);
      }
      dispatcher.fileRepository().save(entity);
      fileProcessed++;
    }
  }

}
