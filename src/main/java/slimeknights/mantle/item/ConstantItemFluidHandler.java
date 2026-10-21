package slimeknights.mantle.item;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
/** Native transactional constant-fluid handler for an item access. */
public class ConstantItemFluidHandler extends ConstantFluidContainerWrapper {
  public ConstantItemFluidHandler(FluidStack fluid, ItemAccess access) { super(fluid, access); }
}
