package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.forcefield.BlockForceField;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.GlobalRegion;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.OfflineAvatarManager;

import javax.annotation.Nonnull;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerHandler {
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerLoadFromFile(@Nonnull final PlayerEvent.LoadFromFile event) {
		OfflineAvatarManager.onPlayerLoggedIn(event.getPlayer());
	}
	
	@SubscribeEvent
	public void onPlayerLoggedOut(@Nonnull final PlayerLoggedOutEvent event) {
		if (WarpDriveConfig.OFFLINE_AVATAR_ENABLE) {
			OfflineAvatarManager.onPlayerLoggedOut(event.getPlayer());
		}
	}
	
	@SubscribeEvent
	public void onBreakSpeed(@Nonnull final BreakSpeed event) {
		final BlockPos blockPos = event.getPos();
		
		// check for lock
		doCancelEventDuringJump(event, blockPos);
		if (event.isCanceled()) {
			return;
		}
		
		// check for maintenance and member access
		final BlockState blockState = event.getState();
		doCancelEventForNonMembers(event, blockPos, blockState);
	}
	
	@SubscribeEvent
	public void onEntityItemPickup(@Nonnull final EntityItemPickupEvent event) {
		doCancelEventDuringJump(event, event.getItem().getPosition());
	}
	
	@SubscribeEvent
	public void onLeftClickBlock(@Nonnull final LeftClickBlock event) {
		final BlockPos blockPos = event.getPos();
		
		// check for lock
		doCancelEventDuringJump(event, blockPos);
		
		// check for maintenance and member access
		final BlockState blockState = event.getWorld().getBlockState(blockPos);
		doCancelEventForNonMembers(event, blockPos, blockState);
	}
	
	@SubscribeEvent
	public void onRightClickBlock(@Nonnull final RightClickBlock event) {
		final BlockPos blockPos = event.getPos();
		
		// check for lock
		doCancelEventDuringJump(event, blockPos);
		
		// check for maintenance and member access
		final BlockState blockState = event.getWorld().getBlockState(blockPos);
		doCancelEventForNonMembers(event, blockPos, blockState);
	}
	
	private void doCancelEventForNonMembers(@Nonnull final PlayerEvent event,
	                                       @Nonnull final BlockPos blockPos, @Nonnull final BlockState blockState) {
		final PlayerEntity entityPlayer = event.getPlayer();
		final boolean isCrewMember = checkMaintenanceAndCrew(event, entityPlayer, blockPos, blockState);
		
		// restrict mining/access to members only
		if ( !isCrewMember
		  && !entityPlayer.isCreative() ) {
			if (Commons.throttleMe("cancelingEventForNonMember")) {
				WarpDrive.logger.info(String.format("Cancelling event for non-member %s",
				                                     entityPlayer));
			}
			event.setCanceled(true);
		}
	}
	
	public static boolean checkMaintenanceAndCrew(@Nonnull final Event event, @Nonnull final PlayerEntity entityPlayer,
	                                              @Nonnull final BlockPos blockPos, @Nonnull final BlockState blockState) {
		final Block block = blockState.getBlock();
		final float hardness = blockState.getBlockHardness(entityPlayer.getEntityWorld(), blockPos);
		
		// skip force field, anchor and non-reinforced blocks
		if ( block instanceof BlockForceField
		  || hardness < WarpDriveConfig.HULL_HARDNESS[1]
		  || Dictionary.BLOCKS_ANCHOR.contains(block) ) {
			return true;
		}
		// keep blocks inside a ship
		final ArrayList<GlobalRegion> globalRegions = GlobalRegionManager.getContainers(EnumGlobalRegionType.SHIP, entityPlayer.world, blockPos);
		if (globalRegions.isEmpty()) {
			return true;
		}
		
		// sanitize & summarize the ship list
		boolean isUnderMaintenance = true;
		boolean isCrewMember = true;
		for (final GlobalRegion globalRegion : globalRegions) {
			// abort on invalid ship cores
			final TileEntity tileEntity = entityPlayer.world.getTileEntity(globalRegion.getBlockPos());
			if (!(tileEntity instanceof TileEntityShipCore)) {
				if (Commons.throttleMe("onBreakSpeed-InvalidInstance")) {
					WarpDrive.logger.error(String.format("Unable to adjust harvest speed due to invalid tile entity for global region, expecting TileEntityShipCore, got %s",
					                                     tileEntity ));
				}
				return false;
			}
			// skip invalid assemblies
			final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
			if (!tileEntityShipCore.isAssemblyValid()) {
				if (Commons.throttleMe("onBreakSpeed-InvalidAssembly")) {
					WarpDrive.logger.debug(String.format("Skipping maintenance & crew members for invalid ship assembly %s",
					                                     tileEntity ));
				}
				continue;
			}
			isUnderMaintenance &= tileEntityShipCore.isUnderMaintenance();
			isCrewMember &= tileEntityShipCore.isCrewMember(entityPlayer);
		}
		
		// apply maintenance bypass/boost
		if (isUnderMaintenance) {
			// @TODO: return an enum to remove this hack
			if (event instanceof BreakSpeed) {
				((BreakSpeed) event).setNewSpeed(5.0F * hardness);
			}
			return true;
		}
		
		return isCrewMember
		    || entityPlayer.isCreative();
	}
	
	private void doCancelEventDuringJump(@Nonnull final PlayerEvent event, @Nonnull final BlockPos blockPos) {
		assert event.isCancelable();
		if (event.isCanceled()) {
			return;
		}
		
		if (AbstractSequencer.isLocked(blockPos)) {
			event.setCanceled(true);
		}
	}
}
