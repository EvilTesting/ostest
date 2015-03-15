package security;

import com.sun.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class TrustManagerSimple implements X509TrustManager {

    @Override
    public boolean isClientTrusted(X509Certificate[] cert) {
        return true;
    }

    @Override
    public boolean isServerTrusted(X509Certificate[] cert) {
        return true;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
