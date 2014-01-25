package tk.pbfy0.betterbow;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public final class BetterBowListener implements Listener {
	BetterBow plugin;

	public BetterBowListener(BetterBow plugin_) {
		plugin = plugin_;
	}
	
	@EventHandler
	public void onBowFire(EntityShootBowEvent event){
		if(event instanceof EntityShootBetterBowEvent) return;
		if(!(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		if(plugin.nofire.contains(player.getName())){
			event.setCancelled(true);
			plugin.nofire.remove(player.getName());
		}else{
			ItemStack bow = player.getItemInHand();
			if(bow != null && bow.containsEnchantment(Enchantment.PROTECTION_PROJECTILE)){
				Arrow arrow = (Arrow)event.getProjectile();
				arrow.setVelocity(arrow.getVelocity().add(
						player.getLocation().getDirection().multiply(bow.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE)/3)));
			}
		}
	}
	@EventHandler
	public void onSlotChange(PlayerItemHeldEvent event){
		String name = event.getPlayer().getName();
		if(plugin.nofire.contains(name)) plugin.nofire.remove(name);
	}

}
