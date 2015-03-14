package tk.pbfy0.betterbow;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.bukkit.util.Vector;

import net.minecraft.server.v1_8_R1.EntityArrow;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftArrow;
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
import org.mcstats.Metrics;

import com.comphenix.packetwrapper.WrapperPlayClientBlockPlace;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;

public final class BetterBow extends JavaPlugin {
	private BetterBow self;
	Random random;
	Set<String> nofire;

	@Override
	public void onEnable() {
		self = this;
		nofire = new HashSet<String>();
		random = new Random();
		getLogger().info("BetterBow enabled");
		try{
			Metrics metrics = new Metrics(this);
			metrics.start();
		}catch(IOException e){
			getLogger().info("Metrics failed to start");
		}
		getServer().getPluginManager().registerEvents(
				new BetterBowListener(this), this);
		ProtocolLibrary.getProtocolManager().addPacketListener(
				new PacketAdapter(this, PacketType.Play.Client.BLOCK_PLACE) {
					@Override
					public void onPacketReceiving(PacketEvent event) {
						PacketContainer packet = event.getPacket();
						WrapperPlayClientBlockPlace wrapper = new WrapperPlayClientBlockPlace(
								packet);
						BlockPosition pos = wrapper.getLocation();
						if (pos.getX() == -1 && pos.getY() == -1
								&& pos.getZ() == -1) {
							Player player = event.getPlayer();
							if(!player.hasPermission("betterbow.fire")) return;
							ItemStack item = wrapper.getHeldItem();
							int effLevel = item
									.getEnchantmentLevel(Enchantment.DIG_SPEED);
							if (item.getType() == Material.BOW && effLevel >= 1) {
								if (!player.hasPermission("betterbow.fire") || nofire.contains(player.getName())) return;
								Fire fire = new Fire(self, player, player.getInventory().getHeldItemSlot());
								fire.runTaskLater(self, Math.max(11 - effLevel, 0));
								nofire.add(player.getName());
							}
						}
					}
				});
	}

	@Override
	public void onDisable() {

	}
}

class Fire extends BukkitRunnable {
	BetterBow plugin;
	Player player;
	private int slot;
	private ItemStack bow;
	private boolean autofire = false;

	public Fire(BetterBow plugin_, Player player_, int slot_) {
		plugin = plugin_;
		player = player_;
		slot = slot_;
		bow = player.getInventory().getItem(slot);
		autofire = bow.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS) && player.hasPermission("betterbow.autofire");
	}

	class RestoreBow extends BukkitRunnable {
		@Override
		public void run() {
			Inventory inventory = player.getInventory();
			ItemStack item = inventory.getItem(slot);
			if (item != null && !item.equals(bow))
				player.getWorld().dropItem(player.getLocation(), item);
			player.getInventory().setItem(slot, bow);
		}
	}

	@Override
	public void run() {
		if (bow.getType() != Material.BOW)
			return;
		if (!(player.getInventory().contains(Material.ARROW))
				&& player.getGameMode() != GameMode.CREATIVE)
			return;
		if (autofire) {
			player.getInventory().setItem(slot, null);
			plugin.nofire.remove(player.getName());
		}
		int arrows = bow.getEnchantmentLevel(Enchantment.LURE) + 1;
		arrows = player.hasPermission("betterbow.multi") ? (autofire ? Math.min(arrows, 10) : Math.min(arrows, 100)) : 1;
		for (int i = 0; i < arrows; i++) {
			Arrow arrow = player.launchProjectile(Arrow.class);
			Vector velocity = player.getLocation().getDirection();
			if (!player.hasPermission("betterbow.antispread") || bow.getEnchantmentLevel(Enchantment.SILK_TOUCH) < 5) {
				double spreadConstant = 0.00745D * (5D - bow
						.getEnchantmentLevel(Enchantment.SILK_TOUCH)) / 5D;
				Vector spread = new Vector(
						(plugin.random.nextBoolean() ? 1 : -1)
								* (plugin.random.nextDouble() * spreadConstant),
						(plugin.random.nextBoolean() ? 1 : -1)
								* (plugin.random.nextDouble() * spreadConstant),
						(plugin.random.nextBoolean() ? 1 : -1)
								* (plugin.random.nextDouble() * spreadConstant));
				velocity.add(spread);

			}
			if(player.hasPermission("betterbow.sniper"))
				velocity.multiply(3 + bow.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) / 3);
 
			arrow.setVelocity(velocity); // seems about right
			EntityArrow entityArrow = ((CraftArrow) arrow).getHandle();
			if (bow.containsEnchantment(Enchantment.ARROW_DAMAGE)) {
				entityArrow.b(2.5 + bow
						.getEnchantmentLevel(Enchantment.ARROW_DAMAGE) / 2); 
				// b is setDamage
			}
			if (bow.containsEnchantment(Enchantment.ARROW_FIRE)) {
				arrow.setFireTicks(2000);
			}
			if (bow.containsEnchantment(Enchantment.ARROW_KNOCKBACK)) {
				//entityArrow.
				entityArrow.setKnockbackStrength(bow
						.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK));
			}
			EntityShootBetterBowEvent ev = new EntityShootBetterBowEvent(
					player, bow, arrow, 1.0F);
			plugin.getServer().getPluginManager().callEvent(ev);
			if (ev.isCancelled()) {
				arrow.remove();
				if (autofire)
					new RestoreBow().runTaskLater(plugin, 1);
				continue;
			}
			player.getWorld().playSound(player.getLocation(),
					Sound.SHOOT_ARROW, 1.0f, 1.0f);
			if (bow.containsEnchantment(Enchantment.ARROW_INFINITE)
					|| player.getGameMode() == GameMode.CREATIVE) {
				entityArrow.fromPlayer = 2;
			} else {
				player.getInventory().removeItem(
						new ItemStack(Material.ARROW, 1));
			}
			if (!(player.getGameMode() == GameMode.CREATIVE || bow
					.containsEnchantment(Enchantment.DURABILITY)
					&& plugin.random.nextInt(bow
							.getEnchantmentLevel(Enchantment.DURABILITY)) != 0)) {
				bow.setDurability((short) (bow.getDurability() + 1));
				if (bow.getDurability() >= bow.getType().getMaxDurability()) {
					if(!autofire)
						player.getInventory().setItem(slot, null);
					autofire = false; // Prevents the bow from being restored
					player.getWorld().playSound(player.getLocation(),
							Sound.ITEM_BREAK, 1.0F, 1.0F);
					break;
				}
			}
		}

		if (autofire) new RestoreBow().runTaskLater(plugin, 1);
	}
}

class EntityShootBetterBowEvent extends EntityShootBowEvent {
	public EntityShootBetterBowEvent(LivingEntity shooter, ItemStack bow,
			Projectile projectile, float force) {
		super(shooter, bow, projectile, force);
	}
}