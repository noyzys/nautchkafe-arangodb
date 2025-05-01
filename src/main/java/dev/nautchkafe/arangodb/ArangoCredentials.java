package dev.nautchkafe.arangodb;

record ArangoCredentials(
        String hostname,
        int port,
        String user,
        String password,
        String databaseName,
        boolean useSsl
) {
    ArangoCredentials {
        final ArangoTry<Void> validation = ArangoTry.allOf(
                ArangoValidation.requireNonBlank(hostname, "Host cannot be blank"),
                ArangoValidation.validate(port, p -> p > 0 && p <= 65535,
                        () -> new IllegalArgumentException("Port must be between 1 and 65535")),

                ArangoValidation.requireNonBlank(user, "User cannot be blank"),
                ArangoValidation.requireNonBlank(databaseName, "Database name cannot be blank")
        );

        validation.fold(success -> {},
                error -> { throw new IllegalArgumentException(error.getMessage()); });
    }

    ArangoCredentials(final String host, final int port,
                      final String user, final String password, final String databaseName) {
        this(host, port, user, password, databaseName, false);
    }
}
