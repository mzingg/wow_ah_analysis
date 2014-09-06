package mrwolf.dbimport.export;

import mrwolf.dbimport.model.AuctionDuration;
import mrwolf.dbimport.model.Faction;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mrwolf.TestUtil.aTestPath;
import static org.junit.Assert.assertEquals;

public class AuctionHouseExportRecordTest {

  @Test
  public void testRecordIsReadCorrectlyFromFile() throws Exception {
    AuctionHouseExportDirectory testObj = new AuctionHouseExportDirectory(aTestPath("export/fetchah"));

    List<AuctionHouseExportFile> actual = testObj.fileList();

    Map<String, Object> expected = new HashMap<>();
    expected.put("realm", "thrall");
    expected.put("faction", Faction.ALLIANCE);
    expected.put("auctionId", 2002878503);
    expected.put("itemId", 79011);
    expected.put("bidAmount", 179960L);
    expected.put("buyoutAmount", 199960L);
    expected.put("quantity", 20);
    expected.put("timeLeft", AuctionDuration.VERY_LONG);
    expected.put("petSpeciesId", 0);
    expected.put("petBreedId", 0);
    expected.put("petLevel", 0);
    expected.put("petQualityId", 0);
    assertRecord(actual.get(0), 74519, 0, expected);

    expected.put("realm", "thrall");
    expected.put("faction", Faction.HORDE);
    expected.put("auctionId", 2003065431);
    expected.put("itemId", 82800);
    expected.put("bidAmount", 2999999L);
    expected.put("buyoutAmount", 4299999L);
    expected.put("quantity", 1);
    expected.put("timeLeft", AuctionDuration.VERY_LONG);
    expected.put("petSpeciesId", 1227);
    expected.put("petBreedId", 7);
    expected.put("petLevel", 1);
    expected.put("petQualityId", 3);
    assertRecord(actual.get(5), 71089, 23616, expected);

    expected.put("realm", "thrall");
    expected.put("faction", Faction.NEUTRAL);
    expected.put("auctionId", 2034963702);
    expected.put("itemId", 40914);
    expected.put("bidAmount", 150858L);
    expected.put("buyoutAmount", 158798L);
    expected.put("quantity", 1);
    expected.put("timeLeft", AuctionDuration.VERY_LONG);
    expected.put("petSpeciesId", 0);
    expected.put("petBreedId", 0);
    expected.put("petLevel", 0);
    expected.put("petQualityId", 0);
    assertRecord(actual.get(9), 64445, 64443, expected);
  }

  private void assertRecord(AuctionHouseExportFile testFile, int expectedRecordCount, int indexToTest, Map<String, Object> expectedValues) throws AuctionHouseExportException {
    List<AuctionHouseExportRecord> records = testFile.read(0).records();

    assertEquals(expectedRecordCount, records.size());

    AuctionHouseExportRecord record = records.get(indexToTest);
    assertEquals(expectedValues.get("realm"), record.realm());
    assertEquals(expectedValues.get("faction"), record.faction());

    assertEquals(expectedValues.get("auctionId"), record.auctionId());

    assertEquals(expectedValues.get("itemId"), record.itemId());
    assertEquals(expectedValues.get("bidAmount"), record.bidAmount());
    assertEquals(expectedValues.get("buyoutAmount"), record.buyoutAmount());
    assertEquals(expectedValues.get("quantity"), record.quantity());
    assertEquals(expectedValues.get("timeLeft"), record.timeLeft());
    assertEquals(expectedValues.get("petSpeciesId"), record.petSpeciesId());
    assertEquals(expectedValues.get("petBreedId"), record.petBreedId());
    assertEquals(expectedValues.get("petLevel"), record.petLevel());
    assertEquals(expectedValues.get("petQualityId"), record.petQualityId());
  }
}
