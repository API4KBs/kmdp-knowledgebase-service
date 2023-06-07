package edu.mayo.kmdp.examples._0.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;

/**
 * Answer ({@link org.omg.spec.api4kp._20200801.Answer}) is the monad-style wrapper that combines famous
 * monads such as Optional/Maybe, Try (for error handling), Writer (for explanations) and Composite
 */
public class AnswerExample {

  /**
   * Like Optional, Answer supports success and failure
   */
  @Test
  void testAnswerConstruction() {
    Answer<String> ans = Answer.of("test");
    assertTrue(ans.isSuccess());
    assertEquals("test", ans.get());

    Answer<String> fail = Answer.failed();
    assertFalse(fail.isSuccess());
    assertThrows(NoSuchElementException.class, fail::get);
    assertEquals("default", fail.orElse("default"));
  }

  /**
   * Map and flatMap support chaining
   */
  @Test
  void testAnswerChaining() {
    Answer<String> ans = Answer.of("test")
        .map(str -> str.toUpperCase() + "-" + str.length());
    assertEquals("TEST-4", ans.get());

    Answer<Integer> ans2 = Answer.of("test")
        .flatMap(str -> Answer.of(str.length()));
    assertEquals(4, ans2.get());
  }

  /**
   * Mapped functions are evaluated lazily only on success
   */
  @Test
  void testAnswerLaziness() {
    Answer<?> ans = Answer.failed()
        // failure causes the computation to fail early - no matter what happens next
        .map(x -> {
          throw new IllegalStateException();
        })
        .map(x -> {
          for (; ; ) {
            try {
              java.util.concurrent.TimeUnit.SECONDS.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            if (Math.random() > 2.0) {
              return -1;
            }
          }
        });
    assertTrue(ans.isFailure());
  }

}
