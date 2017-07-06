package au.com.williamhill.flywheel.edge.backplane.kafka;

import org.apache.kafka.clients.consumer.*;

public class KafkaReceiver<K, V> extends Thread implements AutoCloseable {
  @FunctionalInterface
  public interface RecordHandler<K, V> {
    void handle(ConsumerRecords<K, V> records);
  }
  
  private final Consumer<K, V> consumer;
  
  private final long pollTimeoutMillis;
  
  private final RecordHandler<K, V> handler;
  
  private volatile boolean running = true;
  
  public KafkaReceiver(Consumer<K, V> consumer, long pollTimeoutMillis, String name, RecordHandler<K, V> handler) {
    super(name);
    this.consumer = consumer;
    this.pollTimeoutMillis = pollTimeoutMillis;
    this.handler = handler;
    start();
  }
  
  public Consumer<K, V> getConsumer() {
    return consumer;
  }
  
  @Override 
  public void run() {
    while (running) {
      final ConsumerRecords<K, V> records = consumer.poll(pollTimeoutMillis);
      if (! records.isEmpty()) {
        handler.handle(records);
      }
    }
    consumer.close();
  }
  
  @Override
  public void close() throws InterruptedException {
    running = false;
    interrupt();
  }
  
  public void await() throws InterruptedException {
    join();
  }
}
