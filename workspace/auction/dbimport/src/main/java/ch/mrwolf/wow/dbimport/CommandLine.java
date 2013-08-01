package ch.mrwolf.wow.dbimport;

import java.lang.management.ManagementFactory;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.mrwolf.wow.dbimport.io.AuctionExportReader;

@CommonsLog
public class CommandLine {

  private static final String SPRING_CONFIG_FILENAME = "application.xml";

  public static void main(final String... arguments) {

    log.info(ManagementFactory.getRuntimeMXBean().getName());

    try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(SPRING_CONFIG_FILENAME)) {
      AuctionExportReader reader = context.getBean(AuctionExportReader.class);

      reader.read();
    }

  }
}
