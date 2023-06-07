package edu.mayo.kmdp.ops;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedTransform;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

/**
 * Implementations of this interface are expected to generate new Assets (Artifacts + Surrogates)
 * from a different source Asset, which may be Atomic, a Composite, or the root of a Composite.
 * <p>
 * The new Asset is ephemeral: it is created on demand, at the time of the request, and fabricators
 * are not expected to persist it (though internal caching may be allowed for performance)
 * <p>
 * Contrast with Negotiators, which generate new Artifacts for a given Asset, from a preexisting
 * variant expression of that Asset
 * <p>
 * Fabricators are clients of Knowledge Asset/Artifact Repositories, from which they acquire the
 * source Knowledge Resources. Internally, they implement some form of API4KP 'Transform' operation
 */
public interface EphemeralAssetFabricator extends _applyNamedTransform {

  /**
   * @return The ID of this fabricator, as an operator
   */
  UUID getId();

  /**
   * Given a source Asset, fabricates a new Asset, in the form of its canonical representation
   *
   * @param ephemeralAssetId    the ID of the ephemeral Asset
   * @param ephemeralVersionTag the version of the ephemeral Asset
   * @return the canonical representation of the derivative, fabricated asset
   */
  Answer<KnowledgeCarrier> fabricate(
      @Nonnull final UUID ephemeralAssetId,
      @Nonnull final String ephemeralVersionTag);

  /**
   * Given a source Asset, fabricates a new Asset, in the form of its canonical surrogate
   *
   * @param ephemeralAssetId    the ID of the ephemeral Asset
   * @param ephemeralVersionTag the version of the ephemeral Asset
   * @return the canonical surrogate of the derivative, fabricated Asset
   */
  Answer<KnowledgeCarrier> fabricateSurrogate(
      @Nonnull final UUID ephemeralAssetId,
      @Nonnull final String ephemeralVersionTag);

  /**
   * Given a source Asset series ID, determines which version can be used for fabrication, if any.
   * If more than one version is usable, the latest and greatest should be preferred
   *
   * @param ephemeralAssetId the ephemeral Asset series ID
   * @return the best version of the source Asset that can be used for fabrication
   */
  Answer<String> getFabricatableVersion(
      @Nonnull final UUID ephemeralAssetId);

  /**
   * Given the Pointer of a candidate source Asset, predicts the identity and form of the Asset that
   * could be generated from it. This operation is not expected to actually fabricate the new
   * Asset.
   *
   * @param source the Pointer of the source Asset
   * @return a prospective Pointer to the Asset that could be generated from the source, if any
   */
  Optional<Pointer> pledge(
      @Nonnull final Pointer source);

  /**
   * Combines the Pointer to a source Asset with the Pointer(s) of any Asset that this Fabricator
   * can build from that source Asset, if any.
   * <p>
   * If the fabricator cannot use the source Asset, it must return the source Pointer alone
   *
   * @param source the Pointer of the source Asset
   * @return the union of the source Pointer and the Pointer(s) of the Assets that could be
   * fabricated from that source
   * @see #pledge(Pointer)
   */
  default Stream<Pointer> join(
      @Nonnull final Pointer source) {
    return pledge(source)
        .map(ptr -> Stream.of(source, ptr))
        .orElseGet(() -> Stream.of(source));
  }

  /**
   * Predicate.
   * <p>
   * Uses the minimal metadata of a source Asset, to determine whether it can be used to fabricate a
   * new ephemeral Asset
   *
   * @param source the minimal information about the source seed Asset
   * @return true if the referenced source Asset can be used by this fabricator
   */
  boolean canFabricate(
      @Nonnull final Pointer source);

  boolean isFabricator(
      @Nonnull final UUID ephemeralAssetId);



  class UUIDEncrypter {

    private UUIDEncrypter() {
      // functions only
    }

    public static UUID encrypt(UUID original, UUID key) {
      var secretKey = convertUUIDToBytes(key);
      var src = convertUUIDToBytes(original);
      var encrypted = xor(src, secretKey);
      return convertBytesToUUID(encrypted);
    }

    public static UUID decrypt(UUID encrypted, UUID key) {
      var secretKey = convertUUIDToBytes(key);
      var enc = convertUUIDToBytes(encrypted);
      var decrypted = xor(enc, secretKey);
      return convertBytesToUUID(decrypted);
    }

    public static byte[] convertUUIDToBytes(UUID uuid) {
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(uuid.getMostSignificantBits());
      bb.putLong(uuid.getLeastSignificantBits());
      return bb.array();
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      long high = byteBuffer.getLong();
      long low = byteBuffer.getLong();
      return new UUID(high, low);
    }

    public static byte[] xor(byte[] ba1, byte[] ba2) {
      byte[] result = new byte[ba1.length];
      for (int i = 0; i < ba1.length; i++) {
        result[i] = (byte) (ba1[i] ^ ba2[i]);
      }
      return result;
    }

  }

}
