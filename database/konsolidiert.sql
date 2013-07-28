SELECT g.faction, g.realm, g.auction_id, g.item_id, Count(*) AS transactions, MAX(expected_end) AS max_end, MAX(buyout_amount) AS last_buyout, MAX(bid_amount) AS last_bid, MAX(v.snapshot_hash) AS active_hash
FROM
	import.auction_snapshot AS g
LEFT JOIN 
	(SELECT DISTINCT auction_id, snapshot_hash FROM import.auction_snapshot WHERE timestamp = (SELECT MAX(timestamp) FROM import.auction_snapshot ) AND expected_end >= NOW()) AS v ON (g.auction_id = v.auction_id AND g.snapshot_hash = v.snapshot_hash)
WHERE g.faction = 2 AND g.realm = 'thrall'  
GROUP BY g.faction, g.realm, g.auction_id, g.item_id

