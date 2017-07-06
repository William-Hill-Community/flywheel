package au.com.williamhill.flywheel.edge.backplane;

import org.junit.*;

public final class InVMBackplaneTest extends BackplaneTest {
  private static final int CYCLES = 2;
  private static final int SCALE = 1;
  
  private String clusterId;
  
  private InVMBackplane backplane;
  
  @Override
  protected Backplane getBackplane(String clusterId, String brokerId) throws Exception {
    if (clusterId.equals(this.clusterId)) return backplane;
    
    backplane = new InVMBackplane();
    this.clusterId = clusterId;
    return backplane;
  }
  
  @Test
  public void testSingleNode() throws Exception {
    final int connectors = 1;
    final int topics = 3;
    final int messagesPerTopic = 10 * SCALE;
    final int expectedPartitions = (connectors - 1) * topics;
    final int expectedMessages = expectedPartitions * messagesPerTopic;
    test(CYCLES, false, connectors, topics, messagesPerTopic, expectedPartitions, expectedMessages);
    test(CYCLES, true, connectors, topics, messagesPerTopic, expectedPartitions, expectedMessages);
  }
  
  @Test
  public void testMultiNode() throws Exception {
    final int connectors = 4;
    final int topics = 3;
    final int messagesPerTopic = 10 * SCALE;
    final int expectedPartitions = (connectors - 1) * topics;
    final int expectedMessages = expectedPartitions * messagesPerTopic;
    test(CYCLES, false, connectors, topics, messagesPerTopic, expectedPartitions, expectedMessages);
    test(CYCLES, true, connectors, topics, messagesPerTopic, expectedPartitions, expectedMessages);
  }
}
