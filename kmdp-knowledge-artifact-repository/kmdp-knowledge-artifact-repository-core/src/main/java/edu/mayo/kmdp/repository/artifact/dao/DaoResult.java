package edu.mayo.kmdp.repository.artifact.dao;

import java.io.Closeable;

public interface DaoResult<T> extends Closeable {

  T getValue();

  @Override
  default void close() {
    // nothing to close
  }

  default boolean isSuccess() {
    return getValue() != null;
  }

}
