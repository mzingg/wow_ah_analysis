package ch.mrwolf.wow.dbimport.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

import org.springframework.util.StringUtils;

@CommonsLog
public class FilesystemProcessingStateCallback extends AbstractProcessingStateCallback implements Serializable {

  private static final long serialVersionUID = 1L;

  @Setter
  private transient String statusFilename;

  @Override
  public void init() {
    loadState();

    super.init();
  }

  private void loadState() {
    if (StringUtils.isEmpty(statusFilename)) {
      log.error("No status file configured.");
      return;
    }

    final File statusFile = new File(statusFilename);
    if (!statusFile.exists() || !statusFile.canRead() || statusFile.length() == 0) {
      return;
    }

    try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(statusFile))) {
      final Object persistedObject = inputStream.readObject();
      if (persistedObject instanceof FilesystemProcessingStateCallback) {
        FilesystemProcessingStateCallback persistedState = (FilesystemProcessingStateCallback) persistedObject;
        setProcessedFiles(persistedState.getProcessedFiles());
        setProcessedRecords(persistedState.getProcessedRecords());
        setFileCount(persistedState.getFileCount());
        setRecordCount(persistedState.getRecordCount());
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    } catch (ClassNotFoundException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    persistState();
  }

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {
    super.afterFile(file, snapshotTime, snapshotMd5Hash);

    persistState();
  }

  private void persistState() {
    if (StringUtils.isEmpty(statusFilename)) {
      log.error("No status file configured.");
      return;
    }

    final File statusFile = new File(statusFilename);
    if (!statusFile.exists()) {
      try {
        statusFile.createNewFile();
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }
    }

    try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(statusFile))) {
      outputStream.writeObject(this);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

}
