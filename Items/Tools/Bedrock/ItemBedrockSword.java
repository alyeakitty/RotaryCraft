/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.RotaryCraft.Items.Tools.Bedrock;

import ic2.api.item.IElectricItem;

import java.util.List;

import net.machinemuse.api.electricity.MuseElectricItem;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import universalelectricity.api.item.IEnergyItem;
import Reika.DragonAPI.Interfaces.IndexedItemSprites;
import Reika.DragonAPI.Libraries.ReikaEnchantmentHelper;
import Reika.DragonAPI.Libraries.ReikaEntityHelper;
import Reika.DragonAPI.Libraries.IO.ReikaChatHelper;
import Reika.DragonAPI.Libraries.IO.ReikaTextureHelper;
import Reika.DragonAPI.ModRegistry.InterfaceCache;
import Reika.RotaryCraft.RotaryCraft;
import Reika.RotaryCraft.Registry.ConfigRegistry;
import Reika.RotaryCraft.Registry.ItemRegistry;
import cofh.api.energy.IEnergyContainerItem;

import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemBedrockSword extends ItemSword implements IndexedItemSprites {

	private int index;

	public ItemBedrockSword(int tex) {
		super(ToolMaterial.EMERALD);
		this.setIndex(tex);
		maxStackSize = 1;
		this.setMaxDamage(0);
		this.setNoRepair();
		this.setCreativeTab(RotaryCraft.instance.isLocked() ? null : RotaryCraft.tabRotaryTools);

		field_150934_a = 12;
	}

	private void setIndex(int tex) {
		index = tex;
	}

	@Override
	public int getItemSpriteIndex(ItemStack is) {
		return index;
	}

	@Override
	public String getTexture(ItemStack is) {
		return "/Reika/RotaryCraft/Textures/Items/items2.png";
	}

	@Override
	public Class getTextureReferenceClass() {
		return RotaryCraft.class;
	}

	@Override
	public float func_150931_i()
	{
		return 1;
	}

	@Override
	public boolean hitEntity(ItemStack is, EntityLivingBase target, EntityLivingBase player)
	{
		for (int i = 1; i < 5; i++) {
			ItemStack arm = target.getEquipmentInSlot(i);
			if (arm != null && this.canDamageArmorOf(target)) {
				if (InterfaceCache.MUSEELECTRICITEM.instanceOf(arm.getItem())) {
					MuseElectricItem ms = (MuseElectricItem)arm.getItem();
					ms.extractEnergy(arm, 5000, false);
				}
				else if (InterfaceCache.RFENERGYITEM.instanceOf(arm.getItem())) {
					IEnergyContainerItem ie = (IEnergyContainerItem)arm.getItem();
					ie.extractEnergy(arm, 5000, false);
				}
				else if (InterfaceCache.IELECTRICITEM.instanceOf(arm.getItem())) {
					IElectricItem ie = (IElectricItem)arm.getItem();
					///???
					Item id = ie.getEmptyItem(arm);
					ItemStack newarm = new ItemStack(id, 1, 0);
					target.setCurrentItemOrArmor(i, newarm);
				}
				else if (InterfaceCache.UEENERGYITEM.instanceOf(arm.getItem())) {
					IEnergyItem ie = (IEnergyItem)arm.getItem();
					ie.discharge(arm, 5000, true);
				}
				else if (arm.getItem() instanceof ItemBedrockArmor) {
					//do nothing
				}
				else {
					arm.damageItem(100, target);
					if (arm.getItemDamage() > arm.getMaxDamage() || arm.stackSize <= 0) {
						arm = null;
						target.setCurrentItemOrArmor(i, null);
					}
					target.playSound("random.break", 0.1F, 0.8F);
				}
			}
		}
		if (target.isDead || target.getHealth() <= 0) {
			if (itemRand.nextInt(5) == 0) {
				ReikaEntityHelper.dropHead(target);
			}
		}
		return true;
	}

	private boolean canDamageArmorOf(EntityLivingBase target) {
		MinecraftServer ms = MinecraftServer.getServer();
		return target instanceof EntityPlayer ? ms != null && ms.isPVPEnabled() : true;
	}

	@Override
	public boolean onBlockDestroyed(ItemStack par1ItemStack, World par2World, Block par3, int par4, int par5, int par6, EntityLivingBase par7EntityLivingBase)
	{
		return true;
	}

	/**
	 * Return the enchantability factor of the item, most of the time is based on material.
	 */
	@Override
	public int getItemEnchantability()
	{
		return ConfigRegistry.PREENCHANT.getState() ? 0 : ToolMaterial.IRON.getEnchantability();
	}

	/**
	 * Return the name for this tool's material.
	 */
	@Override
	public String getToolMaterialName()
	{
		return "Bedrock";
	}

	/**
	 * Return whether this item is repairable in an anvil.
	 */
	@Override
	public boolean getIsRepairable(ItemStack par1ItemStack, ItemStack par2ItemStack)
	{
		return false;
	}

	/**
	 * Gets a map of item attribute modifiers, used by ItemSword to increase hit damage.
	 */
	@Override
	public Multimap getItemAttributeModifiers()
	{
		Multimap multimap = super.getItemAttributeModifiers();
		boolean flag = multimap.remove(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), 9);
		multimap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(field_111210_e, "Weapon modifier", this.func_150931_i(), 0));
		return multimap;
	}

	// To make un-unenchantable
	@Override
	public void onUpdate(ItemStack is, World world, Entity entity, int par4, boolean par5) {
		this.forceEnchants(is, world, entity, par4);
	}

	private void forceEnchants(ItemStack is, World world, Entity entity, int slot) {
		if (!ReikaEnchantmentHelper.hasEnchantment(Enchantment.looting, is) || !ReikaEnchantmentHelper.hasEnchantment(Enchantment.sharpness, is)) {
			entity.playSound("random.break", 1, 1);
			if (entity instanceof EntityPlayer) {
				EntityPlayer ep = (EntityPlayer)entity;
				ep.inventory.setInventorySlotContents(slot, null);
				ep.attackEntityFrom(DamageSource.generic, 10);
				ReikaChatHelper.sendChatToPlayer(ep, "The dulled tool has broken.");
				is = null;
			}
		}
	}

	@Override
	public boolean onEntityItemUpdate(EntityItem ei) {
		ItemStack is = ei.getEntityItem();
		if (!ReikaEnchantmentHelper.hasEnchantment(Enchantment.looting, is) || !ReikaEnchantmentHelper.hasEnchantment(Enchantment.sharpness, is)) {
			ei.playSound("random.break", 1, 1);
			ei.setDead();
		}
		return false;
	}

	@Override
	public final IIcon getIconFromDamage(int dmg) { //To get around a bug in backtools
		return RotaryCraft.instance.isLocked() ? ReikaTextureHelper.getMissingIcon() : Items.stone_sword.getIconFromDamage(0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item par1, CreativeTabs par2CreativeTabs, List par3List) //Adds the metadata blocks to the creative inventory
	{
		ItemStack item = new ItemStack(par1, 1, 0);
		item.addEnchantment(Enchantment.sharpness, 5);
		item.addEnchantment(Enchantment.looting, 5);
		par3List.add(item);
	}

	@Override
	public String getItemStackDisplayName(ItemStack is) {
		return ItemRegistry.getEntry(is).getBasicName();
	}

}
