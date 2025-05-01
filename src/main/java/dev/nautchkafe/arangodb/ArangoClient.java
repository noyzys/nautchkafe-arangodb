package dev.nautchkafe.arangodb;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBAsync;
import com.arangodb.internal.ArangoDBAsyncImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ArangoClient implements ArangoConnection {

    private final ExecutorService executorService;

    ArangoClient(final ExecutorService executorService) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public ArangoDatabaseOperation connect(final ArangoCredentials credentials) {
        return ArangoValidation.requireNonNull(credentials, "Credentials  cannot be null")
                .orElseThrow();
    }

    private ArangoTry<ArangoDatabaseOperation> createDatabase(final ArangoCredentials credentials) {
        return ArangoTry.of(() -> {
           final ArangoDB arangoDriver = new ArangoDB.Builder()
                   .host(credentials.hostname(), credentials.port())
                   .user(credentials.user())
                   .password(credentials.password())
                   .useSsl(credentials.useSsl())
                   .build();

           arangoDriver.async();

           return null;
        });
    }

    @Override
    public void shutdown() {

    }
}
