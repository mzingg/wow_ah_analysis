SELECT auction_id, faction, realm, item_id, Count(*) AS transactions, MAX(expected_end) AS max_end, MAX(buyout_amount) AS last_buyout, MAX(bid_amount) AS last_bid
FROM import.auction_snapshot
GROUP BY faction, realm, auction_id, item_id

