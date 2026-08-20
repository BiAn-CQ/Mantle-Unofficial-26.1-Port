package slimeknights.mantle.fluid;

import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import java.util.Map;
import java.util.Map.Entry;

/** Fluid where up is down and down is up */
public abstract class InvertedFluid extends BaseFlowingFluid {
  protected InvertedFluid(Properties properties) {
    super(properties);
  }

  @Override
  public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluid) {
    double xHeight = 0.0D;
    double zHeight = 0.0D;
    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

    for (Direction direction : Direction.Plane.HORIZONTAL) {
      mutable.setWithOffset(pos, direction);
      FluidState sideFluid = level.getFluidState(mutable);
      if (sideFluid.isEmpty() || sideFluid.getType().isSame(this)) {
        float sideHeight = sideFluid.getOwnHeight();
        float deltaHeight = 0.0F;
        if (sideHeight == 0.0F) {
          if (!level.getBlockState(mutable).blocksMotion()) {
            BlockPos above = mutable.above();
            FluidState aboveFluid = level.getFluidState(above);
            if (aboveFluid.isEmpty() || aboveFluid.getType().isSame(this)) {
              sideHeight = aboveFluid.getOwnHeight();
              if (sideHeight > 0.0F) {
                deltaHeight = fluid.getOwnHeight() - sideHeight + 0.8888889F;
              }
            }
          }
        } else if (sideHeight > 0.0F) {
          deltaHeight = fluid.getOwnHeight() - sideHeight;
        }

        if (deltaHeight != 0.0F) {
          xHeight += direction.getStepX() * deltaHeight;
          zHeight += direction.getStepZ() * deltaHeight;
        }
      }
    }

    Vec3 vector = new Vec3(xHeight, 0.0D, zHeight);
    if (fluid.getValue(FALLING)) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        mutable.setWithOffset(pos, direction);
        if (this.isSolidFace(level, mutable, direction) || this.isSolidFace(level, mutable.below(), direction)) {
          vector = vector.normalize().add(0.0D, 6.0D, 0.0D);
          break;
        }
      }
    }

    return vector.normalize();
  }

  @Override
  protected boolean isSolidFace(BlockGetter level, BlockPos neighbor, Direction side) {
    BlockState block = level.getBlockState(neighbor);
    FluidState fluid = level.getFluidState(neighbor);
    return !fluid.getType().isSame(this) && (side == Direction.DOWN || !(block.getBlock() instanceof IceBlock) && block.isFaceSturdy(level, neighbor, side));
  }

  @Override
  protected void spread(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState) {
    // recreation that swaps downs for ups
    if (fluidState.isEmpty()) {
      return;
    }

    BlockPos abovePos = pos.above();
    BlockState aboveState = level.getBlockState(abovePos);
    FluidState aboveFluid = aboveState.getFluidState();
    if (aboveFluid.canBeReplacedWith(level, abovePos, fluidState.getType(), Direction.DOWN)
        && canHoldFluid(level, abovePos, aboveState, fluidState.getType())) {
      FluidState newAbove = this.getNewLiquid(level, abovePos, aboveState);
      if (newAbove.getType().isSame(this)) {
        this.spreadTo(level, abovePos, aboveState, Direction.UP, newAbove);
        if (this.sourceNeighborCount(level, pos) >= 3) {
          this.spreadToSides(level, pos, fluidState, state);
        }
        return;
      }
    }

    if (fluidState.isSource() || !this.isWaterHole(level, pos, state, abovePos, aboveState)) {
      this.spreadToSides(level, pos, fluidState, state);
    }
  }

  private void spreadToSides(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState state) {
    int neighbor = fluidState.getAmount() - this.getDropOff(level);
    if (fluidState.getValue(FALLING)) {
      neighbor = 7;
    }

    if (neighbor > 0) {
      for (Entry<Direction, FluidState> entry : this.getSpread(level, pos, state).entrySet()) {
        Direction spread = entry.getKey();
        BlockPos neighborPos = pos.relative(spread);
        this.spreadTo(level, neighborPos, level.getBlockState(neighborPos), spread, entry.getValue());
      }
    }
  }

  @Override
  protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
    int highestNeighbor = 0;
    int neighborSources = 0;

    for (Direction direction : Direction.Plane.HORIZONTAL) {
      BlockPos side = pos.relative(direction);
      BlockState sideBlock = level.getBlockState(side);
      FluidState sideFluid = sideBlock.getFluidState();
      if (sideFluid.getType().isSame(this) && this.canPassThroughWall(direction, level, pos, state, side, sideBlock)) {
        if (sideFluid.isSource() && EventHooks.canCreateFluidSource(level, side, sideBlock)) {
          neighborSources++;
        }
        highestNeighbor = Math.max(highestNeighbor, sideFluid.getAmount());
      }
    }

    if (neighborSources >= 2) {
      BlockState belowState = level.getBlockState(pos.below());
      FluidState belowFluid = belowState.getFluidState();
      if (belowState.isSolid() || this.isSourceBlockOfThisType(belowFluid)) {
        return this.getSource(false);
      }
    }

    BlockPos abovePos = pos.above();
    BlockState aboveState = level.getBlockState(abovePos);
    FluidState aboveFluid = aboveState.getFluidState();
    if (!aboveFluid.isEmpty() && aboveFluid.getType().isSame(this)
        && this.canPassThroughWall(Direction.UP, level, pos, state, abovePos, aboveState)) {
      return this.getFlowing(8, true);
    }

    int newHeight = highestNeighbor - this.getDropOff(level);
    return newHeight <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(newHeight, false);
  }


  @Override
  protected Map<Direction, FluidState> getSpread(ServerLevel level, BlockPos pos, BlockState state) {
    Map<Direction, FluidState> spread = Maps.newEnumMap(Direction.class);
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      BlockPos side = pos.relative(direction);
      BlockState sideBlock = level.getBlockState(side);
      FluidState sideFluid = sideBlock.getFluidState();
      if (this.canPassThrough(level, this.getFlowing(), pos, state, direction, side, sideBlock, sideFluid)) {
        FluidState newFluid = this.getNewLiquid(level, side, sideBlock);
        if (sideFluid.canBeReplacedWith(level, side, newFluid.getType(), direction)) {
          spread.put(direction, newFluid);
        }
      }
    }
    return spread;
  }

  private boolean isWaterHole(BlockGetter level, BlockPos pos, BlockState block, BlockPos spreadPos, BlockState spreadBlock) {
    // recreation swapping downs for ups
    return this.canPassThroughWall(Direction.UP, level, pos, block, spreadPos, spreadBlock)
      && (spreadBlock.getFluidState().getType().isSame(this) || this.canHoldFluid(level, spreadPos, spreadBlock, this.getFlowing()));
  }

  private int sourceNeighborCount(BlockGetter level, BlockPos pos) {
    int count = 0;
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      if (this.isSourceBlockOfThisType(level.getFluidState(pos.relative(direction)))) {
        count++;
      }
    }
    return count;
  }

  private boolean isSourceBlockOfThisType(FluidState state) {
    return state.getType().isSame(this) && state.isSource();
  }

  private boolean canPassThrough(BlockGetter level, Fluid fluid, BlockPos sourcePos, BlockState sourceState,
                                 Direction direction, BlockPos targetPos, BlockState targetState, FluidState targetFluidState) {
    return !this.isSourceBlockOfThisType(targetFluidState)
      && canHoldAnyFluid(targetState)
      && this.canPassThroughWall(direction, level, sourcePos, sourceState, targetPos, targetState)
      && canHoldSpecificFluid(level, targetPos, targetState, fluid);
  }

  private boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos sourcePos, BlockState sourceState,
                                     BlockPos targetPos, BlockState targetState) {
    VoxelShape targetShape = targetState.getCollisionShape(level, targetPos);
    if (targetShape == Shapes.block()) {
      return false;
    }
    VoxelShape sourceShape = sourceState.getCollisionShape(level, sourcePos);
    if (sourceShape == Shapes.block()) {
      return false;
    }
    if (sourceShape == Shapes.empty() && targetShape == Shapes.empty()) {
      return true;
    }
    return !Shapes.mergedFaceOccludes(sourceShape, targetShape, direction);
  }

  private boolean canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
    return canHoldAnyFluid(state) && canHoldSpecificFluid(level, pos, state, fluid);
  }

  private static boolean canHoldAnyFluid(BlockState state) {
    if (state.getBlock() instanceof LiquidBlockContainer) {
      return true;
    }
    return !state.blocksMotion()
      && !(state.getBlock() instanceof DoorBlock)
      && !state.is(BlockTags.SIGNS)
      && !state.is(Blocks.LADDER)
      && !state.is(Blocks.SUGAR_CANE)
      && !state.is(Blocks.BUBBLE_COLUMN)
      && !state.is(Blocks.NETHER_PORTAL)
      && !state.is(Blocks.END_PORTAL)
      && !state.is(Blocks.END_GATEWAY)
      && !state.is(Blocks.STRUCTURE_VOID);
  }

  private static boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid newFluid) {
    return state.getBlock() instanceof LiquidBlockContainer container
      ? container.canPlaceLiquid(null, level, pos, state, newFluid)
      : true;
  }

  public static class Flowing extends InvertedFluid {
    public Flowing(Properties properties) {
      super(properties);
      this.registerDefaultState(this.getStateDefinition().any().setValue(LEVEL, 7));
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
      super.createFluidStateDefinition(builder);
      builder.add(LEVEL);
    }

    @Override
    public int getAmount(FluidState state) {
      return state.getValue(LEVEL);
    }

    @Override
    public boolean isSource(FluidState state) {
      return false;
    }
  }

  public static class Source extends InvertedFluid {
    public Source(Properties properties) {
      super(properties);
    }

    @Override
    public int getAmount(FluidState state) {
      return 8;
    }

    @Override
    public boolean isSource(FluidState state) {
      return true;
    }
  }
}
