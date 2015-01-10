package mrwolf.dbimport.export;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static java.lang.Integer.parseInt;

@Accessors(fluent = true)
@EqualsAndHashCode(exclude = "inputDirectory")
@ToString
public class AuctionHouseExportDirectory {

  private static final AuctionExportFilenameFilter AUCTION_EXPORT_FILENAME_FILTER = new AuctionExportFilenameFilter();
  @NonNull
  private final File inputDirectory;
  private final Set<AuctionHouseExportFile> fileSet;
  @Getter
  @Setter
  private String id;

  public AuctionHouseExportDirectory(String path) throws AuctionHouseExportException {
    this.inputDirectory = checkInputDirectory(path);
    this.fileSet = new TreeSet<>();
    readDirectoryRecursivly(this.fileSet, this.inputDirectory);
  }

  public List<AuctionHouseExportFile> fileList() {
    return new LinkedList<>(fileSet);
  }

  private File checkInputDirectory(String path) throws AuctionHouseExportException {
    File result = new File(path);
    if (result.exists() && result.canRead() && result.isDirectory()) {
      return result;
    }
    throw new AuctionHouseExportException("Could not read input directory [" + path + "]");
  }

  private void readDirectoryRecursivly(Set<AuctionHouseExportFile> outputList, File parentDirectory) {
    File[] listFiles = parentDirectory.listFiles();
    // lisFiles can be null, so we have to check this case (should however not occure at this point)
    if (listFiles == null) {
      listFiles = new File[0];
    }

    for (File child : listFiles) {
      if (AUCTION_EXPORT_FILENAME_FILTER.accept(parentDirectory, child.getName())) {
        AuctionHouseExportFile cur = new AuctionHouseExportFile(hashOfFile(child)).file(child);
        cur.snapshotTime(AUCTION_EXPORT_FILENAME_FILTER.fileDate().toEpochSecond(ZoneOffset.UTC));
        outputList.add(cur);
      } else if (child.isDirectory() && child.canRead()) {
        readDirectoryRecursivly(outputList, child);
      }
    }
  }

  private long hashOfFile(final File file) {
    /*
    String result = StringUtils.EMPTY;
    try (FileInputStream fileStream = new FileInputStream(file)) {
      result = DigestUtils.sha1Hex(fileStream);
    } catch (IOException e) {
      // fall through
    }
    */

    CRC32 checksum = new CRC32();
    checksum.update(file.getAbsolutePath().getBytes());
    return checksum.getValue();
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
