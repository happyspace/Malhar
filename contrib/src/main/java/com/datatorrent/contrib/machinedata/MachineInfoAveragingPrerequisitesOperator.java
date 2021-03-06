/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.contrib.machinedata;

import com.datatorrent.api.BaseOperator;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.contrib.machinedata.operator.averaging.AverageData;
import com.datatorrent.contrib.machinedata.operator.averaging.AveragingInfo;
import com.datatorrent.lib.util.KeyValPair;
import com.datatorrent.lib.util.TimeBucketKey;
import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableLong;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>MachineInfoAveragingPrerequisitesOperator class.</p>
 *
 * @since 0.3.5
 */
public class MachineInfoAveragingPrerequisitesOperator extends BaseOperator {

    // Aggregate sum of all values seen for a key.
    private HashMap<TimeBucketKey, Map<String, MutableDouble>> sums =
            new HashMap<TimeBucketKey, Map<String, MutableDouble>>();

    // Count of number of values seen for key.
    private HashMap<TimeBucketKey, MutableLong> counts = new HashMap<TimeBucketKey, MutableLong>();

    public final transient DefaultOutputPort<KeyValPair<TimeBucketKey, Map<String, AverageData>>> outputPort =
            new DefaultOutputPort<KeyValPair<TimeBucketKey, Map<String, AverageData>>>();

    public transient DefaultInputPort<MachineInfo> inputPort = new DefaultInputPort<MachineInfo>() {

        @Override
        public void process(MachineInfo tuple) {
            MachineKey key = tuple.getMachineKey();

            Map<String, MutableDouble> sumsMap = sums.get(key);
            if (sumsMap == null) {
                sumsMap = new HashMap<String, MutableDouble>();
                sumsMap.put("cpu", new MutableDouble(tuple.getCpu()));
                sumsMap.put("ram", new MutableDouble(tuple.getRam()));
                sumsMap.put("hdd", new MutableDouble(tuple.getHdd()));
                sums.put(key, sumsMap);
            } else {
                sumsMap.get("cpu").add(tuple.getCpu());
                sumsMap.get("ram").add(tuple.getRam());
                sumsMap.get("hdd").add(tuple.getHdd());
            }

            MutableLong count = counts.get(key);
            if (count == null) {
                count = new MutableLong(1);
                counts.put(key, count);
            } else {
                count.increment();
            }
        }
    };

    @Override
    public void endWindow() {

        for (Map.Entry<TimeBucketKey, Map<String, MutableDouble>> entry : sums.entrySet()) {

            Map<String, MutableDouble> sumMap = sums.get(entry.getKey());
            long count = counts.get(entry.getKey()).longValue();

            Map<String, AverageData> avg = new HashMap<String, AverageData>();
            avg.put("cpu", new AverageData(sumMap.get("cpu").doubleValue(), count));
            avg.put("ram", new AverageData(sumMap.get("ram").doubleValue(), count));
            avg.put("hdd", new AverageData(sumMap.get("hdd").doubleValue(), count));

            outputPort.emit(new KeyValPair<TimeBucketKey, Map<String, AverageData>>(entry.getKey(), avg));
        }
        sums.clear();
        counts.clear();
    }

}
