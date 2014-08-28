package mrwolf.dbimport;

import mrwolf.dbimport.executors.AuctionProcessDispatcher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Starter {

  private static final String SPRING_CONFIG_FILENAME = "application.xml";

  public static void main(final String... arguments) {

    try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(SPRING_CONFIG_FILENAME)) {
      context.getBean("dispatcher", AuctionProcessDispatcher.class).start();
    }

  }
}
