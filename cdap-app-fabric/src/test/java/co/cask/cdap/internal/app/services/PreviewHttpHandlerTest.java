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
package co.cask.cdap.internal.app.services;

import co.cask.cdap.app.guice.AppFabricServiceRuntimeModule;
import co.cask.cdap.app.guice.AuthorizationModule;
import co.cask.cdap.app.guice.ProgramRunnerRuntimeModule;
import co.cask.cdap.app.guice.ServiceStoreModules;
import co.cask.cdap.app.preview.PreviewManager;
import co.cask.cdap.app.preview.PreviewServerModule;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.guice.ConfigModule;
import co.cask.cdap.common.guice.DiscoveryRuntimeModule;
import co.cask.cdap.common.guice.IOModule;
import co.cask.cdap.common.guice.NonCustomLocationUnitTestModule;
import co.cask.cdap.config.guice.ConfigStoreModule;
import co.cask.cdap.data.runtime.DataFabricModules;
import co.cask.cdap.data.runtime.DataSetServiceModules;
import co.cask.cdap.data.runtime.DataSetsModules;
import co.cask.cdap.data.runtime.TransactionExecutorModule;
import co.cask.cdap.data.stream.StreamAdminModules;
import co.cask.cdap.data.stream.service.StreamServiceRuntimeModule;
import co.cask.cdap.data.view.ViewAdminModules;
import co.cask.cdap.explore.guice.ExploreClientModule;
import co.cask.cdap.gateway.handlers.meta.RemoteSystemOperationsServiceModule;
import co.cask.cdap.gateway.handlers.preview.PreviewHttpHandler;
import co.cask.cdap.logging.guice.LogReaderRuntimeModules;
import co.cask.cdap.logging.guice.LoggingModules;
import co.cask.cdap.metadata.MetadataServiceModule;
import co.cask.cdap.metrics.guice.MetricsClientRuntimeModule;
import co.cask.cdap.metrics.guice.MetricsHandlerModule;
import co.cask.cdap.notifications.feeds.guice.NotificationFeedServiceRuntimeModule;
import co.cask.cdap.notifications.guice.NotificationServiceRuntimeModule;
import co.cask.cdap.proto.id.ApplicationId;
import co.cask.cdap.security.authorization.AuthorizationEnforcementModule;
import co.cask.cdap.security.guice.SecureStoreModules;
import co.cask.cdap.store.guice.NamespaceStoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;

/**
 * Test for {@link PreviewHttpHandler}.
 */
public class PreviewHttpHandlerTest {

  private static Injector injector;

  @BeforeClass
  public static void beforeClass() throws Throwable {
    injector = Guice.createInjector(
      new ConfigModule(CConfiguration.create(), new Configuration()),
      new IOModule(),
      new DataFabricModules().getInMemoryModules(),
      new DataSetsModules().getStandaloneModules(),
      new TransactionExecutorModule(),
      new DataSetServiceModules().getInMemoryModules(),
      new DiscoveryRuntimeModule().getInMemoryModules(),
      new AppFabricServiceRuntimeModule().getInMemoryModules(),
      new ServiceStoreModules().getInMemoryModules(),
      new ProgramRunnerRuntimeModule().getInMemoryModules(),
      new NonCustomLocationUnitTestModule().getModule(),
      new LoggingModules().getInMemoryModules(),
      new LogReaderRuntimeModules().getInMemoryModules(),
      new MetricsHandlerModule(),
      new MetricsClientRuntimeModule().getInMemoryModules(),
      new ExploreClientModule(),
      new NotificationFeedServiceRuntimeModule().getInMemoryModules(),
      new NotificationServiceRuntimeModule().getInMemoryModules(),
      new ConfigStoreModule().getInMemoryModule(),
      new ViewAdminModules().getInMemoryModules(),
      new StreamAdminModules().getInMemoryModules(),
      new StreamServiceRuntimeModule().getInMemoryModules(),
      new NamespaceStoreModule().getStandaloneModules(),
      new MetadataServiceModule(),
      new RemoteSystemOperationsServiceModule(),
      new AuthorizationModule(),
      new AuthorizationEnforcementModule().getStandaloneModules(),
      new SecureStoreModules().getInMemoryModules(),
      new PreviewServerModule()
    );
  }

  private Injector getInjector() {
    return injector;
  }

  @Test
  public void testInjector() throws Exception {
    // Make sure same instance of the PreviewHttpHandler is returned
    Assert.assertEquals(getInjector().getInstance(PreviewHttpHandler.class),
                        getInjector().getInstance(PreviewHttpHandler.class));

    PreviewHttpHandler handler = getInjector().getInstance(PreviewHttpHandler.class);
    Injector previewInjector = handler.createPreviewInjector(new ApplicationId("ns1", "app1"), new HashSet<String>());

    // Make sure same PreviewManager instance is returned for a same preview
    Assert.assertEquals(previewInjector.getInstance(PreviewManager.class),
                        previewInjector.getInstance(PreviewManager.class));

    Injector anotherPreviewInjector
      = handler.createPreviewInjector(new ApplicationId("ns2", "app2"), new HashSet<String>());

    Assert.assertNotEquals(previewInjector.getInstance(PreviewManager.class),
                           anotherPreviewInjector.getInstance(PreviewManager.class));
  }
}
