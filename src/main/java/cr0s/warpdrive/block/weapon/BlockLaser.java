package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

public class BlockLaser extends BlockAbstractContainer {
	
	public BlockLaser(@Nonnull final String registryName, @Nonnull final EnumTier enumTier) {
		super(getDefaultProperties(null)
				.hardnessAndResistance(50.0F, 20.0F),
		      registryName, enumTier);
	}
}