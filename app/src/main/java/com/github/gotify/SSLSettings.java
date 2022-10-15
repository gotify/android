package com.github.gotify;

public class SSLSettings {
    public boolean validateSSL;
    public String cert;
    public String clientCert;
    public String clientCertPassword;

    public SSLSettings(boolean validateSSL, String cert, String clientCert, String clientCertPassword) {
        this.validateSSL = validateSSL;
        this.cert = cert;
        this.clientCert = clientCert;
        this.clientCertPassword = clientCertPassword;
    }
}
