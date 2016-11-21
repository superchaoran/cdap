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
import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.ha.HAServiceStatus;
import org.apache.hadoop.yarn.client.RMHAServiceTarget;
import org.apache.hadoop.yarn.conf.HAUtil;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.YarnVersionInfo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

/**
 * {@link OperationalStats} for collecting YARN info.
 */
public class YARNInfo extends AbstractYARNStats implements YARNInfoMXBean {

  @Override
  public String getStatType() {
    return "info";
  }

  @Override
  public String getVersion() {
    return YarnVersionInfo.getVersion();
  }

  @Override
  public String getWebURL() throws IOException {
    return getResourceManager();
  }

  @Override
  public String getLogsURL() throws IOException {
    return getResourceManager() + "/logs";
  }

  @Override
  public void collect() throws IOException {
    // no-op
  }

  private String getResourceManager() throws IOException {
    if (HAUtil.isHAEnabled(conf)) {
      return getHAWebURL().toString();
    }
    String httpPolicy = conf.get(YarnConfiguration.YARN_HTTP_POLICY_KEY, YarnConfiguration.YARN_HTTP_POLICY_DEFAULT);
    String webURL;
    switch (httpPolicy) {
      case "HTTP_ONLY":
        webURL = conf.get(YarnConfiguration.RM_WEBAPP_ADDRESS);
        break;
      case "HTTPS_ONLY":
        webURL = conf.get(YarnConfiguration.RM_WEBAPP_HTTPS_ADDRESS);
        break;
      default:
        throw new IOException("unexpected");
    }
    return webURL;
  }

  private InetSocketAddress getHAWebURL() throws IOException {
    InetSocketAddress activeRM = null;
    Collection<String> rmIds = getRMIds();
    if (rmIds.isEmpty()) {
      throw new IllegalStateException("");
    }
    for (String rmId : rmIds) {
      YarnConfiguration yarnConf = new YarnConfiguration(conf);
      yarnConf.set(YarnConfiguration.RM_HA_ID, rmId);
      RMHAServiceTarget rmhaServiceTarget = new RMHAServiceTarget(conf);
      HAServiceProtocol proxy = rmhaServiceTarget.getProxy(conf, 10000);
      HAServiceStatus serviceStatus = proxy.getServiceStatus();
      if (HAServiceProtocol.HAServiceState.ACTIVE != serviceStatus.getState()) {
        continue;
      }
      activeRM = rmhaServiceTarget.getAddress();
    }
    if (activeRM == null) {
      throw new IllegalStateException("Could not find an active resource manager");
    }
    return activeRM;
  }

  private Collection<String> getRMIds() {
    if (HAUtil.isHAEnabled(conf)) {
      return HAUtil.getRMHAIds(conf);
    }
    return Collections.emptySet();
  }


}
