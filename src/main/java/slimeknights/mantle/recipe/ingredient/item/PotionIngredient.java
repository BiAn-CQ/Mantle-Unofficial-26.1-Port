package slimeknights.mantle.recipe.ingredient.item;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;

/** Canonical package for potion ingredients, retaining the original package entry point. */
public class PotionIngredient extends slimeknights.mantle.recipe.ingredient.PotionIngredient {
  public PotionIngredient(HolderSet<Potion> potion, HolderSet<Item> items) { super(potion, items); }
}
