package tk.pbfy0.betterbow;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class BetterBowListener implements Listener {
	BetterBow plugin;

	public BetterBowListener(BetterBow plugin_) {
		plugin = plugin_;
	}
	
	@EventHandler
	public void onBowFire(EntityShootBowEvent event){
		if(!(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		if(plugin.nofire.contains(player.getName())){
			event.setCancelled(true);
			plugin.nofire.remove(player.getName());
			return;
		}
		ItemStack bow = event.getBow();
		if(!(event instanceof EntityShootBetterBowEvent) && bow.getEnchantmentLevel(Enchantment.LURE) > 0 && player.hasPermission("betterbow.multishot")){
			event.setCancelled(true);
			new Fire(plugin, player, player.getInventory().getHeldItemSlot(), bow, false, event.getForce()).run();
			return;
		}
		if(!bow.containsEnchantment(Enchantment.SILK_TOUCH) && !bow.containsEnchantment(Enchantment.PROTECTION_PROJECTILE))
			return;
		
		Arrow arrow = (Arrow)event.getProjectile();
		Vector velocity = player.getLocation().getDirection();
		velocity.multiply(event.getForce());
		if (bow.getEnchantmentLevel(Enchantment.SILK_TOUCH) < 5) {
			double spreadConstant = 0.00745D;
			if(player.hasPermission("betterbow.antispread")) spreadConstant *= 
					1 - (bow.getEnchantmentLevel(Enchantment.SILK_TOUCH) / 5D);
			Vector spread = new Vector(
					(plugin.random.nextBoolean() ? 1 : -1)
							* (plugin.random.nextDouble() * spreadConstant),
					(plugin.random.nextBoolean() ? 1 : -1)
							* (plugin.random.nextDouble() * spreadConstant),
					(plugin.random.nextBoolean() ? 1 : -1)
							* (plugin.random.nextDouble() * spreadConstant));
			velocity.add(spread);

		}
		
		velocity.multiply(3 + (player.hasPermission("betterbow.sniper") ? bow.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) / 3 : 0));
		
		arrow.setVelocity(velocity);
	}
	@EventHandler
	public void onSlotChange(PlayerItemHeldEvent event){
		String name = event.getPlayer().getName();
		if(plugin.nofire.contains(name)) plugin.nofire.remove(name);
	}

}
