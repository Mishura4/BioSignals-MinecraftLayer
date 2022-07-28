package il.co.biosignals.minecraftlayer;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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

  /*
    See this for volume : https://bukkit.org/threads/playsound-parameters-volume-and-pitch.151517/

    > I couldn't find exact information on playSound() so I had a look:

    > The volume of a sound source is determined by the volume parameter limited to the range 0.0 to 1.0. The volume of the sound as heard by the player is the volume of the sound multiplied by 1 minus the distance between the player and the source divided by the rolloff distance, multiplied by the player's sound volume setting. The rolloff distance is the greater of 16 and 16 times the volume. In code:

    > Code:
    > sourceVolume = max(0.0, min(volume, 1.0));
    > rolloffDistance = max(16, 16 * volume);
    > distance = player.getLocation().distance(location);

    > volumeOfSoundAtPlayer = sourceVolume * ( 1 - distance / rolloffDistance ) * PlayersSoundVolumeSetting;

    > This means that 1.0 is the loudest a sound can possibly be. Setting it higher increases the distance from which the sound can be heard. For example, sounds with volumes of 1.0 and 10.0 are just as loud at their sources, but the one with a volume of 1.0 can barely be heard 15 blocks away, while the other can still be heard 150 blocks away.
  */
  public void playSoundAroundPlayer(Player player, double radius, String resourcePath)
  {
    World world = player.getWorld();
    List<Player> playerList = world.getPlayers();
    double radiusSq = radius * radius;

    playerList.forEach((Player p) -> {
      if (p.getLocation().distanceSquared(player.getLocation()) < radiusSq)
        p.playSound(player.getLocation(), resourcePath, SoundCategory.PLAYERS, (float)radius / 16, 1.0f);
    });
  }

  public void removePlayer(Player p)
  {
    Hologram hologram = this.holograms.remove(p);

    if (hologram == null)
      return;

    hologram.delete();
  }

  public void clear()
  {
    this.holograms.forEach((Player p, Hologram h) ->
     {
       h.delete();
     });
  }
}
