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

package co.cask.cdap.gateway.handlers.preview;

import co.cask.cdap.api.security.store.SecureStore;
import co.cask.cdap.app.guice.ProgramRunnerRuntimeModule;
import co.cask.cdap.app.preview.PreviewManager;
import co.cask.cdap.app.preview.PreviewModule;
import co.cask.cdap.common.BadRequestException;
import co.cask.cdap.common.NotFoundException;
import co.cask.cdap.common.app.RunIds;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.guice.ConfigModule;
import co.cask.cdap.common.guice.IOModule;
import co.cask.cdap.common.guice.LocationRuntimeModule;
import co.cask.cdap.common.guice.preview.PreviewDiscoveryRuntimeModule;
import co.cask.cdap.common.utils.Networks;
import co.cask.cdap.config.PreferencesStore;
import co.cask.cdap.config.guice.ConfigStoreModule;
import co.cask.cdap.data.runtime.DataSetServiceModules;
import co.cask.cdap.data.runtime.preview.PreviewDataModules;
import co.cask.cdap.data.stream.StreamCoordinatorClient;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.data2.transaction.stream.StreamAdmin;
import co.cask.cdap.data2.transaction.stream.StreamConsumerFactory;
import co.cask.cdap.data2.transaction.stream.inmemory.InMemoryStreamConsumerFactory;
import co.cask.cdap.explore.client.ExploreClient;
import co.cask.cdap.explore.client.MockExploreClient;
import co.cask.cdap.gateway.handlers.meta.RemoteSystemOperationsServiceModule;
import co.cask.cdap.gateway.handlers.util.AbstractAppFabricHttpHandler;
import co.cask.cdap.internal.app.namespace.LocalStorageProviderNamespaceAdmin;
import co.cask.cdap.internal.app.namespace.StorageProviderNamespaceAdmin;
import co.cask.cdap.internal.app.runtime.artifact.ArtifactRepository;
import co.cask.cdap.internal.app.runtime.artifact.ArtifactStore;
import co.cask.cdap.logging.guice.LoggingModules;
import co.cask.cdap.metrics.guice.MetricsClientRuntimeModule;
import co.cask.cdap.proto.artifact.AppRequest;
import co.cask.cdap.proto.id.ApplicationId;
import co.cask.cdap.proto.id.NamespaceId;
import co.cask.cdap.security.auth.context.AuthenticationContextModules;
import co.cask.cdap.security.authorization.AuthorizerInstantiator;
import co.cask.cdap.security.guice.SecurityModules;
import co.cask.cdap.security.guice.preview.PreviewSecureStoreModule;
import co.cask.cdap.security.spi.authorization.AuthorizationEnforcer;
import co.cask.cdap.security.spi.authorization.PrivilegesManager;
import co.cask.cdap.store.guice.NamespaceStoreModule;
import co.cask.http.HttpResponder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.hadoop.conf.Configuration;
import org.apache.tephra.TransactionManager;
import org.apache.twill.discovery.DiscoveryService;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * /**
 * {@link co.cask.http.HttpHandler} to manage preview lifecycle for v3 REST APIs
 */
@Singleton
@Path(Constants.Gateway.API_VERSION_3 + "/namespaces/{namespace-id}")
public class PreviewHttpHandler extends AbstractAppFabricHttpHandler {

  private static final Gson GSON = new Gson();
  private static final Logger LOG = LoggerFactory.getLogger(PreviewHttpHandler.class);

  private final CConfiguration cConf;
  private final Configuration hConf;
  private final DiscoveryService discoveryService;
  private final DatasetFramework datasetFramework;
  private final PreferencesStore preferencesStore;
  private final SecureStore secureStore;
  private final TransactionManager transactionManager;
  private final ArtifactRepository artifactRepository;
  private final ArtifactStore artifactStore;
  private final AuthorizerInstantiator authorizerInstantiator;
  private final StreamAdmin streamAdmin;
  private final StreamCoordinatorClient streamCoordinatorClient;
  private final PrivilegesManager privilegesManager;
  private final AuthorizationEnforcer authorizationEnforcer;
  private final Cache<ApplicationId, Injector> appInjectors;

