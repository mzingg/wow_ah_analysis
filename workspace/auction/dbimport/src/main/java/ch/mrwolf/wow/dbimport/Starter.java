package ch.mrwolf.wow.dbimport;

import ch.mrwolf.wow.dbimport.io.AuctionExportReader;
import ch.mrwolf.wow.dbimport.tasks.TaskList;
import org.apache.commons.cli.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Starter {

  private static final String OPTION_SKIP_READER = "skipReader";
  private static final String SPRING_CONFIG_FILENAME = "application.xml";

  public static void main(final String... arguments) {

    final CommandLine cmd = tryParseCommandline(arguments);
    if (cmd == null) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Starter", setupOptions());
      return;
    }

    try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(SPRING_CONFIG_FILENAME)) {
      final TaskList setupTaskList = context.getBean("setup", TaskList.class);
      setupTaskList.executeTasks();

      String readerName = "defaultReader";
      if (cmd.hasOption(OPTION_SKIP_READER)) {
        readerName = "nopReader";
      }
      final AuctionExportReader reader = context.getBean(readerName, AuctionExportReader.class);
      reader.read();

      final TaskList cleanupTaskList = context.getBean("cleanup", TaskList.class);
      cleanupTaskList.executeTasks();
    }

  }

  private static CommandLine tryParseCommandline(final String[] arguments) {
    final CommandLineParser parser = new BasicParser();
    final Options options = setupOptions();
    try {
      return parser.parse(options, arguments);
    } catch (ParseException e) {
      return null;
    }
  }

  private static Options setupOptions() {
    final Options result = new Options();

    result.addOption(OPTION_SKIP_READER, false, "Skips the auction file processing. Only performing the setup and cleanup tasks");

    return result;
  }
}
