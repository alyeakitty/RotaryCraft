/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.TileEntities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import Reika.DragonAPI.Libraries.ReikaNBTHelper.NBTTypes;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.RotaryCraft.Base.TileEntity.TileEntityPowerReceiver;
import Reika.RotaryCraft.Registry.MachineRegistry;

public class TileEntitySorting extends TileEntityPowerReceiver {

	public static final int LENGTH = 9;

	private ForgeDirection facingDir;

	private ItemStack[] mappings = new ItemStack[LENGTH*3];

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		this.getIOSides(world, x, y, z, meta);
		this.getPower(false);
		if (!world.isRemote) {
			AxisAlignedBB box = this.getBox();
			List<EntityItem> li = world.getEntitiesWithinAABB(EntityItem.class, box);
			this.sortItems(world, x, y, z, li);
		}
		//ReikaJavaLibrary.pConsole(this.getSide()+": "+Arrays.deepToString(mappings));
	}

	public void getIOSides(World world, int x, int y, int z, int metadata) {
		switch(metadata) {
		case 0:
			facingDir = ForgeDirection.EAST;
			break;
		case 1:
			facingDir = ForgeDirection.WEST;
			break;
		case 2:
			facingDir = ForgeDirection.NORTH;
			break;
		case 3:
			facingDir = ForgeDirection.SOUTH;
			break;
		}
		read = facingDir;
	}

	private void sortItems(World world, int x, int y, int z, List<EntityItem> li) {
		for (EntityItem ei : li) {
			ItemStack is = ei.getEntityItem();
			if (is.stackSize <= 1)
				ei.setDead();
			else {
				is.stackSize--;
			}
			ForgeDirection dir = this.getSideForItem(is);
			double dx = x+0.5+dir.offsetX*0.75;
			double dy = y+0.5+dir.offsetY*0.75;
			double dz = z+0.5+dir.offsetZ*0.75;
			EntityItem e = new EntityItem(world, dx, dy, dz, ReikaItemHelper.getSizedItemStack(is, 1));
			double v = 0.1;
			e.motionX = dir.offsetX*v;
			e.motionY = dir.offsetY*v;
			e.motionZ = dir.offsetZ*v;
			world.spawnEntityInWorld(e);
		}
	}

	private ForgeDirection getSideForItem(ItemStack is) {
		for (int k = 0; k < mappings.length; k++) {
			ItemStack map = mappings[k];
			if (map != null) {
				Item item = is.getItem();
				Item item2 = map.getItem();
				if (item.getHasSubtypes() || item2.getHasSubtypes()) {
					if (ReikaItemHelper.matchStacks(map, is))
						return this.getDirection(k);
				}
				else {
					if (is.getItem() == map.getItem())
						return this.getDirection(k);
				}
			}
		}
		return ForgeDirection.DOWN;
	}

	private ForgeDirection getDirection(int index) {
		index /= LENGTH;
		List<ForgeDirection> li = new ArrayList();
		for (int i = 2; i < 6; i++)
			li.add(dirs[i]);
		li.remove(facingDir);
		return li.get(index);
	}

	private ForgeDirection getFacingDir(int meta) {
		switch(meta) {
		case 0:
			return ForgeDirection.EAST;
		case 1:
			return ForgeDirection.WEST;
		case 2:
			return ForgeDirection.NORTH;
		case 3:
			return ForgeDirection.SOUTH;
		}
		return ForgeDirection.DOWN;
	}

	private AxisAlignedBB getBox() {
		return AxisAlignedBB.getBoundingBox(xCoord, yCoord+1, zCoord, xCoord+1, yCoord+1.25, zCoord+1);
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	@Override
	public MachineRegistry getMachine() {
		return MachineRegistry.SORTING;
	}

	@Override
	public boolean hasModelTransparency() {
		return false;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	public boolean setMapping(int index, ItemStack item) {
		if (item == null) {
			mappings[index] = null;
			return true;
		}
		else {
			if (this.isValidForSlot(index, item)) {
				Item i = item.getItem();
				if (i.getHasSubtypes())
					mappings[index] = new ItemStack(item.getItem(), 1, item.getItemDamage());
				else
					mappings[index] = new ItemStack(item.getItem(), 1, 0);
				return true;
			}
			else
				return false;
		}
	}

	public ItemStack getMapping(int index) {
		return mappings[index];
	}

	private boolean isValidForSlot(int index, ItemStack item) {
		for (int i = 0; i < mappings.length; i++) {
			ItemStack is = mappings[i];
			//ReikaJavaLibrary.pConsole(is);
			if (is != null) {
				if (ReikaItemHelper.matchStacks(item, is))
					return false;
			}
		}
		return true;
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT)
	{
		super.readSyncTag(NBT);

		NBTTagList nbttaglist = NBT.getTagList("Items", NBTTypes.COMPOUND.ID);
		mappings = new ItemStack[LENGTH*3];

		for (int i = 0; i < nbttaglist.tagCount(); i++)
		{
			NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
			byte byte0 = nbttagcompound.getByte("Slot");

			if (byte0 >= 0 && byte0 < mappings.length)
			{
				mappings[byte0] = ItemStack.loadItemStackFromNBT(nbttagcompound);
			}
		}
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT)
	{
		super.writeSyncTag(NBT);

		NBTTagList nbttaglist = new NBTTagList();

		for (int i = 0; i < mappings.length; i++)
		{
			if (mappings[i] != null)
			{
				NBTTagCompound nbttagcompound = new NBTTagCompound();
				nbttagcompound.setByte("Slot", (byte)i);
				mappings[i].writeToNBT(nbttagcompound);
				nbttaglist.appendTag(nbttagcompound);
			}
		}

		NBT.setTag("Items", nbttaglist);

	}

	public static byte getSlot(int col, int side) {
		return (byte)(side*3+col);
	}

	public static int[] getParams(int slot) {
		int l = LENGTH;
		int[] p = new int[2];
		p[0] = slot%l;
		p[1] = slot/l;
		return p;
	}

	@Override
	public int getTextureStateForSide(int s) {
		switch(this.getBlockMetadata()) {
		case 0:
			switch(s) {
			case 2:
				return 1;
			case 3:
				return 2;
			case 4:
				return 3;
			case 5:
				return 0;
			}
			break;

		case 1:
			switch(s) {
			case 2:
				return 1;
			case 3:
				return 2;
			case 4:
				return 0;
			case 5:
				return 3;
			}
			break;

		case 2:
			switch(s) {
			case 2:
				return 0;
			case 3:
				return 1;
			case 4:
				return 2;
			case 5:
				return 3;
			}
			break;

		case 3:
			switch(s) {
			case 2:
				return 1;
			case 3:
				return 0;
			case 4:
				return 2;
			case 5:
				return 3;
			}
			break;
		}
		return 0;
	}

}
