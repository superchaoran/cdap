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

package co.cask.cdap.internal.app.preview;

import co.cask.cdap.app.runtime.ProgramRuntimeService;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.logging.LoggingContextAccessor;
import co.cask.cdap.common.logging.ServiceLoggingContext;
import co.cask.cdap.data2.datafabric.dataset.service.DatasetService;
import co.cask.cdap.internal.app.namespace.DefaultNamespaceEnsurer;
import co.cask.cdap.internal.app.runtime.artifact.SystemArtifactLoader;
import co.cask.cdap.internal.app.services.ApplicationLifecycleService;
import co.cask.cdap.internal.app.services.ProgramLifecycleService;
import co.cask.cdap.logging.appender.LogAppenderInitializer;
import co.cask.cdap.proto.Id;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;

/**
 * Service that runs the preview.
 */
public class PreviewRuntimeService extends AbstractIdleService {

  private final DatasetService datasetService;
  private final LogAppenderInitializer logAppenderInitializer;
  private final ApplicationLifecycleService applicationLifecycleService;
  private final SystemArtifactLoader systemArtifactLoader;
  private final ProgramRuntimeService programRuntimeService;
  private final ProgramLifecycleService programLifecycleService;
  private final DefaultNamespaceEnsurer namespaceEnsurer;

  @Inject
  PreviewRuntimeService(DatasetService datasetService, LogAppenderInitializer logAppenderInitializer,
                        ApplicationLifecycleService applicationLifecycleService,
                        SystemArtifactLoader systemArtifactLoader, ProgramRuntimeService programRuntimeService,
                        ProgramLifecycleService programLifecycleService, DefaultNamespaceEnsurer namespaceEnsurer) {
    this.datasetService = datasetService;
    this.logAppenderInitializer = logAppenderInitializer;
    this.applicationLifecycleService = applicationLifecycleService;
    this.systemArtifactLoader = systemArtifactLoader;
    this.programRuntimeService = programRuntimeService;
    this.programLifecycleService = programLifecycleService;
    this.namespaceEnsurer = namespaceEnsurer;
  }

  @Override
  protected void startUp() throws Exception {
    datasetService.startAndWait();

    // It is recommended to initialize log appender after datasetService is started,
    // since log appender instantiates a dataset.
    logAppenderInitializer.initialize();

    LoggingContextAccessor.setLoggingContext(new ServiceLoggingContext(Id.Namespace.SYSTEM.getId(),
                                                                       Constants.Logging.COMPONENT_NAME,
                                                                       Constants.Service.PREVIEW_HTTP));
    Futures.allAsList(
      applicationLifecycleService.start(),
      systemArtifactLoader.start(),
      programRuntimeService.start(),
      programLifecycleService.start()
    ).get();

    namespaceEnsurer.startAndWait();
  }

  @Override
  protected void shutDown() throws Exception {
    namespaceEnsurer.stopAndWait();
    programRuntimeService.stopAndWait();
    applicationLifecycleService.stopAndWait();
    systemArtifactLoader.stopAndWait();
    programLifecycleService.stopAndWait();
    logAppenderInitializer.close();
    datasetService.stopAndWait();
  }
}
