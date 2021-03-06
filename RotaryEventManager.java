/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.world.BlockEvent;
import Reika.DragonAPI.Instantiable.Event.SlotEvent.RemoveFromSlotEvent;
import Reika.DragonAPI.Libraries.ReikaEntityHelper;
import Reika.DragonAPI.Libraries.Java.ReikaObfuscationHelper;
import Reika.DragonAPI.Libraries.Java.ReikaReflectionHelper;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.RotaryCraft.Auxiliary.GrinderDamage;
import Reika.RotaryCraft.Auxiliary.HarvesterDamage;
import Reika.RotaryCraft.Items.Tools.Bedrock.ItemBedrockArmor;
import Reika.RotaryCraft.Items.Tools.Charged.ItemSpringBoots;
import Reika.RotaryCraft.Registry.BlockRegistry;
import Reika.RotaryCraft.Registry.ConfigRegistry;
import Reika.RotaryCraft.Registry.ItemRegistry;
import Reika.RotaryCraft.Registry.MachineRegistry;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RotaryEventManager {

	public static final RotaryEventManager instance = new RotaryEventManager();

	private RotaryEventManager() {

	}
	/*
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void renderItemInSlot(RenderItemInSlotEvent evt) {
		if (evt.getGuiClass() == GuiAutoCrafter.class) {
			if (evt.slotIndex < 18) {
				ItemStack is = evt.getItem();
				if (is != null) {
					ItemStack out = ItemCraftPattern.getRecipeOutput(is);
					ReikaGuiAPI.instance.drawItemStack(new RenderItem(), out, evt.slotX, evt.slotY);
				}
			}
		}
	}*/

	@SubscribeEvent
	public void bonemealEvent(BonemealEvent event)
	{
		if (!event.world.isRemote)  {
			if (event.block == BlockRegistry.CANOLA.getBlockInstance()) {
				World world = event.world;
				int x = event.x;
				int y = event.y;
				int z = event.z;
				event.setResult(Event.Result.DENY);
			}
		}
	}

	@SubscribeEvent
	public void onRemoveArmor(RemoveFromSlotEvent evt) {
		int id = evt.slotID;
		if (evt.slotID == 36) { //foot armor
			ItemStack is = evt.getItem();
			if (is != null && is.getItem() instanceof ItemSpringBoots) {
				evt.player.stepHeight = 0.5F;
			}
		}
	}

	@SubscribeEvent
	public void fallEvent(LivingFallEvent event)
	{
		EntityLivingBase e = event.entityLiving;
		ItemStack is = e.getEquipmentInSlot(1);

		if (is != null) {
			if (is.getItem() instanceof ItemSpringBoots) {
				if (is.getItem() == ItemRegistry.BEDJUMP.getItemInstance() || is.getItemDamage() > 0) {
					//ReikaJavaLibrary.pConsole(event.distance);
					event.distance *= 0.6F;
					//ReikaJavaLibrary.pConsole(event.distance);
					if (is.getItem() == ItemRegistry.BEDJUMP.getItemInstance())
						event.distance = Math.min(event.distance, 25);
				}
			}
		}
	}

	@SubscribeEvent
	public void bedrockSave(LivingHurtEvent evt) {
		EntityLivingBase e = evt.entityLiving;
		if (evt.ammount < 1000) {
			if (e instanceof EntityPlayer) {
				if (ItemBedrockArmor.isWearingFullSuitOf(e)) {
					evt.ammount = Math.min(evt.ammount, 5);
					if (evt.ammount <= 1) {
						evt.ammount = 0;
						return;
					}
					else {
						Entity attacker = evt.source.getSourceOfDamage();
						if (attacker instanceof EntityPlayer) {
							ItemStack held = ((EntityPlayer)attacker).getCurrentEquippedItem();
							if (held != null && "rapier".equals(held.getItem().getClass().getSimpleName().toLowerCase())) {
								evt.ammount = 0;
								int dmg = held.getItem().getDamage(held);
								held.getItem().setDamage(held, dmg+120);
							}
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void enforceHarvesterLooting(LivingDropsEvent ev) {
		if (ev.source instanceof HarvesterDamage) {
			HarvesterDamage dmg = (HarvesterDamage)ev.source;
			int looting = dmg.getLootingLevel();
			EntityLivingBase e = ev.entityLiving;
			ArrayList<EntityItem> li = ev.drops;
			li.clear();
			e.captureDrops = true;
			try {
				ReikaObfuscationHelper.getMethod("dropFewItems").invoke(e, true, looting);
				ReikaObfuscationHelper.getMethod("dropEquipment").invoke(e, true, dmg.hasInfinity() ? 100 : looting*4);
				int rem = RotaryCraft.rand.nextInt(200) - looting*4;
				if (rem <= 5 || dmg.hasInfinity())
					ReikaObfuscationHelper.getMethod("dropRareDrop").invoke(e, 1);
			}
			catch (Exception ex) {
				RotaryCraft.logger.debug("Could not process harvester drops event!");
				if (RotaryCraft.logger.shouldDebug())
					ex.printStackTrace();
			}
			e.captureDrops = false;
		}
	}

	@SubscribeEvent
	public void meatGrinding(LivingDropsEvent ev) {
		if (ev.source instanceof GrinderDamage) {
			ItemStack food = ReikaEntityHelper.getFoodItem(ev.entityLiving);
			ev.drops.clear();
			if (food != null) {
				World world = ev.entityLiving.worldObj;
				Random rand = RotaryCraft.rand;
				int num = 4+rand.nextInt(4)+rand.nextInt(4)+rand.nextInt(4);
				ItemStack is = ReikaItemHelper.getSizedItemStack(food, num);
				ReikaItemHelper.dropItem(world, ev.entityLiving.posX, ev.entityLiving.posY, ev.entityLiving.posZ, is);
			}
			ev.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void disallowDespawn(AllowDespawn ad) {
		EntityLivingBase e = ad.entityLiving;
		PotionEffect pe = e.getActivePotionEffect(RotaryCraft.freeze);
		if (pe == null)
			return;
		ad.setResult(Result.DENY);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public void preventDisallowOfCrucialBlocks(BlockEvent ev) {
		Class c = ev.getClass();
		boolean place = c.getSimpleName().contains("BlockPlaceEvent");
		World world = ev.world;
		int x = ev.x;
		int y = ev.y;
		int z = ev.z;
		if (ev.block == null)
			return;
		Block id = ev.block;
		int meta = ev.blockMetadata;
		MachineRegistry m = MachineRegistry.getMachineFromIDandMetadata(id, meta);
		if (place) { //Bukkit Block Place Event
			if (m != null) {
				if (ConfigRegistry.ALLOWBAN.getState()) {
					if (m.isCrucial()) {
						if (m.canBeDisabledInOverworld() && ReikaWorldHelper.otherDimensionsExist() && world.provider.dimensionId == 0) {
							if (ev.isCanceled())
								RotaryCraft.logger.log("Something successfully cancelled the placement of "+m+". This ban applies to the overworld only!");
						}
						else {
							if (ev.isCanceled())
								RotaryCraft.logger.log("Something tried to cancel the placement of "+m+". This machine is essential and its placement may not be disallowed.");
							ev.setCanceled(false);
						}
					}
					else {
						if (ev.isCanceled())
							RotaryCraft.logger.log("Something successfully cancelled the placement of "+m+". Unless this machine really needs to be disabled, it is recommended you remove this placement ban.");
					}
				}
				else {
					if (ev.isCanceled())
						RotaryCraft.logger.log("Something tried to cancel the placement of "+m+". This is permissible, but you must change the configs to allow it.");
					ev.setCanceled(false);
				}
			}
		}

		if (ConfigRegistry.LOGBLOCKS.getState()) {
			if (m != null) {
				EntityPlayer ep = (EntityPlayer)ReikaWorldHelper.getClosestLivingEntityOfClass(EntityPlayer.class, world, x+0.5, y+0.5, z+0.5, 6);
				String s = place ? "placed" : "removed";
				String name = ep != null ? ep.getCommandSenderName() : "<No Player>";
				RotaryCraft.logger.log("A "+m.getName()+" was "+s+" by "+name+" at "+x+", "+y+", "+z+" in world dimension "+world.provider.dimensionId);
			}
		}

	}

	private void hardCancel(Event e, boolean cancel, boolean print) {
		if (!e.isCancelable())
			throw new IllegalArgumentException("Event "+e.getClass().getSimpleName()+" cannot be cancelled!");
		try {
			Field f = ReikaReflectionHelper.getProtectedInheritedField(e, "cancel");
			f.setAccessible(true);
			f.set(e, cancel);
		}
		catch (Exception ex) {
			if (print)
				ex.printStackTrace();
		}
		try {
			Field f = Event.class.getDeclaredField("isCanceled");
			f.setAccessible(true);
			f.set(e, cancel);
		}
		catch (Exception ex) {
			if (print)
				ex.printStackTrace();
		}
	}
}
