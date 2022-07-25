package il.co.biosignals.minecraftlayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class EventListener implements Listener
{
  private final JavaPlugin plugin;
  private final HologramManager hologramManager;

  EventListener(JavaPlugin _plugin, HologramManager _hologramManager)
  {
    this.plugin = _plugin;
    this.hologramManager = _hologramManager;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e)
  {
    this.hologramManager.updateHologramDataForPlayer(e.getPlayer(), "wee", "wee.png");
    //BukkitTask querier = new DatabaseQuerier(this.plugin).runTaskTimer(this.plugin, 20, 20);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent e)
  {
    this.hologramManager.updateHologramLocationForPlayer(e.getPlayer());
  }
}
