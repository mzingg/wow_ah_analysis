package ch.mrwolf.wow.dbimport.io.jsondb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.util.StringUtils;

import ch.mrwolf.wow.dbimport.io.AbstractJdbcProcessingStateCallback;

@CommonsLog
public class ProcessingCallback extends AbstractJdbcProcessingStateCallback {

  @Override
  public void afterFile(final File file, final Calendar snapshotTime, final String snapshotMd5Hash) {

    final String sqlStatement = String.format("INSERT INTO %s (snapshot_hash, timestamp, data) VALUES (?, ?, ?)", getTableName());

    String jsonValue = readBz2JsonFile(file);

    if (!StringUtils.isEmpty(jsonValue)) {

      jsonValue = jsonValue.replaceAll("timeLeft", "timeleft");
      jsonValue = jsonValue.replaceAll("petSpeciesId", "petspeciesid");
      jsonValue = jsonValue.replaceAll("petBreedId", "petbreedid");
      jsonValue = jsonValue.replaceAll("petLevel", "petlevel");
      jsonValue = jsonValue.replaceAll("petQualityId", "petqualityid");

      try {

        final PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        jsonObject.setValue(jsonValue);

        getJdbcTemplate().update(sqlStatement, new PreparedStatementSetter() {
          @Override
          public void setValues(final PreparedStatement ps) throws SQLException {

            ps.setString(1, snapshotMd5Hash);
            ps.setTimestamp(2, new Timestamp(snapshotTime.getTimeInMillis()));
            ps.setObject(3, jsonObject);
          }

        });

      } catch (SQLException e) {
        log.error(e.getMessage(), e);
      }

    } else {
      log.info(String.format("Skipped file [%s] because it contains no value", file.getName()));
    }
    super.afterFile(file, snapshotTime, snapshotMd5Hash);
  }

  private String readBz2JsonFile(final File file) {
    final StringBuilder jsonDataValue = new StringBuilder();
    try (BZip2CompressorInputStream inputStream = new BZip2CompressorInputStream(new FileInputStream(file))) {
      try (BufferedReader jsonData = new BufferedReader(new InputStreamReader(inputStream))) {
        String line = null;
        do {
          line = jsonData.readLine();
          if (line != null) {
            jsonDataValue.append(line);
          }
        } while (line != null);
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return jsonDataValue.toString();
  }
}
