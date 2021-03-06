package com.ericlam.mc.nametaghide;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class NameTagHide extends JavaPlugin implements Listener, CommandExecutor {
    private List<String> worlds = new ArrayList<>();
    private HashMap<UUID, Scoreboard> cache = new HashMap<>();

    @Override
    public void onEnable() {
        this.getLogger().info("NameTagHide Enabled.");
        this.saveDefaultConfig();
        loadConfig();
        this.getServer().getPluginManager().registerEvents(this,this);
    }

    private void loadConfig(){
        worlds = this.getConfig().getStringList("enable-worlds");
    }

    private void registerScoreboard(UUID uuid,Scoreboard scoreboard){
        if (cache.containsKey(uuid)) return;
        Team hide = scoreboard.getTeam("HideTag");
        Team show = scoreboard.getTeam("ShowTag");
        if (hide == null){
            Team teamHide = scoreboard.registerNewTeam("HideTag");
            teamHide.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            teamHide.setAllowFriendlyFire(true);
            teamHide.setCanSeeFriendlyInvisibles(false);
        }
        if (show == null){
           Team teamShow = scoreboard.registerNewTeam("ShowTag");
           teamShow.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
           teamShow.setAllowFriendlyFire(true);
           teamShow.setCanSeeFriendlyInvisibles(false);
        }
        cache.put(uuid,scoreboard);
    }

    private void restoreScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        Scoreboard scoreboard = player.getScoreboard();
        if (!scoreboard.equals(cache.get(uuid))) {
            player.setScoreboard(cache.get(uuid));
        }
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
       Bukkit.getScheduler().runTask(this,()->{
           Player player = e.getPlayer();
           UUID uuid = player.getUniqueId();
           Scoreboard scoreboard = player.getScoreboard();
           if (scoreboard==null) scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
           registerScoreboard(uuid,scoreboard);
           if (cache.containsKey(player.getUniqueId())) {
               controlVisibility(scoreboard, player.getWorld(), player);
           }
       });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e){
        Player player = e.getPlayer();
        if (!cache.containsKey(player.getUniqueId())) return;
        restoreScoreboard(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            World world = e.getPlayer().getWorld();
            Scoreboard scoreboard = cache.get(player.getUniqueId());
            controlVisibility(scoreboard, world, player);
        }, 60L);

    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        World world = e.getRespawnLocation().getWorld();
        if (!cache.containsKey(player.getUniqueId())) return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            restoreScoreboard(player);
            Scoreboard scoreboard = cache.get(player.getUniqueId());
            controlVisibility(scoreboard, world, player);
        }, 30L);
    }

    private void controlVisibility(Scoreboard scoreboard,World world,Player player){
        String name = player.getName();
        Team hideTeam = scoreboard.getTeam("HideTag");
        Team showTeam = scoreboard.getTeam("ShowTag");
        if (!worlds.contains(world.getName())){
            //this.getLogger().info("Showing "+player.getName());
            if (hideTeam.getEntries().contains(name)) hideTeam.removeEntry(name);
            if (!showTeam.getEntries().contains(name)) showTeam.addEntry(name);
        }else{
            //this.getLogger().info("Hiding "+player.getName());
            if (!hideTeam.getEntries().contains(name)) hideTeam.addEntry(name);
            if (showTeam.getEntries().contains(name)) showTeam.removeEntry(name);
        }
        player.setScoreboard(hideTeam.getScoreboard());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("nth-reload")){
            if (!sender.hasPermission("nth.reload")) return false;
            this.reloadConfig();
            loadConfig();
            sender.sendMessage(ChatColor.GREEN+"Reload completed.");
        }

        return true;
    }
}
