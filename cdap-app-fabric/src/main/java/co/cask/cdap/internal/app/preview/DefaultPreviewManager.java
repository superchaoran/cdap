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

import co.cask.cdap.api.artifact.ArtifactScope;
import co.cask.cdap.api.metrics.MetricTimeSeries;
import co.cask.cdap.app.preview.PreviewManager;
import co.cask.cdap.app.preview.PreviewStatus;
import co.cask.cdap.app.runtime.ProgramController;
import co.cask.cdap.app.runtime.ProgramRuntimeService;
import co.cask.cdap.app.store.preview.PreviewStore;
import co.cask.cdap.common.NotFoundException;
import co.cask.cdap.internal.app.deploy.ProgramTerminator;
import co.cask.cdap.internal.app.runtime.AbstractListener;
import co.cask.cdap.internal.app.services.ApplicationLifecycleService;
import co.cask.cdap.internal.app.services.ProgramLifecycleService;
import co.cask.cdap.internal.app.services.PropertiesResolver;
import co.cask.cdap.proto.Id;
import co.cask.cdap.proto.ProgramType;
import co.cask.cdap.proto.artifact.AppRequest;
import co.cask.cdap.proto.artifact.ArtifactSummary;
import co.cask.cdap.proto.id.ApplicationId;
import co.cask.cdap.proto.id.NamespaceId;
import co.cask.cdap.proto.id.ProgramId;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import org.apache.twill.api.logging.LogEntry;
import org.apache.twill.common.Threads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Default implementation of the {@link PreviewManager}.
 */
public class DefaultPreviewManager implements PreviewManager {
  private static final Gson GSON = new Gson();
  private final ApplicationLifecycleService applicationLifecycleService;
  private final ProgramLifecycleService programLifecycleService;
  private final PreviewStore previewStore;
  private final PropertiesResolver propertiesResolver;
  private PreviewStatus status;
  private AppRequest request;

  @Inject
  DefaultPreviewManager(ApplicationLifecycleService applicationLifecycleService,
                        ProgramLifecycleService programLifecycleService, PreviewStore previewStore,
                        PropertiesResolver propertiesResolver) {
    this.applicationLifecycleService = applicationLifecycleService;
    this.programLifecycleService = programLifecycleService;
    this.previewStore = previewStore;
    this.propertiesResolver = propertiesResolver;
    this.status = null;
  }

  @Override
  public void start(ApplicationId preview, AppRequest<?> request) throws Exception {
    this.request = request;
    ArtifactSummary artifactSummary = request.getArtifact();
    NamespaceId artifactNamespace = ArtifactScope.SYSTEM.equals((artifactSummary.getScope())) ? NamespaceId.SYSTEM
      : preview.getParent();

    Id.Artifact artifactId =
      Id.Artifact.from(artifactNamespace.toId(), artifactSummary.getName(), artifactSummary.getVersion());

    String config = request.getConfig() == null ? null : GSON.toJson(request.getConfig());

    try {
      applicationLifecycleService.deployApp(preview.getParent(), preview.getApplication(), preview.getVersion(),
                                            artifactId, config, new ProgramTerminator() {
        @Override
        public void stop(ProgramId programId) throws Exception {

        }
      });
    } catch (Exception e) {
      this.status = new PreviewStatus(PreviewStatus.Status.DEPLOY_FAILED, e);
      return;
    }

    ProgramId programId = getProgramIdFromRequest(preview, request);
    Map<String, String> sysArgs = propertiesResolver.getSystemProperties(programId.toId());
    Map<String, String> userArgs = propertiesResolver.getUserProperties(programId.toId());
    ProgramRuntimeService.RuntimeInfo runtimeInfo = programLifecycleService.start(programId, sysArgs, userArgs, false);

    runtimeInfo.getController().addListener(new AbstractListener() {
      @Override
      public void init(ProgramController.State currentState, @Nullable Throwable cause) {
        status = new PreviewStatus(PreviewStatus.Status.RUNNING, null);
      }

      @Override
      public void completed() {
        status = new PreviewStatus(PreviewStatus.Status.COMPLETED, null);
      }

      @Override
      public void killed() {
        status = new PreviewStatus(PreviewStatus.Status.KILLED, null);
      }

      @Override
      public void error(Throwable cause) {
        status = new PreviewStatus(PreviewStatus.Status.RUN_FAILED, cause);
      }
    }, Threads.SAME_THREAD_EXECUTOR);
  }

  private ProgramId getProgramIdFromRequest(ApplicationId preview, AppRequest request) {
    if (request.getPreview() == null) {
      return preview.workflow("DataPipelineWorkflow");
    }

    String programName = request.getPreview().getProgramName();
    ProgramType programType = request.getPreview().getProgramType();

    if (programName == null || programType == null) {
      throw new IllegalArgumentException("ProgramName or ProgramType cannot be null.");
    }

    return preview.program(programType, programName);
  }

  @Override
  public PreviewStatus getStatus(ApplicationId preview) throws NotFoundException {
    return status;
  }

  @Override
  public void stop(ApplicationId preview) throws Exception {
    programLifecycleService.stop(getProgramIdFromRequest(preview, request));
  }

  @Override
  public List<String> getTracers(ApplicationId preview) throws NotFoundException {
    return new ArrayList<>();
  }

  @Override
  public Map<String, List<JsonElement>> getData(ApplicationId preview, String tracerName) {
    return previewStore.get(preview, tracerName);
  }

  @Override
  public Collection<MetricTimeSeries> getMetrics(ApplicationId preview) throws NotFoundException {
    return null;
  }

  @Override
  public List<LogEntry> getLogs(ApplicationId preview) throws NotFoundException {
    return null;
  }
}
