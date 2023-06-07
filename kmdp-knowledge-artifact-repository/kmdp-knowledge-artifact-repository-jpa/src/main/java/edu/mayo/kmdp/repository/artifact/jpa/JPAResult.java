package edu.mayo.kmdp.repository.artifact.jpa;

import edu.mayo.kmdp.repository.artifact.dao.DaoResult;

public class JPAResult<T> implements DaoResult<T> {

  private final T value;

  <X extends T> JPAResult(X value) {
    this.value = value;
  }

  @Override
  public void close() {
    // nothing to close
  }

  public T getValue() {
    return value;
  }

  public static <T> JPAResult<T> ofJPA(T value) {
    return new JPAResult<>(value);
  }

}

