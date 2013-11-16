package ch.mrwolf.wow.dbimport.io;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AsyncQueue<T> implements Runnable {

  private static final DecimalFormat MS_FORMAT = new DecimalFormat("0.000");

  private final int batchSize;

  private final BlockingQueue<T> workQueue;

  private boolean running;

  public AsyncQueue(final int batchSize) {
    this.batchSize = batchSize;
    workQueue = new LinkedBlockingQueue<>();
  }

  public final synchronized void enqueue(final T record) {
    workQueue.add(record);
  }

  private synchronized Queue<T> dequeue() {
    final Queue<T> result = new LinkedList<>();
    for (int i = 1; i <= batchSize; i++) {
      if (workQueue.isEmpty()) {
        break;
      }
      result.add(workQueue.poll());
    }
    return result;
  }

  public final void stopWorking() {
    running = false;
  }

  public final int size() {
    return workQueue.size();
  }

  @Override
  public final void run() {
    running = true;
    while (workQueue.size() > 0 || running) {

      if (workQueue.size() > 0) {
        log.info("Remaining queue size: {}", workQueue.size());
      }

      flushQueue(dequeue());

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  private void flushQueue(final Queue<T> processingQueue) {
    if (processingQueue.size() == 0) {
      return;
    }

    final int count = processingQueue.size();
    long start = System.currentTimeMillis();

    process(processingQueue);

    long end = System.currentTimeMillis();

    final long duration = end - start;

    log.info("Processed {} records. {}ms per record.", count, MS_FORMAT.format(duration * 1d / count));
  }

  protected abstract void process(final Queue<T> processingQueue);

}
