package net.benjaminurquhart.treenuke;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class Nuker implements Listener {
	
	private static final Set<Material> AXES = EnumSet.of(
			Material.DIAMOND_AXE, 
			Material.GOLDEN_AXE, 
			Material.IRON_AXE, 
			Material.STONE_AXE, 
			Material.WOODEN_AXE
	);
	
	private boolean isLog(Block block) {
		return block != null && block.getType().toString().endsWith("_LOG");
	}
	
	private List<ItemStack> nuke(Block base, ItemStack tool) {
		
		int limit = -1;
		int broken = 0;
		
		Damageable durability = null;
		
		if(tool != null) {
			ItemMeta meta = tool.getItemMeta();
			
			if(meta != null && !meta.isUnbreakable() && (meta instanceof Damageable)) {
				durability = (Damageable) meta;
				
				// We respect your tools
				if(tool.getType().getMaxDurability()-durability.getDamage() < 2) {
					return Collections.emptyList();
				}
				
				limit = Math.min(1000, tool.getType().getMaxDurability()-durability.getDamage());
			}
			else {
				limit = 1000;
			}
		}
		Block block, neighbor;
		
		List<ItemStack> drops = new ArrayList<>();
		
		Deque<Block> now = new ArrayDeque<>(), next = new ArrayDeque<>(), swap;
		
		now.add(base);
		
		loop:
		while(!now.isEmpty()) {
			//System.out.println(now);
			while(!now.isEmpty()) {
				
				block = now.poll();
				
				if(!this.isLog(block)) continue;
				
				drops.addAll(block.getDrops(tool));
				
				if(++broken >= limit && limit >= 0) {
					break loop;
				}
				
				block.setType(Material.AIR);
				
				// Avert your eyes!
				for(int i = -1; i <= 1; i++) {
					for(int j = -1; j <= 1; j++) {
						for(int k = -1; k <= 1; k++) {
							neighbor = block.getRelative(i,j,k);
							if(this.isLog(neighbor)) {
								next.offer(neighbor);
							}
						}
					}
				}
			}
			swap = now;
			now = next;
			next = swap;
		}
		if(durability != null) {
			durability.setDamage(durability.getDamage()+Math.min(limit, broken));
			tool.setItemMeta((ItemMeta) durability);
		}
		return drops;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		
		boolean creative = player.getGameMode() == GameMode.CREATIVE;
		
		ItemStack tool = player.getInventory().getItemInMainHand();
		
		if(player.isSneaking() && (tool.getType().equals(Material.AIR) || AXES.contains(tool.getType())) && this.isLog(block)) {
			
			// The block will be broken in the nuker
			event.setCancelled(true);
			Map<Integer, ItemStack> overflow = player.getInventory().addItem(nuke(block, creative ? null : tool).toArray(ItemStack[]::new));
			
			if(!overflow.isEmpty() && !creative) {
				World world = block.getWorld();
				
				overflow.values().forEach(stack -> world.dropItemNaturally(player.getLocation(), stack));
			}
		}
	}
}
