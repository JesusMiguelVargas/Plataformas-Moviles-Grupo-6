package com.jesus.iot01.data

import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

object SslUtils {

    fun getSocketFactory(
        caCertPem: String,
        clientCertPem: String,
        privateKeyPem: String
    ): SSLSocketFactory {

        val cf = CertificateFactory.getInstance("X.509")

        // CA Certificate
        val caCert = cf.generateCertificate(
            ByteArrayInputStream(caCertPem.trimIndent().toByteArray())
        ) as X509Certificate

        // Client Certificate
        val clientCert = cf.generateCertificate(
            ByteArrayInputStream(clientCertPem.trimIndent().toByteArray())
        ) as X509Certificate

        // ✅ Private Key — ahora en formato PKCS8
        val privateKeyPemClean = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()

        val keyBytes = Base64.getDecoder().decode(privateKeyPemClean)
        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(keyBytes))

        // TrustStore con CA
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca-cert", caCert)
        }

        val tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        ).apply { init(trustStore) }

        // KeyStore con certificado cliente + clave privada
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("client-cert", clientCert)
            setKeyEntry(
                "private-key",
                privateKey,
                "".toCharArray(),
                arrayOf(clientCert)
            )
        }

        val kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        ).apply { init(keyStore, "".toCharArray()) }

        // SSLContext final
        return SSLContext.getInstance("TLSv1.2").apply {
            init(kmf.keyManagers, tmf.trustManagers, null)
        }.socketFactory
    }
}