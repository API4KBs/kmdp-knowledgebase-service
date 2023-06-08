package edu.mayo.kmdp.examples._9.libre;

import static cyclops.function.Function2._2;
import static cyclops.function.Function3.__23;

import cyclops.data.tuple.Tuple;
import cyclops.instances.reactive.IOInstances;
import cyclops.typeclasses.Do;
import cyclops.typeclasses.monad.Monad;
import edu.mayo.kmdp.examples.MockAssetRepository;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;

/**
 * https://medium.com/modernnerd-code/dsls-with-the-free-monad-in-java-8-part-i-701408e874f8
 * https://medium.com/@johnmcclean/dsls-with-the-free-monad-in-java-8-part-ii-f0010f012ae1
 *
 * https://medium.com/@johnmcclean/simulating-higher-kinded-types-in-java-b52a18b72c74
 */
public class Program<W,A,T> {

  Monad<W> monad;
  KARSAlgebra<W> client;


  public Program(
      Monad<W> monad,
      KARSAlgebra<W> client) {
    this.monad = monad;
    this.client = client;
  }

  public static void main(String[] args) {
    MockAssetRepository kars = new MockAssetRepository();
    kars.clearKnowledgeAssetCatalog();
    kars.publish(new KnowledgeAsset()
        .withAssetId(SemanticIdentifier.randomId())
            .withFormalType(KnowledgeAssetTypeSeries.Lexicon)
        .withName("AAAA"), null);

    new Program<>(
        Ans.monad(),
        KARSAlgebra.defaultImpl(kars::listKnowledgeAssets, kars::getKnowledgeAssetVersion))
        .run();

    new Program<>(
        IOInstances.monad(),
        KARSAlgebra.ioImpl())
        .run();

  }

  private void run() {

     var ans = Do.forEach(monad)
        ._of("Lexicon")
        .__(client::listKnowledgeAssets)
        .map(l -> l.get(0))
        .__(_2(client::getKnowledgAssetVersion))
        .map(KnowledgeAsset::getName)
        .yield(__23(Tuple::tuple))
        .unwrap();

  }

}
