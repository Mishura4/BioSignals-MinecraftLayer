package il.co.biosignals.minecraftlayer;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class HologramManager
{
  private final JavaPlugin plugin;

  Map<Player, Hologram> holograms;

  HologramManager(JavaPlugin _plugin)
  {
    this.plugin = _plugin;
    this.holograms = new HashMap<>();
  }

  public Location getLocationForPlayer(Player player)
  {
    Location ret = player.getLocation();

    ret.setY(ret.getY() + 2.50);
    return (ret);
  }

  public void updateHologramDataForPlayer(Player player, String text, String imageURL)
  {
    Hologram _hologram = this.holograms.get(player);

    if (_hologram == null)
    {
      // --- TODO : find out why holograms are saved, and why we need to check if they exist when someone logs in
      _hologram = DHAPI.getHologram(player.getName());
      if (_hologram != null)
        DHAPI.removeHologram(player.getName());
      // ---

      _hologram = DHAPI.createHologram(player.getName(), getLocationForPlayer(player), false);
      this.plugin.getLogger().info(_hologram.toString());
      this.holograms.put(player, _hologram);
    }

    DHAPI.addHologramLine(_hologram, imageURL);
    DHAPI.addHologramLine(_hologram, text);
  }

  public void updateHologramLocationForPlayer(Player player)
  {
    Hologram _hologram = this.holograms.get(player);

    if (_hologram == null) // We have not received data yet, nothing to display
      return;

   DHAPI.moveHologram(_hologram, getLocationForPlayer(player));

    /* TODO: fix this? why doesnt it work but the code above does??
    _hologram.setLocation(getLocationForPlayer(player));
    _hologram.realignLines();
     */
  }

  public void clear()
  {
  }
}
