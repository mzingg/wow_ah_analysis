package ch.mrwolf.wow.dbimport;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.mrwolf.wow.dbimport.io.AuctionExportReader;

public class CommandLine {

  private static final String SPRING_CONFIG_FILENAME = "application.xml";

  public static void main(final String... arguments) {

    try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(SPRING_CONFIG_FILENAME)) {
      AuctionExportReader reader = context.getBean(AuctionExportReader.class);

      reader.read();
    }

  }
}
