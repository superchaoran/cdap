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

import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.conf.SConfiguration;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * File based keystore for use with SSL.
 */
public class GeneratedCertKeyStoreCreator {

  public static KeyStore getSSLKeyStore(SConfiguration sConf) {

    KeyStore ks;
    try {
      String password = sConf.get(Constants.Security.AppFabric.SSL_KEYSTORE_PASSWORD);
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
      keyGen.initialize(2048, random);

      // generate a keypair
      KeyPair pair = keyGen.generateKeyPair();
      String dn = "CN=Test, L=London, C=GB";
      X509Certificate cert = getCertificate(dn, pair, 100, "SHA1WithRSA");
      InputStream inputStream = new ByteArrayInputStream(cert.getEncoded());

      ks = KeyStore.getInstance(sConf.get(Constants.Security.AppFabric.SSL_KEYSTORE_TYPE));
      ks.load(inputStream, password.toCharArray());
    } catch (Throwable e) {
      throw new RuntimeException("SSL is enabled but the keystore file could not be read. Please verify that the " +
                                   "keystore file exists and the path is set correctly : "
                                   + sConf.get(Constants.Security.AppFabric.SSL_FILE_KEYSTORE_PATH));
    }
    return ks;
  }

  private static X509Certificate getCertificate(String dn, KeyPair pair, int days, String algorithm)
    throws CertificateException, IOException, NoSuchProviderException, NoSuchAlgorithmException,
    InvalidKeyException, SignatureException {
    PrivateKey privkey = pair.getPrivate();
    X509CertInfo info = new X509CertInfo();
    Date from = new Date();
    Date to = new Date(from.getTime() + days * 86400000L);
    CertificateValidity interval = new CertificateValidity(from, to);
    BigInteger sn = new BigInteger(64, new SecureRandom());
    X500Name owner = new X500Name(dn);

    info.set(X509CertInfo.VALIDITY, interval);
    info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
    info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
    info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
    info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
    info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
    AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
    info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

    // Sign the cert to identify the algorithm that's used.
    X509CertImpl cert = new X509CertImpl(info);
    cert.sign(privkey, algorithm);

    // Update the algorith, and resign.
    algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
    info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
    cert = new X509CertImpl(info);
    cert.sign(privkey, algorithm);
    return cert;
  }
}
