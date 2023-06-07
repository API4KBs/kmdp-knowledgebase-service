package edu.mayo.kmdp.examples._9.libre;

import com.oath.cyclops.hkt.Higher;
import cyclops.typeclasses.monad.Monad;
import java.util.function.Function;
import edu.mayo.kmdp.examples._9.libre.DataWitness.ans;

public class AnsMonad implements Monad<ans> {


  public static <T> Ans<T> narrowK(Higher<ans, T> hk) {
    return (Ans<T>) hk;
  }


  @Override
  public <T, R> Higher<ans, R> flatMap(
      Function<? super T, ? extends Higher<ans, R>> fn,
      Higher<ans, T> ds) {
    var a = narrowK(ds);
    return a.flatMap(fn.andThen(AnsMonad::narrowK));
  }

  @Override
  public <T, R> Higher<ans, R> ap(Higher<ans, ? extends Function<T, R>> fn, Higher<ans, T> apply) {
    Ans<T> ans = narrowK(apply);
    Ans<? extends Function<T, R>> ansFn = narrowK(fn);
    return ansFn.zip(ans, Function::apply);
  }

  @Override
  public <T> Higher<ans, T> unit(T value) {
    return new Ans<T>(value);
  }

  @Override
  public <T, R> Higher<ans, R> map(Function<? super T, ? extends R> fn, Higher<ans, T> ds) {
    return narrowK(ds).map(fn);
  }
}
