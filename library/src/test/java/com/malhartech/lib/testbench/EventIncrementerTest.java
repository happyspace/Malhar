/**
 * Copyright (c) 2012-2012 Malhar, Inc. All rights reserved.
 */
package com.malhartech.lib.testbench;

import com.malhartech.dag.OperatorConfiguration;
import com.malhartech.dag.OperatorContext;
import com.malhartech.api.Sink;
import com.malhartech.dag.Tuple;
import com.malhartech.stream.StramTestSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functional test for {@link com.malhartech.lib.testbench.EventIncrementer}<p>
 * <br>
 * Benchmarks: The benchmark was done in local/inline mode<br>
 * Processing tuples on seed port are at 3.5 Million tuples/sec<br>
 * Processing tuples on increment port are at 10 Million tuples/sec<br>
 * <br>
 * Validates all DRC checks of the node<br>
 */
public class EventIncrementerTest
{
  private static Logger LOG = LoggerFactory.getLogger(FilteredEventClassifier.class);

  class DataSink implements Sink
  {
    HashMap<String, String> collectedTuples = new HashMap<String, String>();
    int count = 0;

    /**
     *
     * @param payload
     */
    @Override
    public void process(Object payload)
    {
      if (payload instanceof Tuple) {
        // LOG.debug(payload.toString());
      }
      else {
        HashMap<String, String> tuple = (HashMap<String, String>)payload;
        for (Map.Entry<String, String> e: ((HashMap<String, String>)payload).entrySet()) {
          collectedTuples.put(e.getKey(), e.getValue());
          count++;
        }
      }
    }

    public void clear()
    {
      count = 0;
      collectedTuples.clear();
    }
  }

  class CountSink implements Sink
  {
    int count = 0;

    /**
     *
     * @param payload
     */
    @Override
    public void process(Object payload)
    {
      if (payload instanceof Tuple) {
        // LOG.debug(payload.toString());
      }
      else {
        HashMap<String, Integer> tuple = (HashMap<String, Integer>)payload;
        for (Map.Entry<String, Integer> e: ((HashMap<String, Integer>)payload).entrySet()) {
          if (e.getKey().equals(EventIncrementer.OPORT_COUNT_TUPLE_COUNT)) {
            count = e.getValue().intValue();
          }
        }
      }
    }
  }

  /**
   * Test configuration and parameter validation of the node
   */
  @Test
  public void testNodeValidation()
  {
    OperatorConfiguration conf = new OperatorConfiguration("mynode", new HashMap<String, String>());
    LoadIncrementer node = new LoadIncrementer();

    conf.set(FilteredEventClassifier.KEY_KEYS, "");
    try {
      node.myValidation(conf);
      Assert.fail("validation error  " + EventIncrementer.KEY_KEYS);
    }
    catch (IllegalArgumentException e) {
      Assert.assertTrue("validate " + EventIncrementer.KEY_KEYS,
                        e.getMessage().contains("is empty"));
    }

    conf.set(EventIncrementer.KEY_KEYS, "a,b"); // from now on keys are a,b,c
    conf.set(EventIncrementer.KEY_LIMITS, "1,100;1,100;1,100");
    try {
      node.myValidation(conf);
      Assert.fail("validation error  " + EventIncrementer.KEY_LIMITS);
    }
    catch (IllegalArgumentException e) {
      Assert.assertTrue("validate " + EventIncrementer.KEY_LIMITS,
                        e.getMessage().contains("does not match number ids in limits"));
    }

    conf.set(EventIncrementer.KEY_LIMITS, "1,100,200;1,100");
    try {
      node.myValidation(conf);
      Assert.fail("validation error  " + EventIncrementer.KEY_LIMITS);
    }
    catch (IllegalArgumentException e) {
      Assert.assertTrue("validate " + EventIncrementer.KEY_LIMITS,
                        e.getMessage().contains("Property \"limits\" has a illegal value"));
    }

    conf.set(EventIncrementer.KEY_LIMITS, "1,a;1,100");
    try {
      node.myValidation(conf);
      Assert.fail("validation error  " + EventIncrementer.KEY_LIMITS);
    }
    catch (IllegalArgumentException e) {
      Assert.assertTrue("validate " + EventIncrementer.KEY_LIMITS,
                        e.getMessage().contains("has illegal format for one of its strings"));
    }

    conf.set(EventIncrementer.KEY_LIMITS, "100,1;1,100");
    try {
      node.myValidation(conf);
      Assert.fail("validation error  " + EventIncrementer.KEY_LIMITS);
    }
    catch (IllegalArgumentException e) {
      Assert.assertTrue("validate " + EventIncrementer.KEY_LIMITS,
                        e.getMessage().contains(">= high_value"));
    }
  }

