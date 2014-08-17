package mrwolf;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class TestUtil {

  private static final String RESOURCES_DIR = StringUtils.join(new String[]{"workspace", "auction", "dbimport", "src", "test", "resources"}, File.separator);

  public static String aTestPath(String relativePart) {
    return RESOURCES_DIR + File.separator + StringUtils.join(relativePart.split("/"), File.separator);
  }

}
