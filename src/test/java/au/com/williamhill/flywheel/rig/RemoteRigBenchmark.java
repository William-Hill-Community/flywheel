package au.com.williamhill.flywheel.rig;

import static com.obsidiandynamics.indigo.util.PropertyUtils.*;

import java.net.*;
import java.util.*;

import com.obsidiandynamics.indigo.benchmark.*;
import com.obsidiandynamics.indigo.util.*;
import com.obsidiandynamics.shell.*;

import au.com.williamhill.flywheel.remote.*;
import au.com.williamhill.flywheel.rig.RemoteRig.*;
import au.com.williamhill.flywheel.rig.TripleRigBenchmark.*;
import au.com.williamhill.flywheel.topic.*;

public final class RemoteRigBenchmark implements TestSupport {
  private static final Properties PROPS = new Properties(System.getProperties());
  private static final String URL = getOrSet(PROPS, "flywheel.rig.url", String::valueOf, "ws://localhost:8080/broker");
  private static final int SYNC_FRAMES = getOrSet(PROPS, "flywheel.rig.syncFrames", Integer::valueOf, 1000);
  private static final String SPEC = getOrSet(PROPS, "flywheel.rig.spec", String::valueOf, "cp://specs/jumbo-leaves.yaml");
  private static final boolean INITIATE = getOrSet(PROPS, "flywheel.rig.initiate", Boolean::valueOf, true);
  private static final double NORMAL_MIN = getOrSet(PROPS, "flywheel.rig.normalMin", RemoteRigBenchmark::doubleOrNaN, Double.NaN);
  private static final boolean CYCLE = getOrSet(PROPS, "flywheel.rig.cycle", Boolean::valueOf, false);
  private static final int CYCLE_WAIT = getOrSet(PROPS, "flywheel.rig.cycleWait", Integer::valueOf, 0);
  private static final int STATS_PERIOD = getOrSet(PROPS, "flywheel.rig.statsPeriod", Integer::valueOf, 100);
  private static final long PRINT_OUTLIERS_OVER = getOrSet(PROPS, "flywheel.rig.printOutliersOver", Long::parseLong, 10_000L);
  
  private static double doubleOrNaN(String value) {
    return value.equals("NaN") ? Double.NaN : Double.parseDouble(value);
  }
  
  private static Summary run(Config c) throws Exception {
    final RemoteNode remote = RemoteNode.builder()
        .build();
    final RemoteRig remoteRig = new RemoteRig(remote, new RemoteRigConfig() {{
      topicSpec = c.topicSpec;
      syncFrames = c.syncFrames;
      uri = getUri(c.host, c.port, c.path);
      initiate = c.initiate;
      normalMinNanos = c.normalMinNanos;
      printOutliersOverMillis = c.printOutliersOverMillis;
      statsPeriod = c.statsPeriod;
      log = c.log;
    }});
    
    remoteRig.run();
    remoteRig.await();
    remoteRig.close();
    return remoteRig.getSummary();
  }
  
  public static void main(String[] args) throws Exception {
    BourneUtils.run("ulimit -Sa", null, true, System.out::print);
    LOG_STREAM.println();
    filter("flywheel.rig", PROPS).entrySet().stream()
    .map(e -> String.format("%-30s: %s", e.getKey(), e.getValue())).forEach(LOG_STREAM::println);
    
    final URI uri = new URI(URL);
    do {
      LOG_STREAM.format("_\nRemote benchmark started (URI: %s, initiate: %b)...\n", 
                        uri, INITIATE);
      new Config() {{
        runner = RemoteRigBenchmark::run;
        host = uri.getHost();
        port = uri.getPort();
        path = uri.getPath();
        syncFrames = SYNC_FRAMES;
        topicSpec = TopicLibrary.load(SPEC);
        initiate = INITIATE;
        normalMinNanos = NORMAL_MIN;
        printOutliersOverMillis = PRINT_OUTLIERS_OVER;
        statsPeriod = STATS_PERIOD;
        log = new LogConfig() {{
          progress = intermediateSummaries = false;
          stages = true;
          summary = true;
        }};
      }}.test();
      
      if (CYCLE) TestSupport.sleep(CYCLE_WAIT);
    } while (CYCLE);
  }
}
