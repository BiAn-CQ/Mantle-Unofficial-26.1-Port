package slimeknights.mantle.data.client;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import slimeknights.mantle.client.model.FluidContainerModel;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.registration.object.FluidObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Quick and dirty data provider to generate fluid bucket models */
public class FluidBucketModelProvider extends GenericDataProvider {
  private final String modId;
  private final List<BucketModel> buckets = new ArrayList<>();
  public FluidBucketModelProvider(PackOutput packOutput, String modId) {
    super(packOutput, Target.RESOURCE_PACK, "items");
    this.modId = modId;
  }

  /** Record for bucket model data */
  private record BucketModel(Identifier fluid, Identifier bucket, boolean flipGas) {
    /** Creates the JSON for this model */
    public JsonObject serialize() {
      JsonObject json = new JsonObject();
      JsonObject model = new slimeknights.mantle.client.model.builder.FluidContainerModelBuilder(Identifier.parse("neoforge:item/bucket_drip"))
        .fluid(fluid).flipGas(flipGas).toJson();
      json.add("model", model);
      return json;
    }
  }

  /** Adds a bucket to the list */
  protected void model(Identifier fluid, Identifier bucket, boolean flipGas) {
    buckets.add(new BucketModel(fluid, bucket, flipGas));
  }

  /** Adds a bucket to the list. */
  protected void model(Identifier fluid, boolean flipGas) {
    model(fluid, fluid.withSuffix("_bucket"), flipGas);
  }

  /** Adds a bucket to the list from the fluid object, without requiring it to be bound. */
  protected void optionalFluid(FluidObject<?> fluid, boolean flipGas) {
    model(fluid.id(), flipGas);
  }

  /** Adds a bucket to the list from the fluid object, requires it to be bound. */
  protected void requiredFluid(FluidObject<?> fluid) {
    optionalFluid(fluid, fluid.getType().isLighterThanAir());
  }

  /** Adds a bucket to the list from a bucket item. */
  protected void bucket(Identifier id, BucketItem bucket) {
    model(Loadables.FLUID.getKey(bucket.content), id, bucket.content.getFluidType().isLighterThanAir());
  }

  /** Adds a bucket to the list from a bucket item. */
  protected void bucket(BucketItem bucket) {
    bucket(Loadables.ITEM.getKey(bucket), bucket);
  }

  /** Adds buckets, may be overridden to avoid the registry scan. */
  protected void addBuckets() {
    for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
      Identifier id = entry.getKey().identifier();
      if (id.getNamespace().equals(modId) && entry.getValue() instanceof BucketItem bucket) {
        bucket(id, bucket);
      }
    }
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    addBuckets();
    return allOf(buckets.stream().map(model -> saveJson(cache, model.bucket, model.serialize())));
  }

  @Override
  public String getName() {
    return modId + " Fluid Bucket Model Provider";
  }
}
