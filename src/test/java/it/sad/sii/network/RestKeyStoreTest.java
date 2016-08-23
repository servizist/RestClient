package it.sad.sii.network;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertNotNull;


/**
 * Created by oskar on 7/13/16.
 */
public class RestKeyStoreTest extends RestTest {

    public static final String INTERNAL_NETWORK_SERVER_ADDRESS = "https://v-theoden.sad.it:8181/";
    public static final String SERVER_USERNAME = "android_wp";
    public static final String SERVER_PASSWORD = "android_wp_secret";

    public static final String KEYSTORE_FILE = "truststore.jks";
    public static final String KEYSTORE_TYPE = "JKS";
    public static final String KEYSTORE_PASSWD = "password";

    @Test
    public void testKeystore() {
        KeyStore ks = null;
        FileInputStream fis = null;
        try {
            ks = KeyStore.getInstance(KEYSTORE_TYPE);
            URL path = getClass().getClassLoader().getResource(KEYSTORE_FILE);
            if (path != null) {
                fis = new FileInputStream(path.getFile());
                ks.load(fis, KEYSTORE_PASSWD.toCharArray());
            }
        } catch (KeyStoreException e) {
            LOG.error(e);
        } catch (CertificateException e) {
            LOG.error(e);
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e);
        } catch (FileNotFoundException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    LOG.error(e);
                }
            }
        }

        // Create RestClient with keystore.
        String jsonResponse = null;
        try {
            RestClient restKS =
                    new RestClient(INTERNAL_NETWORK_SERVER_ADDRESS, SERVER_USERNAME, SERVER_PASSWORD, 5000, ks);
            jsonResponse = restKS.get(INTERNAL_NETWORK_SERVER_ADDRESS + "lines/1916");
        } catch (URISyntaxException e) {
            LOG.error("URISyntaxException in KeyStore test: " + e);
        } catch (IOException e) {
            LOG.error("IOException in KeyStore test: " + e);
        }
        assertNotNull(jsonResponse);
    }
}
