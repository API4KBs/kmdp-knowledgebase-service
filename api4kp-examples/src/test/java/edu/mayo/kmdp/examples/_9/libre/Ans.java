package edu.mayo.kmdp.examples._9.libre;

import com.oath.cyclops.hkt.Higher;
import com.oath.cyclops.types.functor.Transformable;
import edu.mayo.kmdp.examples._9.libre.DataWitness.ans;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.omg.spec.api4kp._20200801.Answer;


public class Ans<T> implements Higher<ans, T>, Transformable<T> {

  private final T value;

  public Ans(T val) {
    this.value = val;
  }

  public static <T> Ans<T> of(T val) {
    return new Ans<>(val);
  }

  public static <T> Higher<ans, T> from(Answer<T> a) {
    return Ans.of(a.get());
  }

  public static AnsMonad monad() {
    return new AnsMonad();
  }

  @Override
  public <R> Ans<R> map(Function<? super T, ? extends R> fn) {
    return new Ans<>(fn.apply(value));
  }

  public <R> Higher<ans, R> flatMap(Function<? super T, ? extends Higher<ans, R>> fn){
    return fn.apply(value);
  }


  public <T2, R> Ans<R> zip(final Ans<T2> app, final BiFunction<? super T, ? super T2, ? extends R> fn){
    var j = fn.apply(this.value, app.value);
    return new Ans<>(j);
  }
}
