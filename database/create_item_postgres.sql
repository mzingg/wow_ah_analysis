-- Table: import.item

DROP TABLE import.item;

CREATE TABLE import.item
(
  item_id integer NOT NULL,
  CONSTRAINT pk_item PRIMARY KEY (item_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE import.item
  OWNER TO wowah;
