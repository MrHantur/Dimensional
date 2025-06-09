package su.mrhantur;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.loadUnblockDate();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("setdimensiondate").setExecutor(this);
        Bukkit.getConsoleSender().sendMessage("Dimensional plugin is now working!");
    }

    private final Map<String, LocalDateTime> unblockDate = new HashMap();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public void onDisable() {
        this.saveUnblockDate();
    }

    private void loadUnblockDate() {
        String netherDate = this.getConfig().getString("nether-lock", "");
        String endDate = this.getConfig().getString("end-lock", "");
        if (!netherDate.isEmpty()) {
            this.unblockDate.put("nether", LocalDateTime.parse(netherDate, this.dateFormatter));
        } else {
            this.unblockDate.put("nether", LocalDateTime.now().plusWeeks(1L));
        }

        if (!endDate.isEmpty()) {
            this.unblockDate.put("end", LocalDateTime.parse(endDate, this.dateFormatter));
        } else {
            this.unblockDate.put("end", LocalDateTime.now().plusWeeks(2L));
        }

    }

    private void saveUnblockDate() {
        this.getConfig().set("nether-lock", this.unblockDate.get("nether").format(this.dateFormatter));
        this.getConfig().set("end-lock", this.unblockDate.get("end").format(this.dateFormatter));
        this.saveConfig();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("setdimensiondate")) {
            if (args.length < 2) {
                sender.sendMessage("Подсказка: /setdimensiondate <nether|end> <dd-MM-yyyy HH:mm>");
            } else {
                String dimension = args[0].toLowerCase();
                if (!dimension.equals("nether") && !dimension.equals("end")) {
                    sender.sendMessage("Неизвестное измерение. Нужно использовать 'nether' или 'end'.");
                } else {
                    try {
                        LocalDateTime date = LocalDateTime.parse(args[1] + " " + (args.length > 2 ? args[2] : "00:00"), this.dateFormatter);
                        this.unblockDate.put(dimension, date);
                        this.saveUnblockDate();
                        sender.sendMessage("Измерение " + dimension + " теперь закрыто до " + date.format(this.dateFormatter) + ".");
                    } catch (DateTimeParseException var7) {
                        sender.sendMessage("Что-то не так с форматом даты. Используйте dd-MM-yyyy HH:mm.");
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World.Environment targetEnvironment = event.getTo().getWorld().getEnvironment();
        if (targetEnvironment == World.Environment.NETHER) {
            if (LocalDateTime.now().isBefore(this.unblockDate.get("nether"))) {
                event.setCancelled(true);
                String red = String.valueOf(ChatColor.RED);
                String gray = String.valueOf(ChatColor.GRAY);
                player.sendActionBar(red + "Незер" + ChatColor.RESET + " закрыт до " + gray + this.unblockDate.get("nether").format(this.dateFormatter) + ChatColor.RESET);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.2F, 1.0F);
            }
        } else if (targetEnvironment == World.Environment.THE_END && LocalDateTime.now().isBefore(this.unblockDate.get("end"))) {
            event.setCancelled(true);
            String purple = String.valueOf(ChatColor.DARK_PURPLE);
            String gray = String.valueOf(ChatColor.GRAY);
            player.sendActionBar(purple + "Энд" + ChatColor.RESET + " закрыт до " + gray + this.unblockDate.get("end").format(this.dateFormatter) + ChatColor.RESET);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.2F, 1.0F);
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
        }
    }
}