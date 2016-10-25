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
import com.google.common.base.Strings;
import org.apache.commons.lang.time.DateUtils;
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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

/**
 * Utility class with methods for generating a X.509 self signed certificate
 * and creating a Java key store with a self signed certificate.
 */
public class GeneratedCertKeyStoreCreator {
  private static final String KEY_PAIR_ALGORITHM = "RSA";
  private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
  private static final String SECURE_RANDOM_PROVIDER = "SUN";
  private static final String DISTINGUISHED_NAME = "CN=Test, L=London, C=GB";
  private static final String SIGNATURE_ALGORITHM = "MD5withRSA";
  private static final String SSL_KEYSTORE_TYPE = "JKS";
  private static final String CERT_ALIAS = "cert";
  private static final int KEY_SIZE = 2048;
  private static final int VALIDITY = 999;

  /**
   * Create a Java key store with a stored self-signed certificate.
   * @return Java keystore which has a self signed X.509 certificate
   */
  public static KeyStore getSSLKeyStore(SConfiguration sConf) {
    KeyStore keyStore;
    String password = sConf.get(Constants.Security.AppFabric.SSL_KEYSTORE_PASSWORD);
    if (Strings.isNullOrEmpty(password)) {
      throw new RuntimeException("SSL is enabled but a password for the keystore file is not set. Please set " +
                                   "a password in cdap-security.xml using " +
                                   Constants.Security.AppFabric.SSL_KEYSTORE_PASSWORD);
    }
    try {
      String keyPairAlgorithm = sConf.get(Constants.Security.AppFabric.KEY_PAIR_ALGORITHM,
                                          KEY_PAIR_ALGORITHM);
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyPairAlgorithm);
      String randomAlgorithm = sConf.get(Constants.Security.AppFabric.SECURE_RANDOM_ALGORITHM,
                                         SECURE_RANDOM_ALGORITHM);
      String randomProvider = sConf.get(Constants.Security.AppFabric.SECURE_RANDOM_PROVIDER,
                                        SECURE_RANDOM_PROVIDER);
      SecureRandom random = SecureRandom.getInstance(randomAlgorithm, randomProvider);
      keyGen.initialize(KEY_SIZE, random);
      // generate a key pair
      KeyPair pair = keyGen.generateKeyPair();
      int validity = sConf.getInt(Constants.Security.AppFabric.CERT_VALIDITY, VALIDITY);
      String distinguishedName = sConf.get(Constants.Security.AppFabric.CERT_DISTINGUISHED_NAME, DISTINGUISHED_NAME);
      String signatureAlgo = sConf.get(Constants.Security.AppFabric.SIGNATURE_ALGORITHM, SIGNATURE_ALGORITHM);

      X509Certificate cert = getCertificate(distinguishedName, pair, validity, signatureAlgo);
      InputStream inputStream = new ByteArrayInputStream(cert.getEncoded());


      keyStore = KeyStore.getInstance(sConf.get(Constants.Security.AppFabric.SSL_KEYSTORE_TYPE, SSL_KEYSTORE_TYPE));
      keyStore.load(null, password.toCharArray());
      keyStore.setCertificateEntry(CERT_ALIAS, cert);
    } catch (Throwable e) {
      throw new RuntimeException("SSL is enabled but a key store file could not be created. A keystore is required " +
                                   "for SSL to be used.", e);
    }
    return keyStore;
  }

  /**
   * Generate an X.509 certificate
   * @param dn Distinguished name for the owner of the certificate, it will also be the signer of the certificate.
   * @param pair Key pair used for signing the certificate.
   * @param days Validity of the certificate.
   * @param algorithm Name of the signature algorithm used.
   * @return A X.509 certificate
   */
  private static X509Certificate getCertificate(String dn, KeyPair pair, int days, String algorithm) {
//    throws CertificateException, IOException, NoSuchProviderException, NoSuchAlgorithmException,
//    InvalidKeyException, SignatureException {
    // Calculate the validity interval of the certificate
    Date from = new Date();
    Date to = DateUtils.addDays(from, days);
    CertificateValidity interval = new CertificateValidity(from, to);
    // Generate a random number to use as the serial number for the certificate
    BigInteger sn = new BigInteger(64, new SecureRandom());
    X509CertInfo info = null;
    X509CertImpl cert = null;
    // Create the name of the owner based on the provided distinguished name
    try {
      X500Name owner = new X500Name(dn);
      // Create an info objects with the provided information, which will be used to create the certificate
      info = new X509CertInfo();
      info.set(X509CertInfo.VALIDITY, interval);
      info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
      // This certificate will be self signed, hence the subject and the issuer are same.
      info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
      info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
      info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
      info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
      AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
      info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
      // Create the certificate and sign it with the private key
      cert = new X509CertImpl(info);
      PrivateKey privateKey = pair.getPrivate();
      cert.sign(privateKey, algorithm);
    } catch (CertificateException | IOException | NoSuchAlgorithmException | SignatureException
      | InvalidKeyException | NoSuchProviderException e) {
      throw new RuntimeException("SSL is enabled but an certificate could not be generated. A certificate is required" +
                                   " for SSL to be used.", e);
    }
    return cert;
  }

  public static void main(String[] args) throws KeyStoreException, NoSuchProviderException, CertificateException,
    NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
    SConfiguration sConfiguration = SConfiguration.create();
    sConfiguration.set(Constants.Security.AppFabric.SSL_KEYSTORE_PASSWORD, "pass");
    KeyStore ks = getSSLKeyStore(sConfiguration);
    System.out.println("All good");
    Enumeration<String> aliases = ks.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      System.out.println(alias);
      System.out.println(ks.getCertificate(alias));
    }

    OutputStream fos = new DataOutputStream(Files.newOutputStream(Paths.get("/tmp/test.jks")));
    ks.store(fos, "pass".toCharArray());
    fos.flush();
    fos.close();
  }
}
