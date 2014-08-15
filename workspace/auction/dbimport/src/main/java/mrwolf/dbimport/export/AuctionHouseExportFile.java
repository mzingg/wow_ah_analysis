package mrwolf.dbimport.export;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.File;

@Document
@Data
@Accessors(fluent = true)
public class AuctionHouseExportFile {
  @Id
  @NonNull
  private final String snapshotHash;

  private long snapshotTime;

  @Transient
  private File file;
}
