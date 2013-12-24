plotItem <- function (itemId, realm='thrall', faction='HORDE') {
  auctionRecordsCursor <- mongo.find(wow_auctions, 'wow_auctions.auctionRecord', createItemQuery(itemId, realm, faction))
  auctionRecords <- NULL
  while (mongo.cursor.next(auctionRecordsCursor)) {
    value <- mongo.cursor.value(auctionRecordsCursor)
    current <- data.frame(
      year=mongo.bson.value(value, 'year'),
      month=mongo.bson.value(value, 'month'),
      day=mongo.bson.value(value, 'day'),
      buyoutAmount=mongo.bson.value(value, 'buyoutAmount'),
      quantity=mongo.bson.value(value, 'quantity')
    )
    auctionRecords <- rbind(auctionRecords, current)
  }
  auctionRecords$bucketDate <- as.POSIXct(paste(auctionRecords$day, auctionRecords$month + 1, auctionRecords$year, sep='.'), format='%d.%m.%Y')
  auctionRecords$buyoutPrice <- auctionRecords$buyoutAmount/(auctionRecords$quantity)
  
  plotData <- ddply(auctionRecords, .(bucketDate, year), summarise, mean=mean(buyoutPrice/10000))
  
  return(xyplot(mean ~ bucketDate, plotData))
}

createItemQuery <- function (itemId, realm='thrall', faction='HORDE') {
  buf <- mongo.bson.buffer.create()
  mongo.bson.buffer.append(buf, 'realm', realm)
  mongo.bson.buffer.append(buf, 'faction', faction)
  mongo.bson.buffer.append.int(buf, 'itemId', itemId)
  return(mongo.bson.from.buffer(buf))
}