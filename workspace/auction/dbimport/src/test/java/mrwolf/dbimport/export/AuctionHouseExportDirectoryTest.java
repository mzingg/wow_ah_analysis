package mrwolf.dbimport.export;

import org.junit.Test;

import java.util.List;

import static mrwolf.TestUtil.aTestPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuctionHouseExportDirectoryTest {

  @Test(expected = AuctionHouseExportException.class)
  public void testCtorWithInvalidDirectoryThrowsException() throws Exception {
    new AuctionHouseExportDirectory("somePath");
  }

  @Test
  public void testCtorWithEmptyDirReturnsEmptyRecordList() throws Exception {
    AuctionHouseExportDirectory testObj = new AuctionHouseExportDirectory(aTestPath("export/emptyDir"));

    assertEquals(0, testObj.fileList().size());
  }

  @Test
  public void testInvalidFileNameisIgnored() throws Exception {
    AuctionHouseExportDirectory testObj = new AuctionHouseExportDirectory(aTestPath("export/invalidFileName"));

    assertEquals(0, testObj.fileList().size());
  }

  @Test
  public void testFileListReturnsCorrectOrderedResult() throws Exception {
    AuctionHouseExportDirectory testObj = new AuctionHouseExportDirectory(aTestPath("export/fetchah"));

    List<AuctionHouseExportFile> actual = testObj.fileList();

    assertEquals(10, actual.size());
    assertEquals("20140701003001.json.bz2", actual.get(0).file().getName());
    assertEquals("20140701053001.json.bz2", actual.get(5).file().getName());
    assertEquals("20140815083001.json.bz2", actual.get(8).file().getName());
    assertEquals("20140815093001.json.bz2", actual.get(9).file().getName());
  }

}