-- Table: import.auction_snapshot

DROP TABLE import.auction_snapshot;

CREATE TABLE import.auction_snapshot
(
  auction_id bigint NOT NULL,
  snapshot_hash character(32) NOT NULL,
  "timestamp" timestamp without time zone,
  item_id integer NOT NULL,
  time_left character varying(20) NOT NULL,
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
