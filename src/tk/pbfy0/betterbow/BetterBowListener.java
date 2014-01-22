package tk.pbfy0.betterbow;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

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
		}
	}

}
