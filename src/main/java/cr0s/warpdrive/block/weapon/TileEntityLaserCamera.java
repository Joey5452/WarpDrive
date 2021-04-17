package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumCameraType;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundNBT;

public class TileEntityLaserCamera extends TileEntityLaser implements IVideoChannel {
	
	private int videoChannel = -1;
	
	private static final int REGISTRY_UPDATE_INTERVAL_TICKS = 15 * 20;
	private static final int PACKET_SEND_INTERVAL_TICKS = 60 * 20;
	
	private int packetSendTicks = 10;
	private int registryUpdateTicks = 20;
	
	public TileEntityLaserCamera(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveLaserCamera";
		addMethods(new String[] {
			"videoChannel"
		});
		// done by ancestor: doRequireUpgradeToInterface();
	}
	
	@Override
	public void tick() {
		super.tick();
		
		// Update video channel on clients (recovery mechanism, no need to go too fast)
		assert world != null;
		if (!world.isRemote()) {
			packetSendTicks--;
			if (packetSendTicks <= 0) {
				packetSendTicks = PACKET_SEND_INTERVAL_TICKS;
				PacketHandler.sendVideoChannelPacket(world, pos, videoChannel);
			}
		} else {
			registryUpdateTicks--;
			if (registryUpdateTicks <= 0) {
				registryUpdateTicks = REGISTRY_UPDATE_INTERVAL_TICKS;
				if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
					WarpDrive.logger.info(String.format("%s Updating registry (%d)",
					                                    this, videoChannel ));
				}
				WarpDrive.cameras.updateInRegistry(world, pos, videoChannel, EnumCameraType.LASER_CAMERA);
			}
		}
	}
	
	@Override
	public int getVideoChannel() {
		return videoChannel;
	}
	
	@Override
	public void setVideoChannel(final int parVideoChannel) {
		if ( videoChannel != parVideoChannel
		  && IVideoChannel.isValid(parVideoChannel) ) {
			final int videoChannelOld = videoChannel;
			videoChannel = parVideoChannel;
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("%s Video channel updated from %d to %d",
				                                    this, videoChannelOld, parVideoChannel ));
			}
			markDirty();
			// force update through main thread since CC & OC are running outside the main thread
			packetSendTicks = 0;
			registryUpdateTicks = 0;
		}
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		setVideoChannel(tagCompound.getInt("cameraFrequency") + tagCompound.getInt(VIDEO_CHANNEL_TAG));
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		tagCompound.putInt(VIDEO_CHANNEL_TAG, videoChannel);
		return tagCompound;
	}
	
	@Override
	public void remove() {
		WarpDrive.cameras.removeFromRegistry(world, pos);
		super.remove();
	}
	
	@Override
	public void onChunkUnloaded() {
		WarpDrive.cameras.removeFromRegistry(world, pos);
		super.onChunkUnloaded();
	}
	
	// Common OC/CC methods
	public Object[] videoChannel(@Nonnull final Object[] arguments) {
		if (arguments.length == 1) {
			setVideoChannel(Commons.toInt(arguments[0]));
		}
		return new Integer[] { getVideoChannel() };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] videoChannel(final Context context, final Arguments arguments) {
		return videoChannel(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "videoChannel":
			return videoChannel(arguments);
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s Beam '%d' Camera '%d' %s",
		                     getClass().getSimpleName(),
		                     beamFrequency,
		                     videoChannel,
		                     Commons.format(world, pos));
	}
}