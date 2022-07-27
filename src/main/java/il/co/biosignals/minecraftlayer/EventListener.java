package il.co.biosignals.minecraftlayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.UUID;

public class EventListener implements Listener
{
  private final JavaPlugin plugin;
  private final HologramManager hologramManager;
  private final DatabaseQuerier databaseQuerier;

  EventListener(JavaPlugin _plugin, HologramManager _hologramManager, DatabaseQuerier _databaseQuerier)
  {
    this.plugin = _plugin;
    this.hologramManager = _hologramManager;
    this.databaseQuerier = _databaseQuerier;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e)
  {
    final UUID userID = e.getPlayer().getUniqueId();
    final Player player = e.getPlayer();

    this.databaseQuerier.addPlayer(userID, Bukkit.getScheduler());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e)
  {
    this.databaseQuerier.removePlayer(e.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent e)
  {
    this.hologramManager.updateHologramLocationForPlayer(e.getPlayer());
  }
}
