package owmii.powah.block.solar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import owmii.lib.block.AbstractEnergyProvider;
import owmii.lib.block.IInventoryHolder;
import owmii.lib.logistics.energy.Energy;
import owmii.lib.util.Misc;
import owmii.powah.block.Tier;
import owmii.powah.block.Tiles;
import owmii.powah.config.generator.SolarConfig;
import owmii.powah.item.Itms;

import javax.annotation.Nullable;

public class SolarTile extends AbstractEnergyProvider<Tier, SolarConfig, SolarBlock> implements IInventoryHolder {
    public static final String CAN_SEE_SKY = "can_see_sky";
    public static final String HAS_LENS_OF_ENDER = "has_lens_of_ender";
    private boolean canSeeSky;
    private boolean hasLensOfEnder;

    public SolarTile(Tier variant) {
        super(Tiles.SOLAR_PANEL, variant);
        this.inv.add(1);
    }

    public SolarTile() {
        this(Tier.STARTER);
    }

    @Override
    public void readSync(CompoundNBT compound) {
        super.readSync(compound);
        this.canSeeSky = compound.getBoolean(CAN_SEE_SKY);
        this.hasLensOfEnder = compound.getBoolean(HAS_LENS_OF_ENDER);
    }

    @Override
    public CompoundNBT writeSync(CompoundNBT compound) {
        compound.putBoolean(CAN_SEE_SKY, this.canSeeSky);
        compound.putBoolean(HAS_LENS_OF_ENDER, this.hasLensOfEnder);
        return super.writeSync(compound);
    }

    @Override
    protected int postTick(World world) {
        if (isRemote()) return -1;
        boolean flag = chargeItems(1) + extractFromSides(world) > 0;
        if (checkRedstone()) {
            if (!this.hasLensOfEnder && this.ticks % 40L == 0L) {
                boolean canSeeSkyNow = Misc.canBlockSeeSky(world, this.pos.up());
                if (this.canSeeSky != canSeeSkyNow) {
                    this.canSeeSky = canSeeSkyNow;
                    sync();
                }
            }
            if (!this.energy.isFull()) {
                if ((this.canSeeSky || this.hasLensOfEnder) && world.isDaytime()) {
                    this.energy.produce(getGeneration());
                    flag = true;
                }
            }
        }
        return flag ? 5 : -1;
    }

    @Override
    public void onRemoved(World world, BlockState state, BlockState newState, boolean isMoving) {
        super.onRemoved(world, state, newState, isMoving);
        if (state.getBlock() != newState.getBlock()) {
            if (this.hasLensOfEnder) {
                Block.spawnAsEntity(world, this.pos, new ItemStack(Itms.LENS_OF_ENDER));
            }
        }
    }

    public boolean canSeeSky() {
        return this.canSeeSky;
    }

    @Override
    public boolean keepEnergy() {
        return true;
    }

    @Override
    public boolean isEnergyPresent(@Nullable Direction side) {
        return Direction.DOWN.equals(side);
    }

    public boolean hasLensOfEnder() {
        return this.hasLensOfEnder;
    }

    public void setHasLensOfEnder(boolean hasLensOfEnder) {
        this.hasLensOfEnder = hasLensOfEnder;
        sync();
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack) {
        return Energy.chargeable(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack) {
        return true;
    }
}
