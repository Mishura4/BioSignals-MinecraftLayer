package il.co.biosignals.minecraftlayer;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class HologramManager
{
  private final JavaPlugin plugin;

  private Map<String, Integer> customModelDataMap = new HashMap<>();

  Map<Player, Hologram> holograms;

  HologramManager(JavaPlugin _plugin)
  {
    this.plugin = _plugin;
    this.holograms = new HashMap<>();
  }

  public void loadCustomModelDataMap(Map<String, Integer> map)
  {
    this.customModelDataMap = map;
  }

  public Location getLocationForPlayer(Player player)
  {
    Location ret = player.getLocation();

    ret.setY(ret.getY() + 3.25);
    return (ret);
  }

  public String getColoredText(String text, String color)
  {
    return ("<" + color + ">" + text + "</" + color + ">");
  }

  public void updateHologramDataForPlayer(Player player,
                                          String topText, String topTextColor,
                                          String bottomText, String bottomTextColor,
                                          String imageURL)
  {
    Hologram _hologram = this.holograms.get(player);

    if (_hologram == null)
    {
      // --- TODO : find out why holograms are saved, and why we need to check if they exist when someone logs in
      _hologram = DHAPI.getHologram(player.getName());
      if (_hologram != null)
        DHAPI.removeHologram(player.getName());
      // ---

      _hologram = DHAPI.createHologram(player.getName(), getLocationForPlayer(player), false, Arrays.asList("", "", ""));
      this.holograms.put(player, _hologram);
    }

    imageURL = "test_item";
    int modelId = customModelDataMap.getOrDefault(imageURL, 0);

    if (modelId == 0)
    {
      MinecraftLayer.getInstance().getLogger().warning("Could not find a model ID for name " + imageURL + " ; defaulting to model ID 1");
      MinecraftLayer.getInstance().getLogger().warning("Make sure the item is registered in the configuration file at " + MinecraftLayer.getInstance().getDataFolder() + "/config.json");
      modelId = 1;
    }

    DHAPI.setHologramLine(_hologram,0, "#ICON: SHULKER_SHELL{CustomModelData:" + modelId + "}");
    DHAPI.setHologramLine(_hologram, 1, getColoredText(topText, topTextColor));
    DHAPI.setHologramLine(_hologram, 2, getColoredText(bottomText, bottomTextColor));
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
