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
		if(!(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		Fire fire = plugin.fires.get(player.getName());
		if(fire != null){
			plugin.fires.remove(player);
			fire.cancel();
		}
	}

}
