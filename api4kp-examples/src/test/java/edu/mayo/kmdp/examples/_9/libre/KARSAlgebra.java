package edu.mayo.kmdp.examples._9.libre;

import com.oath.cyclops.hkt.DataWitness.io;
import com.oath.cyclops.hkt.Higher;
import cyclops.reactive.IO;
import edu.mayo.kmdp.examples._9.libre.DataWitness.ans;
import java.util.List;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal._getKnowledgeAsset;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal._listKnowledgeAssets;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public interface KARSAlgebra<W> {


  static KARSAlgebra<ans> defaultImpl(_listKnowledgeAssets lister, _getKnowledgeAsset getter) {
    return new KARSAlgebra<>() {
      @Override
      public Higher<ans, List<Pointer>> listKnowledgeAssets(String assetType) {
        // lister, getter as functions ?
        var list = lister.listKnowledgeAssets(assetType, null, null, 0, -1);
        // Answr to ans ?
        return Ans.from(list);
      }

      @Override
      public Higher<ans, KnowledgeAsset> getKnowledgAssetVersion(Pointer ptr) {
        var surr = getter.getKnowledgeAsset(ptr.getUuid(), ptr.getVersionTag());
        return Ans.from(surr);
      }
    };
  }

  static KARSAlgebra<io> ioImpl() {
    return new KARSAlgebra<>() {
      @Override
      public Higher<io, List<Pointer>> listKnowledgeAssets(String assetType) {
        return IO.of(List.of(new Pointer()));
      }

      @Override
      public Higher<io, KnowledgeAsset> getKnowledgAssetVersion(Pointer ptr) {
        return IO.of(new KnowledgeAsset().withName("BBB"));
      }
    };
  }

  Higher<W, List<Pointer>> listKnowledgeAssets(String assetType);

  Higher<W, KnowledgeAsset> getKnowledgAssetVersion(Pointer ptr);


}
