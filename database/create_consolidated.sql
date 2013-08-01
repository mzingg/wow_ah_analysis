-- Table: import.consolidated

-- DROP TABLE import.consolidated;

CREATE TABLE import.consolidated
(
  faction smallint,
  realm character varying(100),
  auction_id bigint NOT NULL,
  item_id integer,
  transactions bigint,
  max_end timestamp without time zone,
  last_buyout bigint,
  last_bid bigint,
  CONSTRAINT pk_consolidated PRIMARY KEY (auction_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE import.consolidated
  OWNER TO wowah;

-- Index: import.idx_max_end

-- DROP INDEX import.idx_max_end;

CREATE INDEX idx_max_end
  ON import.consolidated
  USING btree
  (max_end DESC NULLS LAST);

