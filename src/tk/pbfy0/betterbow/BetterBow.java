package tk.pbfy0.betterbow;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.server.v1_7_R1.EntityArrow;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftArrow;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.Inventory;
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
	Random random;
	Set<String> nofire;

	@Override
	public void onEnable(){
		self = this;
		nofire = new HashSet<String>();
		random = new Random();
		getLogger().info("BetterBow enabled");
		getServer().getPluginManager().registerEvents(new BetterBowListener(this), this);
		ProtocolLibrary.getProtocolManager().addPacketListener(
				new PacketAdapter(this, PacketType.Play.Client.BLOCK_PLACE){
					@Override
					public void onPacketReceiving(PacketEvent event){
						PacketContainer packet = event.getPacket();
						WrapperPlayClientBlockPlace wrapper = new WrapperPlayClientBlockPlace(packet);
						Player player = event.getPlayer();
						if(wrapper.getX() == -1 && wrapper.getY() == -1 && wrapper.getZ() == -1){
							ItemStack item = wrapper.getHeldItem();
							int effLevel = item.getEnchantmentLevel(Enchantment.DIG_SPEED);
							if(item.getType() == Material.BOW && effLevel >= 1){
								if(nofire.contains(player.getName())){
									return;
								}
								Fire fire = new Fire(self, player, item);
								fire.runTaskLater(self, 11-effLevel);
								nofire.add(player.getName());
							}
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
	private int slot;
	private ItemStack bow;
	public Fire(BetterBow plugin_, Player player_, ItemStack bow_){
		plugin = plugin_;
		player = player_;
		bow = bow_;
		slot = player.getInventory().getHeldItemSlot();
	}

	@Override
	public void run() {
		plugin.nofire.remove(player.getName());
		if(bow.getType() != Material.BOW) return;
		if(!(player.getInventory().contains(Material.ARROW)) && player.getGameMode() != GameMode.CREATIVE) return;
		player.getInventory().setItem(slot, null);
		Arrow arrow = player.launchProjectile(Arrow.class);
		arrow.setVelocity(player.getLocation().getDirection().multiply(3 + bow.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE)/3)); // seems about right
		EntityArrow entityArrow = ((CraftArrow)arrow).getHandle();
		if(bow.containsEnchantment(Enchantment.ARROW_DAMAGE)){
			entityArrow.b(2.5 + bow.getEnchantmentLevel(Enchantment.ARROW_DAMAGE)/2); // b is setDamage
		}
		if(bow.containsEnchantment(Enchantment.ARROW_FIRE)){
			arrow.setFireTicks(2000);
		}
		if(bow.containsEnchantment(Enchantment.ARROW_KNOCKBACK)){
			entityArrow.a(bow.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK));
		}
		EntityShootBetterBowEvent ev = new EntityShootBetterBowEvent(player, bow, arrow, 1.0F);
		plugin.getServer().getPluginManager().callEvent(ev);
		if(ev.isCancelled()){
			arrow.remove();
			return;
		}
		player.getWorld().playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 1.0f);
		if(bow.containsEnchantment(Enchantment.ARROW_INFINITE) || player.getGameMode() == GameMode.CREATIVE){
			entityArrow.fromPlayer = 2;
		}else{
			player.getInventory().removeItem(new ItemStack(Material.ARROW, 1));
		}
		if(!(player.getGameMode() == GameMode.CREATIVE || bow.containsEnchantment(Enchantment.DURABILITY) &&
				plugin.random.nextInt(bow.getEnchantmentLevel(Enchantment.DURABILITY)) != 0)){
			bow.setDurability((short) (bow.getDurability() - 1));
		}
		
		new BukkitRunnable(){
			@Override
			public void run() {
				Inventory inventory = player.getInventory();
				ItemStack item = inventory.getItem(slot);
				if(item != null && !item.equals(bow)) player.getWorld().dropItem(player.getLocation(), item);
				player.getInventory().setItem(slot, bow);
			}
		}.runTaskLater(plugin, 6-bow.getEnchantmentLevel(Enchantment.DIG_SPEED)/2);
	}
	
}

class EntityShootBetterBowEvent extends EntityShootBowEvent{
	public EntityShootBetterBowEvent(LivingEntity shooter, ItemStack bow,
			Projectile projectile, float force) {
		super(shooter, bow, projectile, force);
	}
}