package mrwolf.dbimport.export;

import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

@Document
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(exclude = "inputDirectory")
@ToString
public class AuctionHouseExportDirectory {

  private static final AuctionExportFilenameFilter AUCTION_EXPORT_FILENAME_FILTER = new AuctionExportFilenameFilter();

  @Id
  @Setter
  private String id;

  @Transient
  @NonNull
  private final File inputDirectory;

  private final List<AuctionHouseExportFile> fileList;

  public AuctionHouseExportDirectory(String path) throws AuctionHouseExportException {
    this.inputDirectory = checkInputDirectory(path);
    this.fileList = new LinkedList<>();
    readDirectoryRecursivly(this.fileList, this.inputDirectory);
  }

  private File checkInputDirectory(String path) throws AuctionHouseExportException {
    File result = new File(path);
    if (result.exists() && result.canRead() && result.isDirectory()) {
      return result;
    }
    throw new AuctionHouseExportException("Could not read input directory [" + path + "]");
  }

  private void readDirectoryRecursivly(List<AuctionHouseExportFile> outputList, File parentDirectory) {
    for (File child : parentDirectory.listFiles()) {
      if (AUCTION_EXPORT_FILENAME_FILTER.accept(parentDirectory, child.getName())) {
        AuctionHouseExportFile cur = new AuctionHouseExportFile(md5HashOfFile(child)).file(child);
        cur.snapshotTime(AUCTION_EXPORT_FILENAME_FILTER.fileDate());
        outputList.add(cur);
      } else if (child.isDirectory() && child.canRead()) {
        readDirectoryRecursivly(outputList, child);
      }
    }
  }

  private String md5HashOfFile(final File file) {
    String result;
    try (FileInputStream fileStream = new FileInputStream(file)) {
      result = DigestUtils.md5Hex(fileStream);
    } catch (IOException e) {
      String filePath = file.getAbsolutePath() + File.separator + file.getName();
      filePath = filePath.replace(inputDirectory.getAbsolutePath(), "");
      result = DigestUtils.md5Hex(filePath);
    }

    return result;
  }

  @Getter
  @Accessors(fluent = true)
  private static class AuctionExportFilenameFilter implements FilenameFilter {

    private static final Pattern AUCTION_EXPORT_PATTERN = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})\\.json.bz2");

    private LocalDateTime fileDate;

    @Override
    public boolean accept(File dir, String name) {
      Matcher m = AUCTION_EXPORT_PATTERN.matcher(name);
      if (m.matches()) {
        try {
          fileDate = LocalDateTime.of(parseInt(m.group(1)), parseInt(m.group(2)), parseInt(m.group(3)), parseInt(m.group(4)), parseInt(m.group(5)));
          return true;
        } catch (NumberFormatException ignored) {
          // fall through
        }
      }
      return false;
    }
  }
}
