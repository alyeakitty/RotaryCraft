/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.API.Event;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import Reika.DragonAPI.Instantiable.Event.TileEntityEvent;

public class WorktableCraftEvent extends TileEntityEvent {

	public final boolean automated;
	public final String playerName;
	private final ItemStack item;

	public WorktableCraftEvent(TileEntity te, String ep, boolean wasAuto, ItemStack is) {
		super(te);
		automated = wasAuto;
		playerName = ep;
		item = is;
	}

	public final ItemStack getItem() {
		return item != null ? item.copy() : null;
	}

}
