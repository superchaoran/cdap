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

package co.cask.cdap.etl.spark.streaming;

import co.cask.cdap.api.TxRunnable;
import co.cask.cdap.api.data.DatasetContext;
import co.cask.cdap.api.spark.JavaSparkExecutionContext;
import co.cask.cdap.etl.api.batch.SparkCompute;
import co.cask.cdap.etl.api.batch.SparkExecutionPluginContext;
import co.cask.cdap.etl.api.batch.SparkSink;
import co.cask.cdap.etl.api.streaming.Windower;
import co.cask.cdap.etl.planner.StageInfo;
import co.cask.cdap.etl.spark.SparkCollection;
import co.cask.cdap.etl.spark.SparkPairCollection;
import co.cask.cdap.etl.spark.batch.BasicSparkExecutionPluginContext;
import co.cask.cdap.etl.spark.streaming.function.ComputeTransformFunction;
import co.cask.cdap.etl.spark.streaming.function.CountingTranformFunction;
import co.cask.cdap.etl.spark.streaming.function.DynamicAggregatorAggregate;
import co.cask.cdap.etl.spark.streaming.function.DynamicAggregatorGroupBy;
import co.cask.cdap.etl.spark.streaming.function.DynamicSparkCompute;
import co.cask.cdap.etl.spark.streaming.function.DynamicTransform;
import co.cask.cdap.etl.spark.streaming.function.StreamingBatchSinkFunction;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;

import javax.annotation.Nullable;

/**
 * JavaDStream backed {@link co.cask.cdap.etl.spark.SparkCollection}
 *
 * @param <T> type of objects in the collection
 */
public class DStreamCollection<T> implements SparkCollection<T> {

  private final JavaSparkExecutionContext sec;
  private final JavaDStream<T> stream;

  public DStreamCollection(JavaSparkExecutionContext sec, JavaDStream<T> stream) {
    this.sec = sec;
    this.stream = stream;
  }

  @SuppressWarnings("unchecked")
  @Override
  public JavaDStream<T> getUnderlying() {
    return stream;
  }

  @Override
  public SparkCollection<T> cache() {
    return wrap(stream.cache());
  }

  @SuppressWarnings("unchecked")
  @Override
  public SparkCollection<T> union(SparkCollection<T> other) {
    return wrap(stream.union((JavaDStream<T>) other.getUnderlying()));
  }

  @Override
  public <U> SparkCollection<U> flatMap(StageInfo stageInfo, FlatMapFunction<T, U> function) {
    return wrap(stream.transform(new DynamicTransform<T, U>(new DynamicDriverContext(stageInfo, sec))));
  }

  @Override
  public <K, V> SparkPairCollection<K, V> flatMapToPair(PairFlatMapFunction<T, K, V> function) {
    return new PairDStreamCollection<>(sec, stream.flatMapToPair(function));
  }

  @Override
  public <U> SparkCollection<U> aggregate(StageInfo stageInfo, @Nullable Integer partitions) {
    DynamicDriverContext dynamicDriverContext = new DynamicDriverContext(stageInfo, sec);
    JavaPairDStream<Object, T> keyedCollection =
      stream.transformToPair(new DynamicAggregatorGroupBy<Object, T>(dynamicDriverContext));

    JavaPairDStream<Object, Iterable<T>> groupedCollection = partitions == null ?
      keyedCollection.groupByKey() : keyedCollection.groupByKey(partitions);

    return wrap(groupedCollection.transform(new DynamicAggregatorAggregate<Object, T, U>(dynamicDriverContext)));
  }

  @Override
  public <U> SparkCollection<U> compute(StageInfo stageInfo, SparkCompute<T, U> compute) throws Exception {
    final String stageName = stageInfo.getName();
    final SparkCompute<T, U> wrappedCompute =
      new DynamicSparkCompute<>(new DynamicDriverContext(stageInfo, sec), compute);
    sec.execute(new TxRunnable() {
      @Override
      public void run(DatasetContext datasetContext) throws Exception {
        SparkExecutionPluginContext sparkPluginContext =
          new BasicSparkExecutionPluginContext(sec, JavaSparkContext.fromSparkContext(stream.context().sparkContext()),
                                               datasetContext, stageName);
        wrappedCompute.initialize(sparkPluginContext);
      }
    });
    return wrap(stream.transform(new ComputeTransformFunction<>(sec, stageName, wrappedCompute)));
  }

  @Override
  public void store(StageInfo stageInfo, PairFlatMapFunction<T, Object, Object> sinkFunction) {
    stream.foreachRDD(new StreamingBatchSinkFunction<>(sinkFunction, sec, stageInfo.getName()));
  }

  @Override
  public void store(StageInfo stageInfo, SparkSink<T> sink) throws Exception {
    // should never be called.
    throw new UnsupportedOperationException("Spark sink not supported in Spark Streaming.");
  }

  @Override
  public SparkCollection<T> window(StageInfo stageInfo, Windower windower) {
    String stageName = stageInfo.getName();
    return wrap(stream.transform(new CountingTranformFunction<T>(stageName, sec.getMetrics(), "records.in",
                                                                 sec.getDataTracer(stageName)))
                  .window(Durations.seconds(windower.getWidth()), Durations.seconds(windower.getSlideInterval()))
                  .transform(new CountingTranformFunction<T>(stageName, sec.getMetrics(), "records.out",
                                                             sec.getDataTracer(stageName))));
  }

  private <U> SparkCollection<U> wrap(JavaDStream<U> stream) {
    return new DStreamCollection<>(sec, stream);
  }
}
