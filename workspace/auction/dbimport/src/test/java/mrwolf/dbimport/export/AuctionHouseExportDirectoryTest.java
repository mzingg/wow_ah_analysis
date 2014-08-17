package mrwolf.dbimport.export;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuctionHouseExportDirectoryTest {

  private static final String RESOURCES_DIR = StringUtils.join(new String[] {"workspace", "auction", "dbimport", "src", "test", "resources"}, File.separator);

  @Test(expected = AuctionHouseExportException.class)
  public void testCtorWithInvalidDirectoryThrowsException() throws Exception {
    new AuctionHouseExportDirectory("somePath");
  }

  @Test
  public void testCtorWithEmptyDirReturnsEmptyRecordList() throws Exception {
    AuctionHouseExportDirectory testObj = new AuctionHouseExportDirectory(aTestPath("emptyDir"));
    assertNotNull(testObj);
  }

  @Test
  public void testFileIteratorReturnsCorrectResult() throws Exception {
    AuctionHouseExportDirectory testObj = new AuctionHouseExportDirectory(aTestPath("fetchah"));

    List<AuctionHouseExportFile> result = testObj.fileList();

    assertEquals(4, result.size());
  }

  private String aTestPath(String... relativePart) {
    return RESOURCES_DIR + File.separator + StringUtils.join(relativePart, File.separator);
  }
}