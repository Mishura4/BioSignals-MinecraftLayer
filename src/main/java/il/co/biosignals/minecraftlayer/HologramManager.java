package il.co.biosignals.minecraftlayer;

import com.google.gson.JsonObject;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class HologramManager
{
  private final JavaPlugin plugin;

  private String itemOverride;
  private double topOffset;
  private double bottomOffset;

  private Map<String, Integer> customModelDataMap = new HashMap<>();

  private class HologramData
  {
    private Hologram topHologram;
    private Hologram bottomHologram;
  }

  Map<Player, HologramData> holograms;

  HologramManager(JavaPlugin _plugin)
  {
    this.plugin = _plugin;
    this.holograms = new HashMap<>();
  }

  public void setItemOverride(String string)
  {
    this.itemOverride = string;
  }

  public void loadCustomModelDataMap(Map<String, Integer> map)
  {
    this.customModelDataMap = map;
  }

  public void setTopOffset(double _topOffset)
  {
    this.topOffset = _topOffset;
  }

  public void setBottomOffset(double _bottomOffset)
  {
    this.bottomOffset = _bottomOffset;
  }

  public Location getTopLocationForPlayer(Player player)
  {
    Location ret = player.getLocation();

    ret.setY(ret.getY() + topOffset);
    return (ret);
  }

  public Location getBottomLocationForPlayer(Player player)
  {
    Location ret = player.getLocation();

    ret.setY(ret.getY() + bottomOffset);
    return (ret);
  }

  public String getColoredText(String text, String color)
  {
    if (color.isEmpty())
      return (text);
    return ("<" + color + ">" + text + "</" + color + ">");
  }

  public void updateHologramDataForPlayer(Player player, DatabaseQuerier.PlayerData playerData)
  {
    HologramData hologramData = this.holograms.get(player);
    List<String> topLines = new LinkedList<>();
    List<String> bottomLines = new LinkedList<>();

    if (hologramData == null)
    {/*
      // --- TODO : find out why holograms are saved, and why we need to check if they exist when someone logs in
      hologramData = DHAPI.getHologram(player.getName());
      if (hologramData != null)
        DHAPI.removeHologram(player.getName());
      // ---*/

      hologramData = new HologramData();
      hologramData.topHologram = DHAPI.createHologram(player.getName() + "_top", getTopLocationForPlayer(player), false);
      hologramData.topHologram.setDownOrigin(true);
      hologramData.bottomHologram = DHAPI.createHologram(player.getName() + "_bottom", getBottomLocationForPlayer(player), false);
      this.holograms.put(player, hologramData);
    }

    if(!playerData.newData.texturePath.isEmpty())
    {
      int modelId = customModelDataMap.getOrDefault(playerData.newData.texturePath, 0);

      if (modelId == 0)
      {
        if (!playerData.newData.texturePath.contentEquals(playerData.oldData.texturePath))
        {
          MinecraftLayer.getInstance()
                        .getLogger()
                        .warning("Could not find a model ID for name " +
                                 playerData.newData.texturePath +
                                 " ; defaulting to model ID 1");
          MinecraftLayer.getInstance()
                        .getLogger()
                        .warning(
                                "Make sure the item is registered in the configuration file at " +
                                MinecraftLayer.getInstance().getDataFolder() +
                                "/config.json");
        }
        modelId = 1;
      }

      topLines.add("#ICON: " + this.itemOverride + "{CustomModelData:" + modelId + "}");
    }

    if (!playerData.newData.topText.isEmpty())
      topLines.add(getColoredText(playerData.newData.topText, playerData.newData.topTextColor));

    if (!playerData.newData.bottomText.isEmpty())
      bottomLines.add(getColoredText(playerData.newData.bottomText, playerData.newData.bottomTextColor));

    if (topLines.isEmpty())
    {
      for (int i = 0; i < DHAPI.getHologramPage(hologramData.topHologram, 0).size(); ++i)
        DHAPI.removeHologramLine(hologramData.topHologram, i);
    }
    else
      DHAPI.setHologramLines(hologramData.topHologram, topLines);

    if (bottomLines.isEmpty())
    {
      for (int i = 0; i < DHAPI.getHologramPage(hologramData.bottomHologram, 0).size(); ++i)
        DHAPI.removeHologramLine(hologramData.bottomHologram, i);
    }
    else
      DHAPI.setHologramLines(hologramData.bottomHologram, bottomLines);
  }

  public void updateHologramLocationForPlayer(Player player)
  {
    HologramData _hologram = this.holograms.get(player);

    if (_hologram == null) // We have not received data yet, nothing to display
      return;

    DHAPI.moveHologram(_hologram.topHologram, getTopLocationForPlayer(player));
    DHAPI.moveHologram(_hologram.bottomHologram, getBottomLocationForPlayer(player));

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
    HologramData hologram = this.holograms.remove(p);

    if (hologram == null)
      return;

    hologram.topHologram.delete();
    hologram.bottomHologram.delete();
  }

  public void clear()
  {
    this.holograms.forEach((Player p, HologramData h) ->
     {
       h.topHologram.delete();
       h.bottomHologram.delete();
     });
  }

  public void saveConfigToJson(JsonObject object)
  {
    object.addProperty("material_override", this.itemOverride);
    object.addProperty("top_offset", this.topOffset);
    object.addProperty("bottom_offset", this.bottomOffset);

    JsonObject modelDataMapObject = new JsonObject();
    List<Map.Entry<String, Integer>> entries = this.customModelDataMap.entrySet().stream()
                                                       .sorted(Comparator.comparing(Map.Entry::getValue))
                                                       .collect(Collectors.toList());
    entries.forEach((tuple) -> modelDataMapObject.addProperty(tuple.getKey(), tuple.getValue()));
    object.add("custom_model_data_map", modelDataMapObject);
  }
}
