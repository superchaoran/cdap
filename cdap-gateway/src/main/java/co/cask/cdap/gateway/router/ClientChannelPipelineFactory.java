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

package co.cask.cdap.gateway.router;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.gateway.router.handlers.IdleEventProcessor;
import co.cask.cdap.security.tools.PermissiveTrustManagerFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
  Constructs a pipeline factory to be used for creating client pipelines.
 */
class ClientChannelPipelineFactory implements ChannelPipelineFactory {

  private final CConfiguration cConf;
  private final ChannelUpstreamHandler connectionTracker;
  private final int connectionTimeout;
  private final Timer timer;

  ClientChannelPipelineFactory(CConfiguration cConf, ChannelUpstreamHandler connectionTracker, int connectionTimeout,
                               Timer timer) {
    this.cConf = cConf;
    this.connectionTracker = connectionTracker;
    this.connectionTimeout = connectionTimeout;
    this.timer = timer;
  }

  @Override
  public ChannelPipeline getPipeline() throws Exception {
    boolean appSslEnabled = cConf.getBoolean(Constants.Security.AppFabric.SSL_ENABLED);
    SSLEngine engine = null;
    if (appSslEnabled) {
      SSLContext clientContext = null;
      try {
        clientContext = SSLContext.getInstance("TLS");
        clientContext.init(null, PermissiveTrustManagerFactory.getTrustManagers(), null);
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        throw new RuntimeException("SSL is enabled for app-fabric but failed to create SSLContext in the router " +
                                     "client.", e);
      }
      engine = clientContext.createSSLEngine();
      engine.setUseClientMode(true);
    }
    ChannelPipeline pipeline = Channels.pipeline();
    if (appSslEnabled) {
      pipeline.addFirst("ssl", new SslHandler(engine));
    }
    pipeline.addLast("tracker", connectionTracker);
    pipeline.addLast("request-encoder", new HttpRequestEncoder());
    // outbound handler gets dynamically added here (after 'request-encoder')
    pipeline.addLast("response-decoder", new HttpResponseDecoder());
    // disable the read-specific and write-specific timeouts; we only utilize IdleState#ALL_IDLE
    pipeline.addLast("idle-event-generator",
                     new IdleStateHandler(timer, 0, 0, connectionTimeout));
    pipeline.addLast("idle-event-processor", new IdleEventProcessor());
    return pipeline;
  }
}
