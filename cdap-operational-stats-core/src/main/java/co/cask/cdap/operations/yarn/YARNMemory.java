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

package co.cask.cdap.operations.yarn;

import co.cask.cdap.operations.OperationalStats;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

import java.io.IOException;
import java.util.List;

/**
 * {@link OperationalStats} for reporting YARN memory stats.
 */
public class YARNMemory extends AbstractYARNStats implements YARNMemoryMXBean {

  private int total;
  private int used;
  private int free;

  @Override
  public String getStatType() {
    return "memory";
  }

  @Override
  public long getTotal() {
    return 0;
  }

  @Override
  public long getUsed() {
    return 0;
  }

  @Override
  public long getFree() {
    return 0;
  }

  @Override
  public void collect() throws IOException {
    YarnClient yarnClient = createYARNClient();
    try {
      List<QueueInfo> rootQueueInfos = yarnClient.getRootQueueInfos();
      for (QueueInfo rootQueueInfo : rootQueueInfos) {
        List<QueueInfo> childQueueInfos = yarnClient.getChildQueueInfos(rootQueueInfo.getQueueName());
        for (QueueInfo childQueueInfo : childQueueInfos) {
        }
      }
    } catch (YarnException e) {
      throw new IOException(e);
    }
  }
}
