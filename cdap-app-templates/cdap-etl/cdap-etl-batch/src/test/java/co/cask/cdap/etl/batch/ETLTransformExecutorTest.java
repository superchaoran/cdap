/*
 * Copyright © 2016 Cask Data, Inc.
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

package co.cask.cdap.etl.batch;

import co.cask.cdap.etl.api.Emitter;
import co.cask.cdap.etl.api.InvalidEntry;
import co.cask.cdap.etl.api.Transform;
import co.cask.cdap.etl.batch.mapreduce.ETLEmitter;
import co.cask.cdap.etl.batch.mapreduce.ETLTransformExecutor;
import co.cask.cdap.etl.batch.mapreduce.SinkEmitter;
import co.cask.cdap.etl.common.DefaultStageMetrics;
import co.cask.cdap.etl.common.TrackedTransform;
import co.cask.cdap.etl.mock.common.MockMetrics;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ETLTransformExecutorTest {

  @Test
  public void testTransforms() throws Exception {
    MockMetrics mockMetrics = new MockMetrics();
    Map<String, ETLTransformDetail> transformationMap = new HashMap<>();
    ETLTransformDetail sink1Detail = new ETLTransformDetail(
      new TrackedTransform<>(new DoubleToString(),
                             new DefaultStageMetrics(mockMetrics, "sink1")), new SinkEmitter<>
        ("sink1", new MockOutputWriter<>(null)));

    ETLTransformDetail sink2Detail = new ETLTransformDetail(
      new TrackedTransform<>(new DoubleToString(),
                             new DefaultStageMetrics(mockMetrics, "sink2")), new SinkEmitter<>
        ("sink2", new MockOutputWriter<>(null)));

    transformationMap.put("sink1", sink1Detail);
    transformationMap.put("sink2", sink2Detail);

    Map<String, ETLTransformDetail> map = new HashMap<>();
    map.put("sink2", sink2Detail);

    ETLTransformDetail transform2Detail = new ETLTransformDetail(
      new TrackedTransform<>(new Filter(100d, Threshold.LOWER),
                             new DefaultStageMetrics(mockMetrics, "transform2")), new ETLEmitter<>(map));
    transformationMap.put("transform2", transform2Detail);

    map.clear();
    map.put("transform2", transform2Detail);
    map.put("sink1", sink1Detail);

    ETLTransformDetail transform1Detail = new ETLTransformDetail(
      new TrackedTransform<>(new IntToDouble(),
                             new DefaultStageMetrics(mockMetrics, "transform1")), new ETLEmitter<>(map));

    transformationMap.put("transform1", transform1Detail);


    ETLTransformExecutor<Integer> executor = new ETLTransformExecutor<>(transformationMap, ImmutableSet.of("transform1"));

    executor.runOneIteration(1);

    Assert.assertEquals(3, mockMetrics.getCount("transform1.records.out"));
    Assert.assertEquals(0, mockMetrics.getCount("transform2.records.out"));
    Assert.assertEquals(3, mockMetrics.getCount("sink1.records.out"));
    Assert.assertEquals(0, mockMetrics.getCount("sink2.records.out"));
    mockMetrics.clearMetrics();
  }

  private static class IntToDouble extends Transform<Integer, Double> {

    @Override
    public void transform(Integer input, Emitter<Double> emitter) throws Exception {
      emitter.emit(input.doubleValue());
      emitter.emit(10 * input.doubleValue());
      emitter.emit(100 * input.doubleValue());
    }
  }

  private enum Threshold {
    LOWER,
    UPPER
  }

  private static class Filter extends Transform<Double, Double> {
    private final Double threshold;
    private final Threshold thresholdType;

    public Filter(Double threshold, Threshold thresholdType) {
      this.threshold = threshold;
      this.thresholdType = thresholdType;
    }

    @Override
    public void transform(Double input, Emitter<Double> emitter) throws Exception {
      if (thresholdType.equals(Threshold.LOWER)) {
        if (input > threshold) {
          emitter.emit(input);
        } else {
          emitter.emitError(new InvalidEntry<>(100, "less than threshold ", input));
        }
      } else {
        if (input < threshold) {
          emitter.emit(input);
        } else {
          emitter.emitError(new InvalidEntry<>(200, "greater than limit ", input));
        }
      }
    }
  }

  private static class DoubleToString extends Transform<Double, String> {

    @Override
    public void transform(Double input, Emitter<String> emitter) throws Exception {
      emitter.emit(String.valueOf(input));
    }
  }
}
