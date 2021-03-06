package au.com.williamhill.flywheel.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.obsidiandynamics.assertion.*;

public final class MapsTest {
  @Test
  public void testConformance() throws Exception {
    Assertions.assertUtilityClassWellDefined(Maps.class);
  }

  @Test
  public void testExisting() {
    final Object value = new String("value");
    final Map<String, Object> map = Collections.singletonMap("key", value);
    final Object put = Maps.putAtomic(map, map, "key", () -> "value");
    assertSame(value, put);
  }

  @Test
  public void testRaceCondition() {
    final Map<String, Object> map = new HashMap<String, Object>() {
      private static final long serialVersionUID = 1L;
      
      private int calls;
      
      @Override
      public Object get(Object key) {
        if (calls == 0) {
          calls++;
          return null;
        } else {
          return super.get(key);
        }
      }
    };
    
    final Object simulatedRaceCondition = new String("value");
    map.put("key", simulatedRaceCondition);
    final Object inserted = new String("value2");
    final Object put = Maps.putAtomic(map, map, "key", () -> inserted);
    assertSame(simulatedRaceCondition, put);
  }

  @Test(expected=IllegalStateException.class)
  public void testException() {
    final Map<String, Object> map = new HashMap<String, Object>() {
      private static final long serialVersionUID = 1L;
      
      private int calls;
      
      @Override
      public Object get(Object key) {
        if (calls == 0) {
          calls++;
          return null;
        } else {
          throw new IllegalStateException();
        }
      }
    };
    
    Maps.putAtomic(map, map, "key", () -> "value");
  }
  
  @Test
  public void testInsert() {
    final Map<String, Object> map = new HashMap<>();
    final Object inserted = new String("value");
    final Object put = Maps.putAtomic(map, map, "key", () -> inserted);
    assertSame(inserted, put);
  }
}
