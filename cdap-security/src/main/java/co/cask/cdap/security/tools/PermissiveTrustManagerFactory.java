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

package co.cask.cdap.security.tools;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * {@link TrustManagerFactorySpi} which accepts any certificate.
 */
public class PermissiveTrustManagerFactory extends TrustManagerFactorySpi {


  private static final TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
      // Always trust - it is an example.
      // You should do something in the real world.
      // You will reach here only if you enabled client certificate auth,
      // as described in SecureChatSslContextFactory.
      System.err.println(
        "UNKNOWN CLIENT CERTIFICATE: " + chain[0].getSubjectDN());
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
      // Always trust - it is an example.
      // You should do something in the real world.
      System.err.println(
        "UNKNOWN SERVER CERTIFICATE: " + chain[0].getSubjectDN());
    }
  };

  public static TrustManager[] getTrustManagers() {
    return new TrustManager[] { DUMMY_TRUST_MANAGER };
  }

  @Override
  protected void engineInit(KeyStore keyStore) throws KeyStoreException {

  }

  @Override
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
    throws InvalidAlgorithmParameterException {

  }

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return getTrustManagers();
  }
}
