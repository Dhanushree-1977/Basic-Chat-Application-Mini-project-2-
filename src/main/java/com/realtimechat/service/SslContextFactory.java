package com.realtimechat.service;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Factory class responsible for configuring TLS/SSL encryption.
 * Initializes SSLContext using KeyStore for the Server and TrustStore for the Client,
 * enabling secure communication with SSLServerSocket and SSLSocket.
 */
public class SslContextFactory {

    public static final String PROTOCOL = "TLSv1.3";
    public static final String KEYSTORE_PATH = "src/main/resources/security/server.keystore";
    public static final String TRUSTSTORE_PATH = "src/main/resources/security/client.truststore";
    public static final char[] STORE_PASSWORD = "chatpassword123".toCharArray();

    /**
     * Creates and initializes a secure SSLServerSocket for the server.
     *
     * @param port listening port
     * @return configured SSLServerSocket
     * @throws Exception if keystore loading or SSLContext initialization fails
     */
    public static SSLServerSocket createSSLServerSocket(int port) throws Exception {
        // 1. Load Server KeyStore (contains Server Private Key + Certificate)
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream ksStream = openStream(KEYSTORE_PATH, "security/server.keystore")) {
            keyStore.load(ksStream, STORE_PASSWORD);
        }

        // 2. Initialize KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, STORE_PASSWORD);

        // 3. Initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
        sslContext.init(kmf.getKeyManagers(), null, null);

        // 4. Create SSLServerSocket
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);

        // Limit to modern secure TLS protocols
        serverSocket.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
        return serverSocket;
    }

    /**
     * Creates and initializes a secure SSLSocket for the client.
     *
     * @param host server host
     * @param port server port
     * @return connected and handshaked SSLSocket
     * @throws Exception if truststore loading or TLS handshake fails
     */
    public static SSLSocket createSSLSocket(String host, int port) throws Exception {
        // 1. Load Client TrustStore (contains trusted Server Public Certificate)
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream tsStream = openStream(TRUSTSTORE_PATH, "security/client.truststore")) {
            trustStore.load(tsStream, STORE_PASSWORD);
        }

        // 2. Initialize TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 3. Initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
        sslContext.init(null, tmf.getTrustManagers(), null);

        // 4. Create and connect SSLSocket
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

        socket.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});

        // Explicitly trigger TLS handshake to fail-fast if certificate is invalid
        socket.startHandshake();
        System.out.println("[SslContextFactory] TLS Handshake complete with cipher: " + 
                           socket.getSession().getCipherSuite());

        return socket;
    }

    private static InputStream openStream(String filesystemPath, String classpathResource) throws Exception {
        // First try filesystem
        File file = new File(filesystemPath);
        if (file.exists()) {
            return new FileInputStream(file);
        }

        // Fallback to classpath
        InputStream in = SslContextFactory.class.getClassLoader().getResourceAsStream(classpathResource);
        if (in != null) {
            return in;
        }

        throw new IllegalStateException("Could not find keystore/truststore file at '" + 
                                       filesystemPath + "' or classpath '" + classpathResource + "'");
    }
}
