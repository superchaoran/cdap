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

import co.cask.cdap.app.preview.DataTracerFactory;
import co.cask.cdap.proto.id.ApplicationId;

import java.util.HashMap;
import java.util.Map;

/**
 * A class which provides {@link DataTracerFactory} based on the {@link ApplicationId}
 */
public final class DataTracerFactoryProvider {
  private static final DataTracerFactory DEFAULT_FACTORY = new NoopDataTracerFactory();
  private static final Map<ApplicationId, DataTracerFactory> FACTORY_MAP = new HashMap<>();

  private DataTracerFactoryProvider() {
  }

  public static synchronized void setDataTracerFactory(ApplicationId applicationId,
                                                       DataTracerFactory dataTracerFactory) {
    FACTORY_MAP.put(applicationId, dataTracerFactory);
  }

  public static DataTracerFactory get(ApplicationId applicationId) {
    return FACTORY_MAP.containsKey(applicationId) ? FACTORY_MAP.get(applicationId) : DEFAULT_FACTORY;
  }

  public static synchronized  void removeDataTracerFactory(ApplicationId applicationId) {
    FACTORY_MAP.remove(applicationId);
  }
}
