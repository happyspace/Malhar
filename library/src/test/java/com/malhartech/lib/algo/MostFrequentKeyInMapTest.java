/**
 * Copyright (c) 2012-2012 Malhar, Inc. All rights reserved.
 */
package com.malhartech.lib.algo;

import com.malhartech.api.OperatorConfiguration;
import com.malhartech.dag.TestCountAndLastTupleSink;
import java.util.ArrayList;
import java.util.HashMap;
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Functional tests for {@link com.malhartech.lib.algo.MostFrequentKeyInMap}<p>
 *
 */
public class MostFrequentKeyInMapTest
{
  private static Logger log = LoggerFactory.getLogger(MostFrequentKeyInMapTest.class);

  /**
   * Test node logic emits correct results
   */
  @Test
  @SuppressWarnings("SleepWhileInLoop")
  public void testNodeProcessing() throws Exception
  {
    MostFrequentKeyInMap<String, Integer> oper = new MostFrequentKeyInMap<String, Integer>();
    TestCountAndLastTupleSink matchSink = new TestCountAndLastTupleSink();
    TestCountAndLastTupleSink listSink = new TestCountAndLastTupleSink();
    oper.most.setSink(matchSink);
    oper.list.setSink(listSink);
    oper.setup(new OperatorConfiguration());

    oper.beginWindow();
    HashMap<String, Integer> amap = new HashMap<String, Integer>(1);
    HashMap<String, Integer> bmap = new HashMap<String, Integer>(1);
    HashMap<String, Integer> cmap = new HashMap<String, Integer>(1);
    int atot = 5;
    int btot = 7;
    int ctot = 6;
    amap.put("a", null);
    bmap.put("b", null);
    cmap.put("c", null);
    for (int i = 0; i < atot; i++) {
      oper.data.process(amap);
    }
    for (int i = 0; i < btot; i++) {
      oper.data.process(bmap);
    }
    for (int i = 0; i < ctot; i++) {
      oper.data.process(cmap);
    }
    oper.endWindow();
    Assert.assertEquals("number emitted tuples", 1, matchSink.count);
    HashMap<String, Integer> tuple = (HashMap<String, Integer>)matchSink.tuple;
    Integer val = tuple.get("b");
    Assert.assertEquals("Count of b was ", btot, val.intValue());
    Assert.assertEquals("number emitted tuples", 1, listSink.count);
    ArrayList<HashMap<String, Integer>> list = (ArrayList<HashMap<String, Integer>>)listSink.tuple;
    val = list.get(0).get("b");
    Assert.assertEquals("Count of b was ", btot, val.intValue());

    matchSink.clear();
    listSink.clear();
    oper.beginWindow();
    atot = 5;
    btot = 4;
    ctot = 5;
    for (int i = 0; i < atot; i++) {
      oper.data.process(amap);
    }
    for (int i = 0; i < btot; i++) {
      oper.data.process(bmap);
    }
    for (int i = 0; i < ctot; i++) {
      oper.data.process(cmap);
    }
    oper.endWindow();
    Assert.assertEquals("number emitted tuples", 1, matchSink.count);
    Assert.assertEquals("number emitted tuples", 1, listSink.count);
    list = (ArrayList<HashMap<String, Integer>>)listSink.tuple;
    int acount = 0;
    int ccount = 0;
    for (HashMap<String, Integer> h: list) {
      val = h.get("a");
      if (val == null) {
        ccount = h.get("c").intValue();
      }
      else {
        acount = val.intValue();
      }
    }
    Assert.assertEquals("Count of a was ", atot, acount);
    Assert.assertEquals("Count of c was ", ctot, ccount);
    HashMap<String, Integer> mtuple = (HashMap<String, Integer>)matchSink.tuple;
    val = mtuple.get("a");
    if (val == null) {
      val = mtuple.get("c");
    }
    Assert.assertEquals("Count of least frequent key was ", ctot, val.intValue());
  }
}