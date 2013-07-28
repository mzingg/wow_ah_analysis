-- Table: import.auction_snapshot

DROP TABLE import.auction_snapshot;

CREATE TABLE import.auction_snapshot
(
  faction smallint NOT NULL,
  realm character varying(100) NOT NULL,
  auction_id bigint NOT NULL,
  snapshot_hash character(32) NOT NULL,
  "timestamp" timestamp without time zone NOT NULL,
  item_id integer NOT NULL,
  time_left character varying(20) NOT NULL,
  expected_end timestamp without time zone NOT NULL,
  owner character varying(255) NOT NULL,
  bid_amount bigint NOT NULL,
  buyout_amount bigint,
  quantity integer NOT NULL,
  pet_species_id integer,
  pet_breed_id integer,
  pet_level integer,
  pet_quality_id integer,
  rand bigint NOT NULL DEFAULT 0,
  seed bigint NOT NULL DEFAULT 0,
  CONSTRAINT pk_auction_snapshot PRIMARY KEY (snapshot_hash, auction_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE import.auction_snapshot
  OWNER TO wowah;

-- Index: import.id_snapshot_hash

-- DROP INDEX import.id_snapshot_hash;

CREATE INDEX id_snapshot_hash
  ON import.auction_snapshot
  USING hash
  (snapshot_hash COLLATE pg_catalog."default");

-- Index: import.idx_expected_end

-- DROP INDEX import.idx_expected_end;

CREATE INDEX idx_expected_end
  ON import.auction_snapshot
  USING btree
  (expected_end);

-- Index: import.idx_timestamp

-- DROP INDEX import.idx_timestamp;

CREATE INDEX idx_timestamp
  ON import.auction_snapshot
  USING hash
  ("timestamp");

