package mrwolf.dbimport.io.mongodb;

import mrwolf.dbimport.io.AsyncQueue;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.data.mongodb.core.MongoTemplate;

public abstract class MongoAsyncQueue<T> extends AsyncQueue<T> {

  @Getter(AccessLevel.PROTECTED)
  private final MongoTemplate mongoTemplate;

  public MongoAsyncQueue(final int batchSize, final MongoTemplate mongoTemplate) {
    super(batchSize);
    this.mongoTemplate = mongoTemplate;
  }

}