  @Inject
  PreviewHttpHandler(final CConfiguration cConf, Configuration hConf, DiscoveryService discoveryService,
                            DatasetFramework datasetFramework, PreferencesStore preferencesStore,
                            SecureStore secureStore, TransactionManager transactionManager,
                            ArtifactRepository artifactRepository, ArtifactStore artifactStore,
                            AuthorizerInstantiator authorizerInstantiator, StreamAdmin streamAdmin,
                            StreamCoordinatorClient streamCoordinatorClient, PrivilegesManager privilegesManager,
                            AuthorizationEnforcer authorizationEnforcer) {
    this.cConf = cConf;
    this.hConf = hConf;
    this.datasetFramework = datasetFramework;
    this.discoveryService = discoveryService;
    this.preferencesStore = preferencesStore;
    this.secureStore = secureStore;
    this.transactionManager = transactionManager;
    this.artifactRepository = artifactRepository;
    this.artifactStore = artifactStore;
    this.authorizerInstantiator = authorizerInstantiator;
    this.streamAdmin = streamAdmin;
    this.streamCoordinatorClient = streamCoordinatorClient;
    this.privilegesManager = privilegesManager;
    this.authorizationEnforcer = authorizationEnforcer;
    // TODO make maximum size and expire after write configurable?
    this.appInjectors = CacheBuilder.newBuilder()
      .maximumSize(10)
      .expireAfterWrite(15, TimeUnit.MINUTES)
      .removalListener(new RemovalListener<ApplicationId, Injector>() {
        @Override
        @ParametersAreNonnullByDefault
        public void onRemoval(RemovalNotification<ApplicationId, Injector> notification) {
          ApplicationId application = notification.getKey();
          if (application == null) {
            return;
          }
          java.nio.file.Path previewDirPath = Paths.get(cConf.get(Constants.CFG_LOCAL_DATA_DIR),
                                                        "preview", application.getApplication()).toAbsolutePath();

          try {
            Files.deleteIfExists(previewDirPath);
          } catch (IOException e) {
            LOG.warn("Error deleting the preview directory {}", previewDirPath, e);
          }
        }
      })
      .build();
  }

  @POST
  @Path("/previews")
  public void start(HttpRequest request, HttpResponder responder,
                    @PathParam("namespace-id") String namespaceId) throws Exception {
    NamespaceId namespace = new NamespaceId(namespaceId);
    ApplicationId application = namespace.app(RunIds.generate().getId());

    AppRequest appRequest;
    try (Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8)) {
      appRequest = GSON.fromJson(reader, AppRequest.class);
    } catch (IOException e) {
      LOG.error("Error reading request body.", e);
      throw new IOException("Error reading request body.");
    } catch (JsonSyntaxException e) {
      throw new BadRequestException("Request body is invalid json: " + e.getMessage());
    }

    Set<String> realDatasets = appRequest.getPreview() == null ? new HashSet<String>()
      : appRequest.getPreview().getRealDatasets();

