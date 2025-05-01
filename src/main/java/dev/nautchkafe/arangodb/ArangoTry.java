package dev.nautchkafe.arangodb;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

sealed interface ArangoTry<TYPE> permits ArangoTry.Sucess, ArangoTry.Failure {

    static <TYPE> ArangoTry<TYPE> of(final Supplier<TYPE> supplier) {
        try {
            return new Sucess<>(supplier.get());
        } catch (final Exception e) {
            return new Failure<>(e);
        }
    }

    static <TYPE> ArangoTry<TYPE> success(final TYPE value) {
        return new Sucess<>(value);
    }

    static <TYPE> ArangoTry<TYPE> failure(final Exception error) {
        return new Failure<>(error);
    }

    <RESULT> ArangoTry<RESULT> map(final Function<TYPE, RESULT> mapper);
    <RESULT> ArangoTry<RESULT> flatMap(final Function<TYPE, ArangoTry<RESULT>> mapper);

    ArangoTry<TYPE> recover(final Function<Exception, TYPE> recovery);
    ArangoTry<TYPE> recoverWith(final Function<Exception, ArangoTry<TYPE>> recovery);

    void fold(final Consumer<TYPE> onSucess, final Consumer<Exception> onFailure);

    TYPE orElse(final TYPE defaultValue);
    TYPE orElseThrow() throws Exception;

    boolean isSuccess();
    boolean isFailure();

    record Sucess<TYPE>(TYPE value) implements ArangoTry<TYPE> {

        @Override
        public <RESULT> ArangoTry<RESULT> map(final Function<TYPE, RESULT> mapper) {
            return ArangoTry.of((() -> mapper.apply(value)));
        }

        @Override
        public <RESULT> ArangoTry<RESULT> flatMap(final Function<TYPE, ArangoTry<RESULT>> mapper) {
            try {
                return mapper.apply(value);
            } catch (final Exception e) {
                return new Failure<>(e);
            }
        }

        @Override
        public ArangoTry<TYPE> recover(final Function<Exception, TYPE> recovery) {
            return this;
        }

        @Override
        public ArangoTry<TYPE> recoverWith(final Function<Exception, ArangoTry<TYPE>> recovery) {
            return this;
        }

        @Override
        public void fold(final Consumer<TYPE> onSucess, final Consumer<Exception> onFailure) {
            onSucess.accept(value);
        }

        @Override
        public TYPE orElse(final TYPE defaultValue) {
            return value;
        }

        @Override
        public TYPE orElseThrow() throws Exception {
            return value;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }
    }

    record Failure<TYPE>(Exception error) implements ArangoTry<TYPE> {

        @Override
        public <RESULT> ArangoTry<RESULT> map(final Function<TYPE, RESULT> mapper) {
            return new Failure<>(error);
        }

        @Override
        public <RESULT> ArangoTry<RESULT> flatMap(final Function<TYPE, ArangoTry<RESULT>> mapper) {
            return new Failure<>(error);
        }

        @Override
        public ArangoTry<TYPE> recover(final Function<Exception, TYPE> recovery) {
            return ArangoTry.of(() -> recovery.apply(error));
        }

        @Override
        public ArangoTry<TYPE> recoverWith(final Function<Exception, ArangoTry<TYPE>> recovery) {
            try {
                return recovery.apply(error);
            } catch (final Exception e) {
                return new Failure<>(e);
            }
        }

        @Override
        public void fold(final Consumer<TYPE> onSucess, final Consumer<Exception> onFailure) {
            onFailure.accept(error);
        }

        @Override
        public TYPE orElse(final TYPE defaultValue) {
            return defaultValue;
        }

        @Override
        public TYPE orElseThrow() throws Exception {
            throw error;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }
    }
}
