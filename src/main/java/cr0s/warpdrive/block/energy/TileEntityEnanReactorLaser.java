package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.IEnanReactorLaser;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.ReactorFace;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.ref.WeakReference;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

import net.minecraftforge.fml.common.Optional;

public class TileEntityEnanReactorLaser extends TileEntityAbstractLaser implements IEnanReactorLaser {
	
	// persistent properties
	private ReactorFace reactorFace = ReactorFace.UNKNOWN;
	private int energyStabilizationRequest = 0;
	
	// computed properties
	private Vector3 vLaser;
	private String reactorSignatureName;
	private Vector3 vReactorCore;
	private WeakReference<TileEntityEnanReactorCore> weakReactorCore;
	
	public TileEntityEnanReactorLaser() {
		super();
		
		peripheralName = "warpdriveEnanReactorLaser";
		addMethods(new String[] {
				"side",
				"stabilize"
		});
		laserMedium_maxCount = 1;
		laserMedium_directionsValid = new EnumFacing[] { EnumFacing.UP, EnumFacing.DOWN };
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		if (reactorFace == ReactorFace.UNKNOWN) {
			// laser isn't linked yet, let's try to update nearby reactors
			onBlockUpdateDetected();
		}
		
		vLaser = new Vector3(this).translate(0.5);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (energyStabilizationRequest > 0) {
			doStabilize(energyStabilizationRequest);
			energyStabilizationRequest = 0;
		}
	}
	
	@Nonnull 
	public ReactorFace getReactorFace() {
		return reactorFace != null ? reactorFace : ReactorFace.UNKNOWN;
	}
	
	public void setReactorFace(@Nonnull final ReactorFace reactorFace, final TileEntityEnanReactorCore reactorCore) {
		// skip if it's already set to another valid reactor core
		if ( this.reactorFace != reactorFace
		  && this.reactorFace != ReactorFace.UNKNOWN ) {
			if (getReactorCore() != null) {
				return;
			}
		}
		
		// always update cached signature name
		reactorSignatureName = reactorCore != null ? reactorCore.getSignatureName() : "";
		
		// refresh blockstate
		final IBlockState blockState_old = world.getBlockState(pos);
		final IBlockState blockState_new;
		if (reactorFace.facingLaserProperty != null) {
			blockState_new = blockState_old.withProperty(BlockProperties.ACTIVE, true)
			                               .withProperty(BlockProperties.FACING, reactorFace.facingLaserProperty);
		} else {
			blockState_new = blockState_old.withProperty(BlockProperties.ACTIVE, false)
			                               .withProperty(BlockProperties.FACING, EnumFacing.DOWN);
		}
		updateBlockState(blockState_old, blockState_new);
		
		// skip if it's already set to save resources
		if (this.reactorFace == reactorFace) {
			return;
		}
		
		// update properties
		this.reactorFace = reactorFace;
		this.weakReactorCore = reactorCore != null && reactorFace != ReactorFace.UNKNOWN ? new WeakReference<>(reactorCore) : null;
		
		// cache reactor coordinates
		if (reactorCore != null) {
			vReactorCore = reactorCore.getCenter();
		}
	}
	
