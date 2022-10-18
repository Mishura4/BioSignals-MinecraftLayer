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
import org.bukkit.util.Vector;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class HologramManager
{
  private final JavaPlugin plugin;

  private String itemOverride;
  public double topOffset;
  public double bottomOffset;

  private Map<String, Integer> customModelDataMap = new HashMap<>();

  private class HologramSet
  {
    public class Data
    {
      public LinkedList<String> linesString = new LinkedList<>();
      public Hologram hologram = null;
      public HologramLine line = null;
      public DatabaseQuerier.HologramData data = null;
      public boolean changed = false;
    }

    public enum ID
    {
      BOTTOM(0),
      TOP(1),
      PICTURE(2),
      MAX(3);

      public final int value;

      ID(int _value)
      {
        this.value = _value;
      }

      public int intValue()
      {
        return (this.value);
      }
    }

    private Data[] data = {new Data(), new Data(), new Data()};
  }

  Map<Player, HologramSet> holograms;

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

  public Location getPictureLocationForPlayer(Player player)
  {
    HologramSet hologramSet = this.holograms.get(player);
    Location hologramPosition = hologramSet.data[HologramSet.ID.PICTURE.intValue()].data.position;
    Location ret = player.getLocation();

    ret.add(hologramPosition.getX(), hologramPosition.getY(), hologramPosition.getZ());
    return (ret);
  }

  public Location getTopLocationForPlayer(Player player)
  {
    HologramSet hologramSet = this.holograms.get(player);
    Location hologramPosition = hologramSet.data[HologramSet.ID.TOP.intValue()].data.position;
    Location ret = player.getLocation();

    ret.add(hologramPosition.getX(), hologramPosition.getY(), hologramPosition.getZ());
    return (ret);
  }

  public Location getBottomLocationForPlayer(Player player)
  {
    HologramSet hologramSet = this.holograms.get(player);
    Location hologramPosition = hologramSet.data[HologramSet.ID.BOTTOM.intValue()].data.position;
    Location ret = player.getLocation();

    ret.add(hologramPosition.getX(), hologramPosition.getY(), hologramPosition.getZ());
    return (ret);
  }

  public String getColoredText(String text, String color)
  {
    if (color.isEmpty())
      return (text);
    return ("<" + color + ">" + text + "</" + color + ">");
  }

  private HologramSet.Data getMergedHolograms(HologramSet set, HologramSet.ID hologramId)
  {
    HologramSet.Data target = set.data[hologramId.intValue()];
    return (target);
    /*HologramSet.Data first = null;

    for (HologramSet.Data entry : set.data)
    {
      if (entry.data.position.equals(target.data.position))
      {
        first = entry;
        break;
      }
    }*/
  }

  public void changePosition(HologramSet set, HologramSet.ID hologramId, Location newPosition)
  {
  }

  public void changeTexture(HologramSet set, HologramSet.ID hologramId, String newTexture)
  {
    HologramSet.Data data = set.data[hologramId.intValue()];

    int modelId = customModelDataMap.getOrDefault(newTexture, 0);

    if (modelId == 0)
    {
      modelId = customModelDataMap.getOrDefault("error", 0);
      MinecraftLayer.getInstance()
                    .getLogger()
                    .warning("Could not find a model ID for name " +
                             newTexture +
                             " ; defaulting to model \"error\"");
      MinecraftLayer.getInstance()
                    .getLogger()
                    .warning(
                            "Make sure the item is registered in the configuration file at " +
                            MinecraftLayer.getInstance().getDataFolder() +
                            "/config.json");
      if (modelId == 0)
      {
        MinecraftLayer.getInstance()
                      .getLogger()
                      .warning("Model \"error\" could not be found, defaulting to 1");
        modelId = 1;
      }
    }

    MinecraftLayer.getInstance()
                  .getLogger()
                  .warning("Picture update is " + "#ICON: " + this.itemOverride + "{CustomModelData:" + modelId + "}");
    data.linesString.clear();
    data.linesString.add("#ICON: " + this.itemOverride + "{CustomModelData:" + modelId + "}");
    data.changed = true;
  }

  public void changeText(HologramSet set, HologramSet.ID hologramId, String newText)
  {
    HologramSet.Data data = set.data[hologramId.intValue()];

    data.linesString.add(newText);
    data.changed = true;
  }

  public void updateHologramDataForPlayer(Player player, DatabaseQuerier.PlayerData playerData)
  {
    HologramSet hologramSet = this.holograms.get(player);
    if (hologramSet == null)
    {
      hologramSet = new HologramSet();
      this.holograms.put(player, hologramSet);
      hologramSet.data[HologramSet.ID.TOP.intValue()].data = playerData.newData.topText;
      hologramSet.data[HologramSet.ID.BOTTOM.intValue()].data = playerData.newData.bottomText;
      hologramSet.data[HologramSet.ID.PICTURE.intValue()].data = playerData.newData.picture;
      hologramSet.data[HologramSet.ID.TOP.intValue()].hologram = DHAPI.createHologram(player.getName() + "_" + HologramSet.ID.TOP.intValue(), getTopLocationForPlayer(player));
      hologramSet.data[HologramSet.ID.BOTTOM.intValue()].hologram = DHAPI.createHologram(player.getName() + "_" + HologramSet.ID.BOTTOM.intValue(), getBottomLocationForPlayer(player));
      hologramSet.data[HologramSet.ID.PICTURE.intValue()].hologram = DHAPI.createHologram(player.getName() + "_" + HologramSet.ID.PICTURE.intValue(), getPictureLocationForPlayer(player));
    }
    else
    {
      hologramSet.data[HologramSet.ID.TOP.intValue()].data = playerData.newData.topText;
      hologramSet.data[HologramSet.ID.BOTTOM.intValue()].data = playerData.newData.bottomText;
      hologramSet.data[HologramSet.ID.PICTURE.intValue()].data = playerData.newData.picture;
    }

    for (int i = 0; i < HologramSet.ID.MAX.intValue(); ++i)
      hologramSet.data[i].linesString.clear();

    if (hologramSet == null)
      hologramSet = new HologramSet();

    if (playerData.newData.picture != null)
    {
      if (!playerData.newData.picture.position.equals(playerData.oldData.picture.position))
        changePosition(hologramSet, HologramSet.ID.PICTURE, playerData.newData.picture.position);
      changeTexture(hologramSet, HologramSet.ID.PICTURE, playerData.oldData.picture.picturePath);
    }

    DatabaseQuerier.HologramTextData textdata = playerData.newData.topText;

    //if (!textdata.position.equals(playerData.oldData.topText.position))
    if (!textdata.text.isEmpty())
    {
      changePosition(hologramSet, HologramSet.ID.TOP, textdata.position);
      changeText(hologramSet, HologramSet.ID.TOP, getColoredText(textdata.text, textdata.color));
    }

    textdata = playerData.newData.bottomText;
    if (!textdata.text.isEmpty())
    {
      changePosition(hologramSet, HologramSet.ID.BOTTOM, textdata.position);
      changeText(hologramSet, HologramSet.ID.BOTTOM, getColoredText(textdata.text, textdata.color));
    }

    for (HologramSet.Data entry : hologramSet.data)
    {
      if (entry.linesString.isEmpty())
      {
        for (int i = 0;
             i < DHAPI.getHologramPage(entry.hologram, 0).size(); ++i)
          DHAPI.removeHologramLine(entry.hologram, i);
      }
      else
        DHAPI.setHologramLines(entry.hologram, entry.linesString);
    }
    if (!playerData.oldData.topText.position.equals(playerData.newData.topText.position) ||
        !playerData.oldData.bottomText.position.equals(playerData.newData.bottomText.position) ||
        !playerData.oldData.picture.position.equals(playerData.newData.picture.position))
      updateHologramLocationForPlayer(player);
  }

  public void updateHologramLocationForPlayer(Player player)
  {
    HologramSet _hologram = this.holograms.get(player);

    if (_hologram == null) // We have not received data yet, nothing to display
      return;

    DHAPI.moveHologram(_hologram.data[HologramSet.ID.TOP.intValue()].hologram, getTopLocationForPlayer(player));
    DHAPI.moveHologram(_hologram.data[HologramSet.ID.BOTTOM.intValue()].hologram, getBottomLocationForPlayer(player));
    DHAPI.moveHologram(_hologram.data[HologramSet.ID.PICTURE.intValue()].hologram, getPictureLocationForPlayer(player));

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
    HologramSet hologram = this.holograms.remove(p);

    if (hologram == null)
      return;

    for (HologramSet.Data entry : hologram.data)
    {
      entry.hologram.delete();
    }
  }

  public void clear()
  {
    this.holograms.forEach((Player p, HologramSet h) ->
     {
       for (HologramSet.Data entry : h.data)
       {
         entry.hologram.delete();
       }
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
