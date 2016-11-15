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

import co.cask.cdap.app.preview.PreviewManager;
import co.cask.cdap.gateway.handlers.preview.PreviewHttpHandler;
import co.cask.cdap.internal.app.services.http.AppFabricTestBase;
import co.cask.cdap.proto.id.ApplicationId;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

/**
 * Test for {@link PreviewHttpHandler}.
 */
public class PreviewHttpHandlerTest extends AppFabricTestBase {

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
