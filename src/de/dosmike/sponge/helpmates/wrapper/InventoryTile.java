package de.dosmike.sponge.helpmates.wrapper;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.impl.DefaultIndexedLens;
import org.spongepowered.common.item.inventory.lens.impl.fabric.IInventoryFabric;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class InventoryTile<TTE extends TileEntity> {
	TTE te;
	Inventory inv;
	
	public InventoryTile(TTE tileEntity, Carrier carrier) {
		te = tileEntity;
		inv = carrier.getInventory();
	}
	
	public InventoryTile(TTE tileEntity) {
		IInventoryFabric fabric  = new IInventoryFabric((IInventory) tileEntity);
//		DefaultIndexedLens<IInventory> lens = new DefaultIndexedLens<>(0, fabric.getSize(), SlotC, slots)
		
	}
}
