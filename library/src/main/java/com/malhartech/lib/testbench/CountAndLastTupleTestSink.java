/**
 * Copyright (c) 2012-2012 Malhar, Inc.
 * All rights reserved.
 */
package com.malhartech.lib.testbench;

import com.malhartech.engine.Tuple;
import com.malhartech.lib.testbench.CountTestSink;
import com.malhartech.lib.testbench.CountTestSink;

/**
 * A sink implementation to collect expected test results.
 */
public class CountAndLastTupleTestSink<T> extends CountTestSink<T>
{
  public  Object tuple = null;
  /**
   *
   * @param payload
   */

  @Override
  public void clear()
  {
    this.tuple = null;
    super.clear();
  }

  @Override
  public void process(T tuple)
  {
    if (tuple instanceof Tuple) {

    }
    else {
      this.tuple = tuple;
      count++;
    }
  }
}