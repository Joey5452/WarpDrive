package cr0s.warpdrive.block.hull;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IHullBlock;
import cr0s.warpdrive.config.WarpDriveConfig;

public class BlockHullPlain extends Block implements IHullBlock {
	@SideOnly(Side.CLIENT)
	private IIcon[] icons;
	private int tier;
	
	public BlockHullPlain(final int tier) {
		super(Material.rock);
		this.tier = tier;
		setHardness(WarpDriveConfig.HULL_HARDNESS[tier - 1]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[tier - 1] * 5 / 3);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setBlockName("warpdrive.hull" + tier + ".plain.");
		setBlockTextureName("warpdrive:hull/plain");
	}
	
	@Override
	public int getMobilityFlag() {
		return 2;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int metadata) {
		return icons[metadata % 16];
	}
	
	@Override
	public int damageDropped(int p_149692_1_) {
		return p_149692_1_;
	}
	
	public static String getDyeColorName(int metadata) {
		return ItemDye.field_150921_b[~metadata & 15];
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs creativeTab, List list) {
		for (int i = 0; i < 16; ++i) {
			list.add(new ItemStack(item, 1, i));
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iconRegister) {
		icons = new IIcon[16];
		
		for (int i = 0; i < 16; ++i) {
			icons[i] = iconRegister.registerIcon(getTextureName() + "_" + getDyeColorName(i));
		}
	}
	
	@Override
	public MapColor getMapColor(int metadata) {
		return MapColor.getMapColorForBlockColored(metadata);
	}
	
	@Override
	public void downgrade(World world, int x, int y, int z) {
		int metadata = world.getBlockMetadata(x, y, z);
		if (tier == 1) {
			world.setBlockToAir(x, y, z);
		} else {
			world.setBlock(x, y, z, WarpDrive.blockHulls_plain[tier - 2], metadata, 2);
		}
	}
}
