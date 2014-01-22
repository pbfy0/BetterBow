package tk.pbfy0.betterbow;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_7_R1.EntityArrow;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftArrow;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.packetwrapper.WrapperPlayClientBlockPlace;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

public final class BetterBow extends JavaPlugin {
	private BetterBow self;
	Map<String, Fire> fires;

	@Override
	public void onEnable(){
		self = this;
		fires = new HashMap<String, Fire>();
		getLogger().info("BetterBow enabled");
		getServer().getPluginManager().registerEvents(new BetterBowListener(this), this);
		ProtocolLibrary.getProtocolManager().addPacketListener(
				new PacketAdapter(this, PacketType.Play.Client.BLOCK_PLACE){
					@Override
					public void onPacketReceiving(PacketEvent event){
						PacketContainer packet = event.getPacket();
						WrapperPlayClientBlockPlace wrapper = new WrapperPlayClientBlockPlace(packet);
						Player player = event.getPlayer();
						ItemStack item = wrapper.getHeldItem();
						int effLevel = item.getEnchantmentLevel(Enchantment.DIG_SPEED);
						if(item.getType() == Material.BOW && effLevel >= 1 && 
								wrapper.getX() == -1 && wrapper.getY() == -1 && wrapper.getZ() == -1){
							Fire fire = new Fire(self, player);
							fire.runTaskLater(self, 11-effLevel);
							fires.put(player.getName(), fire);
						}
					}
				}
				);
	}
	
	@Override
	public void onDisable(){
		
	}
}
class Fire extends BukkitRunnable {
	BetterBow plugin;
	Player player;
	public Fire(BetterBow plugin_, Player player_){
		plugin = plugin_;
		player = player_;
	}

	@Override
	public void run() {
		final ItemStack bow = player.getItemInHand();
		plugin.fires.remove(player);
		if(bow.getType() != Material.BOW) return;
		player.setItemInHand(null);
		Arrow arrow = player.launchProjectile(Arrow.class);
		arrow.setVelocity(player.getLocation().getDirection().multiply(4));
		arrow.setShooter(player);
		EntityArrow entityArrow = ((CraftArrow)arrow).getHandle();
		plugin.getLogger().info("" +entityArrow.e());
		if(bow.containsEnchantment(Enchantment.ARROW_DAMAGE)){
			entityArrow.b(bow.getEnchantmentLevel(Enchantment.ARROW_DAMAGE));
		}
		new BukkitRunnable(){
			@Override
			public void run() {
				player.setItemInHand(bow);
			}
		}.runTaskLater(plugin, 1);
	}
	
}