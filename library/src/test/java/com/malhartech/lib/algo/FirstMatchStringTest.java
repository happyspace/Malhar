/**
 * Copyright (c) 2012-2012 Malhar, Inc. All rights reserved.
 */
package com.malhartech.lib.algo;

import com.malhartech.api.OperatorConfiguration;
import com.malhartech.dag.TestCountAndLastTupleSink;
import java.util.HashMap;
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Functional tests for {@link com.malhartech.lib.algo.FirstMatchString}<p>
 *
 */
public class FirstMatchStringTest
{
  private static Logger log = LoggerFactory.getLogger(FirstMatchStringTest.class);

  /**
   * Test node logic emits correct results
   */
  @Test
  @SuppressWarnings("SleepWhileInLoop")
  public void testNodeProcessing() throws Exception
  {
    FirstMatchString<String> oper = new FirstMatchString<String>();
    TestCountAndLastTupleSink matchSink = new TestCountAndLastTupleSink();
    oper.first.setSink(matchSink);
    oper.setup(new OperatorConfiguration());
    oper.setKey("a");
    oper.setValue(3);
    oper.setTypeEQ();

    oper.beginWindow();
    HashMap<String, String> input = new HashMap<String, String>();
    input.put("a", "4");
    input.put("b", "20");
    input.put("c", "1000");
    oper.data.process(input);
    input.put("a", "3");
    input.put("b", "20");
    input.put("c", "1000");
    oper.data.process(input);
    input.clear();
    input.put("a", "2");
    oper.data.process(input);
    input.clear();
    input.put("a", "4");
    input.put("b", "21");
    input.put("c", "1000");
    oper.data.process(input);
    input.clear();
    input.put("a", "4");
    input.put("b", "20");
    input.put("c", "5");
    oper.data.process(input);
    oper.endWindow();

    Assert.assertEquals("number emitted tuples", 1, matchSink.count);
    HashMap<String, String> tuple = (HashMap<String, String>)matchSink.tuple;
    String aval = tuple.get("a");
    Assert.assertEquals("Value of a was ", "3", aval);
    matchSink.clear();

    oper.beginWindow();
    input.clear();
    input.put("a", "2");
    input.put("b", "20");
    input.put("c", "1000");
    oper.data.process(input);
    input.clear();
    input.put("a", "5");
    oper.data.process(input);
    oper.endWindow();
    // There should be no emit as all tuples do not match
    Assert.assertEquals("number emitted tuples", 0, matchSink.count);
    matchSink.clear();
  }
}