	@Nullable
	private TileEntityEnanReactorCore getReactorCore() {
		if (reactorFace == ReactorFace.UNKNOWN) {
			return null;
		}
		TileEntityEnanReactorCore reactorCore = weakReactorCore != null ? weakReactorCore.get() : null;
		if (reactorCore == null) {
			final BlockPos blockPos = pos.add(- reactorFace.x, - reactorFace.y, - reactorFace.z);
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityEnanReactorCore) {
				reactorCore = (TileEntityEnanReactorCore) tileEntity;
				weakReactorCore = new WeakReference<>(reactorCore);
				vReactorCore = reactorCore.getCenter();
			} else {
				WarpDrive.logger.error(String.format("%s Invalid TileEntityEnanReactorCore %s: %s",
				                                     this,
				                                     Commons.format(world, pos),
				                                     tileEntity));
				reactorFace = ReactorFace.UNKNOWN;
			}
		}
		return reactorCore;
	}
	
	@Override
	public void onBlockUpdateDetected() {
		super.onBlockUpdateDetected();
		
		final TileEntityEnanReactorCore reactorCore = getReactorCore();
		if (reactorCore != null) {
			reactorCore.onBlockUpdateDetected();
		} else {
			final MutableBlockPos mutableBlockPos = new MutableBlockPos(pos);
			for (final ReactorFace reactorFace : ReactorFace.getLasers()) {
				if (reactorFace.indexStability < 0) {
					continue;
				}
				
				mutableBlockPos.setPos(pos.getX() - reactorFace.x,
				                       pos.getY() - reactorFace.y,
				                       pos.getZ() - reactorFace.z);
				if (world.isBlockLoaded(mutableBlockPos, true)) {
					final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
					if (tileEntity instanceof TileEntityEnanReactorCore) {
						((TileEntityEnanReactorCore) tileEntity).onBlockUpdateDetected();
					}
				}
			}
		}
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		if (reactorFace == ReactorFace.UNKNOWN) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.enan_reactor.status_line.missing_reactor_core");
			return false;
		}
		
		return isValid;
	}
	
	protected int stabilize(final int energy) {
		if (energy <= 0) {
			return 0;
		}
		
		if (laserMedium_direction == null) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side doesn't have a laser medium, unable to stabilize %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
			return 0;
		}
		
		if (reactorFace == ReactorFace.UNKNOWN) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side doesn't have a core to stabilize %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
			return 0;
		}
		
		if (energyStabilizationRequest > 0) {
			WarpDrive.logger.debug(String.format("%s Stabilization already requested for %s",
			                                     this, energy ));
			return -energy;
		}
		energyStabilizationRequest = energy;
		return energy;
	}
	
	private void doStabilize(final int energy) {
		if (energy <= 0) {
			WarpDrive.logger.error(String.format("ReactorLaser %s on %s side can't stabilize without energy, please report to mod author %d",
			                                     Commons.format(world, pos), reactorFace.name, energy ));
			return;
		}
		
		if (laserMedium_direction == null) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side no longer has a laser medium, unable to stabilize %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
			return;
		}
		
		final TileEntityEnanReactorCore reactorCore = getReactorCore();
		if (reactorCore == null) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side no longer has a core to stabilize %d",
			                                     Commons.format(world, pos), reactorFace.name, energy ));
			return;
		}
		
		if (!laserMedium_consumeExactly(energy, false)) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side doesn't have enough energy %d/%d while core can output %d/%d",
			                                    Commons.format(world, pos),
			                                    reactorFace.name,
			                                    laserMedium_getEnergyStored(true),
			                                    energy,
			                                    reactorCore.energy_getPotentialOutput(),
			                                    reactorCore.energy_getEnergyStored() ));
			return;
		}
		
		if (WarpDriveConfig.LOGGING_ENERGY && WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("ReactorLaser %s on %s side stabilizing %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
		}
		reactorCore.decreaseInstability(reactorFace, energy);
		PacketHandler.sendBeamPacket(world, vLaser, vReactorCore, 0.1F, 0.2F, 1.0F, 25, 50, 100);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		if (reactorFace != null && reactorFace != ReactorFace.UNKNOWN) {
			tagCompound.setString("reactorFace", reactorFace.getName());
		}
		tagCompound.setInteger("energyStabilizationRequest", energyStabilizationRequest);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		reactorFace = ReactorFace.get(tagCompound.getString("reactorFace"));
		if (reactorFace == null) {
			reactorFace = ReactorFace.UNKNOWN;
		}
		energyStabilizationRequest = tagCompound.getInteger("energyStabilizationRequest");
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("reactorFace");
		tagCompound.removeTag("energyStabilizationRequest");
		return tagCompound;
	}
	
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] { true,
		                      EnergyWrapper.convert(energyStabilizationRequest, units) };
	}
	
	@Override
	public Object[] stabilize(final Object[] arguments) {
		if (arguments.length == 1) {
			final int energy;
			try {
				energy = Commons.toInt(arguments[0]);
				return new Object[] { stabilize(energy) };
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on stabilize(): Integer expected for 1st argument %s",
					                                     this, arguments[0]));
				}
			}
		}
		return new Object[] { energyStabilizationRequest };
	}
	
	@Override
	public Object[] side() {
		if (reactorFace == null || reactorFace.enumTier == null) {
			return new Object[] { null, null, null };
		}
		return new Object[] { reactorFace.indexStability, reactorFace.enumTier.getName(), reactorSignatureName };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] stabilize(final Context context, final Arguments arguments) {
		return stabilize(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] side(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return side();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "stabilize":
			return stabilize(arguments);
			
		case "side":
			return side();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}