    Injector injector = createPreviewInjector(application, realDatasets);
    appInjectors.put(application, injector);
    PreviewManager manager = injector.getInstance(PreviewManager.class);
    manager.start(application, appRequest);
    responder.sendString(HttpResponseStatus.OK, GSON.toJson(application));
  }

  @POST
  @Path("/previews/{preview-id}/stop")
  public void stop(HttpRequest request, HttpResponder responder, @PathParam("namespace-id") String namespaceId,
                   @PathParam("preview-id") String previewId) throws Exception {
    NamespaceId namespace = new NamespaceId(namespaceId);
    ApplicationId application = namespace.app(previewId);

    Injector injector = appInjectors.getIfPresent(application);
    if (injector == null) {
      throw new NotFoundException(application);
    }

    PreviewManager manager = injector.getInstance(PreviewManager.class);
    manager.stop(application);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @GET
  @Path("/previews/{preview-id}/status")
  public void getStatus(HttpRequest request, HttpResponder responder, @PathParam("namespace-id") String namespaceId,
                        @PathParam("preview-id") String previewId)  throws Exception {
    NamespaceId namespace = new NamespaceId(namespaceId);
    ApplicationId application = namespace.app(previewId);

    Injector injector = appInjectors.getIfPresent(application);
    if (injector == null) {
      throw new NotFoundException(application);
    }

    PreviewManager manager = injector.getInstance(PreviewManager.class);
    responder.sendString(HttpResponseStatus.OK, GSON.toJson(manager.getStatus(application)));
  }

  @GET
  @Path("/previews/{preview-id}/tracers")
  public void getTracers(HttpRequest request, HttpResponder responder, @PathParam("namespace-id") String namespaceId,
                         @PathParam("preview-id") String previewId) throws Exception {
    // TODO Implement API in PreviewStore to get all the tracers.
  }

  @GET
  @Path("/previews/{preview-id}/tracers/{tracer-id}")
  public void getData(HttpRequest request, HttpResponder responder, @PathParam("namespace-id") String namespaceId,
                      @PathParam("preview-id") String previewId, @PathParam("tracer-id") String tracerId)
    throws Exception {
    NamespaceId namespace = new NamespaceId(namespaceId);
    ApplicationId application = namespace.app(previewId);

    Injector injector = appInjectors.getIfPresent(application);
    if (injector == null) {
      throw new NotFoundException(application);
    }

    PreviewManager manager = injector.getInstance(PreviewManager.class);
    responder.sendString(HttpResponseStatus.OK, GSON.toJson(manager.getData(application, tracerId)));
  }

  /**
   * Create injector for the given application id.
   */
  @VisibleForTesting
  public Injector createPreviewInjector(ApplicationId applicationId, Set<String> datasetNames) throws IOException {
    CConfiguration previewcConf = CConfiguration.copy(cConf);
    java.nio.file.Path previewDirPath = Paths.get(cConf.get(Constants.CFG_LOCAL_DATA_DIR), "preview").toAbsolutePath();

    Files.createDirectories(previewDirPath);
    java.nio.file.Path previewDir = Files.createTempDirectory(previewDirPath, applicationId.getApplication());
    previewcConf.set(Constants.CFG_LOCAL_DATA_DIR, previewDir.toString());
    previewcConf.set(Constants.Dataset.DATA_DIR, previewDir.toString());
    Configuration previewhConf = new Configuration(hConf);
    previewhConf.set(Constants.CFG_LOCAL_DATA_DIR, previewDir.toString());
    previewcConf.setIfUnset(Constants.CFG_DATA_LEVELDB_DIR, previewDir.toString());

    return Guice.createInjector(
      new ConfigModule(previewcConf, previewhConf),
      new IOModule(),
      new AuthenticationContextModules().getMasterModule(),
      new SecurityModules().getStandaloneModules(),
      new PreviewSecureStoreModule(secureStore),
      new PreviewDiscoveryRuntimeModule(discoveryService),
      new LocationRuntimeModule().getStandaloneModules(),
      new ConfigStoreModule().getStandaloneModule(),
      new PreviewModule(),
      new ProgramRunnerRuntimeModule().getStandaloneModules(),
      new PreviewDataModules().getDataFabricModule(transactionManager),
      new PreviewDataModules().getDataSetsModule(datasetFramework, datasetNames),
      new DataSetServiceModules().getStandaloneModules(),
      new MetricsClientRuntimeModule().getStandaloneModules(),
      new LoggingModules().getStandaloneModules(),
      new NamespaceStoreModule().getStandaloneModules(),
      new RemoteSystemOperationsServiceModule(),
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(ArtifactRepository.class).toInstance(artifactRepository);
          bind(ArtifactStore.class).toInstance(artifactStore);
          bind(AuthorizerInstantiator.class).toInstance(authorizerInstantiator);
          bind(AuthorizationEnforcer.class).toInstance(authorizationEnforcer);
          bind(PrivilegesManager.class).toInstance(privilegesManager);
          bind(StreamAdmin.class).toInstance(streamAdmin);
          bind(StreamConsumerFactory.class).to(InMemoryStreamConsumerFactory.class).in(Singleton.class);
          bind(StreamCoordinatorClient.class).toInstance(streamCoordinatorClient);
          bind(PreferencesStore.class).toInstance(preferencesStore);
          // bind explore client to mock.
          bind(ExploreClient.class).to(MockExploreClient.class);
          bind(StorageProviderNamespaceAdmin.class).to(LocalStorageProviderNamespaceAdmin.class);
        }

        @Provides
        @Named(Constants.Service.MASTER_SERVICES_BIND_ADDRESS)
        @SuppressWarnings("unused")
        public InetAddress providesHostname(CConfiguration cConf) {
          String address = cConf.get(Constants.Preview.ADDRESS);
          return Networks.resolve(address, new InetSocketAddress("localhost", 0).getAddress());
        }
      }
    );
  }
}
