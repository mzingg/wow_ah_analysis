DROP FUNCTION IF EXISTS import.update_items();

DROP TYPE IF EXISTS auction;
CREATE TYPE auction AS (snapshot_hash char(32), auc bigint, item int, owner varchar(255), bid bigint, buyout bigint, quantity int, timeleft varchar(20), rand bigint, seed bigint, petspeciesid int, petbreedid int, petlevel int, petqualityid int);

CREATE FUNCTION import.update_items() RETURNS SETOF auction AS $$
DECLARE
  p_snapshot_hash char(32);
BEGIN
  FOR p_snapshot_hash IN
    SELECT snapshot_hash FROM import.auction_snapshot LIMIT 10
  LOOP
    RETURN QUERY (SELECT p_snapshot_hash AS snapshot_hash, auc, item, owner, bid, buyout, quantity, timeleft, rand, seed, petspeciesid, petbreedid, petlevel, petqualityid FROM json_populate_recordset(null::auction, (SELECT data->'alliance'->'auctions' FROM import.auction_snapshot WHERE snapshot_hash = p_snapshot_hash)) AS json);
  END LOOP;  
  RETURN;
END;
$$ LANGUAGE plpgsql STABLE;