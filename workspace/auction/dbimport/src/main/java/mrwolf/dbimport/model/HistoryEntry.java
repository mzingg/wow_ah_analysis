package mrwolf.dbimport.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class HistoryEntry {

  @Id
  @Getter
  private String id;

  @Getter
  @Setter
  private int numberOfTransactions;

  @Getter
  @Setter
  private long avgBuyout;

  @Getter
  @Setter
  private long avg;
}
