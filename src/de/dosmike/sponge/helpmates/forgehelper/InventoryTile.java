package de.dosmike.sponge.helpmates.forgehelper;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperation;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.item.inventory.adapter.impl.AbstractInventoryAdapter;
import org.spongepowered.common.item.inventory.lens.impl.fabric.IInventoryFabric;

import net.minecraft.inventory.IInventory;

public class InventoryTile {
	TileEntity te;
	Inventory inv;
	
	public InventoryTile(TileEntity tileEntity) {
		te = tileEntity;
		if (tileEntity instanceof Carrier) {
			inv = ((Carrier)tileEntity).getInventory();
		} else {
			IInventoryFabric fabric  = new IInventoryFabric((IInventory) tileEntity);
	//		DefaultIndexedLens<IInventory> lens = new DefaultIndexedLens<>(0, fabric.getSize(), SlotC, slots)
			inv = new AbstractInventoryAdapter<IInventory>(fabric);
		}
	}
	
	public Inventory getInventory() {
		return inv;
	}
	
	public TileEntity getTileEntity() {
		return te;
	}
	
	public Location<World> getLocationInWorld() {
		return te.getLocation();
	}
	
	/** quick access for lazy devs
	 * @return getInventory().query(operations); */
	public <T extends Inventory> T query(QueryOperation<?>... operations) {
		return inv.query(operations);
	}
	
	/** quick access for lazy devs
	 * @return getInvetory().offer(stack); */
	public InventoryTransactionResult offer(ItemStack stack) {
		return inv.offer(stack);
	}
}
