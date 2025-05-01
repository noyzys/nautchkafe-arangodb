package dev.nautchkafe.arangodb;

interface ArangoConnection {

    ArangoDatabaseOperation connect(final ArangoCredentials credentials);

    void shutdown();
}
