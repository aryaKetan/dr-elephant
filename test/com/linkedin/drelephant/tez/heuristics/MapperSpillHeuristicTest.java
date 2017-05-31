/*
 * Copyright 2016 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.linkedin.drelephant.tez.heuristics;

import com.linkedin.drelephant.analysis.ApplicationType;
import com.linkedin.drelephant.analysis.Heuristic;
import com.linkedin.drelephant.analysis.HeuristicResult;
import com.linkedin.drelephant.analysis.Severity;
import com.linkedin.drelephant.tez.data.TezCounterData;
import com.linkedin.drelephant.tez.data.TezDAGApplicationData;
import com.linkedin.drelephant.tez.data.TezDAGData;
import com.linkedin.drelephant.tez.data.TezVertexData;
import com.linkedin.drelephant.tez.data.TezVertexTaskData;
import com.linkedin.drelephant.configurations.heuristic.HeuristicConfigurationData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;


public class MapperSpillHeuristicTest extends TestCase {

  private static Map<String, String> paramsMap = new HashMap<String, String>();
  private static Heuristic _heuristic = new MapperSpillHeuristic(new HeuristicConfigurationData("test_heuristic",
      "test_class", "test_view", new ApplicationType("test_apptype"), paramsMap));

  public void testCritical() throws IOException {
    // Spill ratio 3.0, 1000 tasks
    assertEquals(Severity.CRITICAL, analyzeJob(3000, 1000, 1000));
  }

  public void testSevere() throws IOException {
    // Spill ratio 2.5, 1000 tasks
    assertEquals(Severity.SEVERE, analyzeJob(2500, 1000, 1000));
  }

  public void testModerate() throws IOException {
    // Spill ratio 2.3, 1000 tasks
    assertEquals(Severity.MODERATE, analyzeJob(2300, 1000, 1000));
  }

  public void testLow() throws IOException {
    // Spill ratio 2.1, 1000 tasks
    assertEquals(Severity.LOW, analyzeJob(2100, 1000, 1000));
  }

  public void testNone() throws IOException {
    // Spill ratio 1.0, 1000 tasks
    assertEquals(Severity.NONE, analyzeJob(1000, 1000, 1000));
  }

  public void testSmallNumTasks() throws IOException {
    // Spill ratio 3.0, should be critical, but number of task is small(10), final result is NONE
    assertEquals(Severity.NONE, analyzeJob(3000, 1000, 10));
  }

  private Severity analyzeJob(long spilledRecords, long mapRecords, int numTasks) throws IOException {
	  TezCounterData jobCounter = new TezCounterData();
    TezVertexTaskData[] mappers = new TezVertexTaskData[numTasks + 1];

    TezCounterData counter = new TezCounterData();
    counter.set(TezCounterData.CounterName.SPILLED_RECORDS, spilledRecords);
    counter.set(TezCounterData.CounterName.MAP_OUTPUT_RECORDS, mapRecords);

    int i = 0;
    for (; i < numTasks; i++) {
      mappers[i] = new TezVertexTaskData("task-id-"+i, "task-attempt-id-"+i);
      mappers[i].setTime(new long[5]);
    		mappers[i].setCounter(counter);
    }
    // Non-sampled task, which does not contain time and counter data
    mappers[i] = new TezVertexTaskData("task-id-"+i, "task-attempt-id-"+i);

    TezDAGData tezDags[] = new TezDAGData[1];
    TezDAGData tezDAGData = new TezDAGData(counter);
    TezVertexData tezVertexes[] = new TezVertexData[1];
    TezVertexData tezVertexData = new TezVertexData("new vertex");
    tezVertexes[0]=tezVertexData;
    tezVertexData.setMapperData(mappers);
    tezDags[0]=tezDAGData;
    tezDAGData.setVertexData(tezVertexes);

    TezDAGApplicationData data = new TezDAGApplicationData();
    data.setCounters(counter).setTezDAGData(tezDags);
    HeuristicResult result = _heuristic.apply(data);
    return result.getSeverity();
  }
}
