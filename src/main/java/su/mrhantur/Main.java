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
import static java.time.Duration.between;
import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.loadUnblockDate();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("setunblockdate").setExecutor(this);
        this.getCommand("servertime").setExecutor(this);
        Bukkit.getConsoleSender().sendMessage("===================================");
        Bukkit.getConsoleSender().sendMessage("Dimensional plugin is now working!");
        Bukkit.getConsoleSender().sendMessage("===================================");
    }

    private final Map<String, LocalDateTime> unblockDate = new HashMap();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");


    public void onDisable() {
        this.saveUnblockDate();
    }

    private String getRemainingTime(LocalDateTime unlockTime) {
        long seconds = between(LocalDateTime.now(), unlockTime).getSeconds();
        if (seconds <= 0) return "0 секунд";

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long sec = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" дн ");
        if (hours > 0) sb.append(hours).append(" ч ");
        if (minutes > 0) sb.append(minutes).append(" мин ");
        if (days == 0) sb.append(sec).append(" сек");
        return sb.toString();
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
        if (command.getName().equalsIgnoreCase("setunblockdate")) {
            if (args.length < 2) {
                sender.sendMessage("Подсказка: /setunblockdate <nether|end> <dd-MM-yyyy HH:mm>");
            } else {
                String dimension = args[0].toLowerCase();
                if (!dimension.equals("nether") && !dimension.equals("end")) {
                    sender.sendMessage("Неизвестное измерение. Нужно использовать 'nether' или 'end'");
                } else {
                    String dateString = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    try {
                        LocalDateTime date = LocalDateTime.parse(dateString, this.dateFormatter);
                        this.unblockDate.put(dimension, date);
                        this.saveUnblockDate();
                        sender.sendMessage("Измерение " + ChatColor.GRAY + dimension + ChatColor.RESET + " теперь закрыто до " + ChatColor.GRAY + date.format(this.dateFormatter));
                    } catch (DateTimeParseException e) {
                        sender.sendMessage("Что-то не так с форматом даты. Используйте dd-MM-yyyy HH:mm");
                    }
                }
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("servertime")) {
            sender.sendMessage("Текущее время сервера " + ChatColor.GRAY + LocalDateTime.now().format(dateFormatter));
            sender.sendMessage("Время разблокировки " + ChatColor.RED + "Незера " + ChatColor.GRAY + this.unblockDate.get("nether").format(this.dateFormatter));
            sender.sendMessage("Время разблокировки " + ChatColor.DARK_PURPLE + "Энда " + ChatColor.GRAY + this.unblockDate.get("end").format(this.dateFormatter));
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World.Environment targetEnvironment = event.getTo().getWorld().getEnvironment();
        if (targetEnvironment == World.Environment.NETHER) {
            if (LocalDateTime.now().isBefore(this.unblockDate.get("nether"))) {
                event.setCancelled(true);
                player.sendActionBar(ChatColor.RED + "Незер" + ChatColor.RESET + " закрыт ещё " + ChatColor.GRAY + getRemainingTime(this.unblockDate.get("nether")));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.2F, 1.0F);
            }
        } else if (targetEnvironment == World.Environment.THE_END && LocalDateTime.now().isBefore(this.unblockDate.get("end"))) {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.DARK_PURPLE + "Энд" + ChatColor.RESET + " закрыт ещё " + ChatColor.GRAY + getRemainingTime(this.unblockDate.get("end")));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.2F, 1.0F);
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
        }
    }
}