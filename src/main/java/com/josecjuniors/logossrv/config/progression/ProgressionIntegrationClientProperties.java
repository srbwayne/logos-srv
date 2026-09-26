package com.josecjuniors.logossrv.config.progression;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "logos.progression.integration")
public class ProgressionIntegrationClientProperties {
    private List<Client> clients = new ArrayList<>();

    public List<Client> getClients() { return clients; }
    public void setClients(List<Client> clients) { this.clients = clients == null ? new ArrayList<>() : clients; }

    public static class Client {
        private String clientId;
        private String secret;
        private List<String> allowedSources = new ArrayList<>();
        private List<String> allowedNamespaces = new ArrayList<>();

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public List<String> getAllowedSources() { return allowedSources; }
        public void setAllowedSources(List<String> allowedSources) { this.allowedSources = allowedSources == null ? new ArrayList<>() : allowedSources; }
        public List<String> getAllowedNamespaces() { return allowedNamespaces; }
        public void setAllowedNamespaces(List<String> allowedNamespaces) { this.allowedNamespaces = allowedNamespaces == null ? new ArrayList<>() : allowedNamespaces; }
    }
}