  /**
   * Test node logic emits correct results
   */
  @Test
  public void testNodeProcessing() throws Exception
  {
    final EventIncrementer node = new EventIncrementer();

    DataSink dataSink = new DataSink();
    CountSink countSink = new CountSink();

    node.connect(EventIncrementer.OPORT_DATA, dataSink);
    node.connect(EventIncrementer.OPORT_COUNT, countSink);

    Sink seedSink = node.connect(EventIncrementer.IPORT_SEED, node);
    Sink incrSink = node.connect(EventIncrementer.IPORT_INCREMENT, node);

    OperatorConfiguration conf = new OperatorConfiguration("mynode", new HashMap<String, String>());

    conf.set(EventIncrementer.KEY_KEYS, "x,y");
    conf.set(EventIncrementer.KEY_LIMITS, "1,100;1,200");
    conf.set(EventIncrementer.KEY_DELTA, "1");
    node.setup(conf);

    final AtomicBoolean inactive = new AtomicBoolean(true);
    new Thread()
    {
      @Override
      public void run()
      {
        inactive.set(false);
        node.activate(new OperatorContext("LoadIncrementerTestNode", this));
      }
    }.start();

    // spin while the node gets activated./
    int sleeptimes = 0;
    try {
      do {
        Thread.sleep(20);
        sleeptimes++;
        if (sleeptimes > 5) {
          break;
        }
      }
      while (inactive.get());
    }
    catch (InterruptedException ex) {
      LOG.debug(ex.getLocalizedMessage());
    }

    Tuple bt = StramTestSupport.generateBeginWindowTuple("doesn't matter", 1);
    seedSink.process(bt);
    incrSink.process(bt);

    HashMap<String, Object> stuple = new HashMap<String, Object>(1);
    //int numtuples = 100000000; // For benchmarking
    int numtuples = 1000000;
    String seed1 = "a";
    ArrayList val = new ArrayList();
    val.add(new Integer(10));
    val.add(new Integer(20));
    stuple.put(seed1, val);
    for (int i = 0; i < numtuples; i++) {
      seedSink.process(stuple);
    }

    Tuple et = StramTestSupport.generateEndWindowTuple("doesn't matter", 1, 1);
    seedSink.process(et);
    incrSink.process(et);

    // Let the receiver get the tuples from the queue
    for (int i = 0; i < 20; i++) {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        LOG.error("Unexpected error while sleeping for 1 s", e);
      }
    }

    LOG.debug(String.format("\n*************************\nEmitted %d tuples, Processed %d tuples, Received %d tuples\n******************\n",
                            numtuples,
                            node.tuple_count,
                            dataSink.count));
    for (Map.Entry<String, String> e: ((HashMap<String, String>)dataSink.collectedTuples).entrySet()) {
      LOG.debug(String.format("Got key (%s) and value (%s)", e.getKey(), e.getValue()));
    }

    seedSink.process(bt);
    incrSink.process(bt);

    HashMap<String, Object> ixtuple = new HashMap<String, Object>(1);
    HashMap<String, Integer> ixval = new HashMap<String, Integer>(1);
    ixval.put("x", new Integer(10));
    ixtuple.put("a", ixval);

    HashMap<String, Object> iytuple = new HashMap<String, Object>(1);
    HashMap<String, Integer> iyval = new HashMap<String, Integer>(1);
    iyval.put("y", new Integer(10));
    iytuple.put("a", iyval);

    for (int i = 0; i < numtuples; i++) {
      incrSink.process(ixtuple);
      incrSink.process(iytuple);
    }

    seedSink.process(et);
    incrSink.process(et);

    // Let the receiver get the tuples from the queue
    for (int i = 0; i < 20; i++) {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        LOG.error("Unexpected error while sleeping for 1 s", e);
      }
    }

    LOG.debug(String.format("\n*************************\nEmitted %d tuples, Processed %d tuples, Received %d tuples\n******************\n",
                            numtuples*2,
                            node.tuple_count,
                            countSink.count));
     for (Map.Entry<String, String> e: ((HashMap<String, String>)dataSink.collectedTuples).entrySet()) {
      LOG.debug(String.format("Got key (%s) and value (%s)", e.getKey(), e.getValue()));
    }
  }
}