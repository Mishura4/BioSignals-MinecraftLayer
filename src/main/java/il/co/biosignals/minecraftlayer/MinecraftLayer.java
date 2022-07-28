package il.co.biosignals.minecraftlayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class MinecraftLayer extends JavaPlugin
{
  private final EventListener   eventListener;
  private final HologramManager hologramManager;
  private final DatabaseQuerier databaseQuerier;

  private static MinecraftLayer INSTANCE;

  private File configFile;
  private Map<String, Object> configValues = new HashMap<>();

  public MinecraftLayer()
  {
    this.hologramManager = new HologramManager(this);
    this.databaseQuerier = new DatabaseQuerier(this);
    this.eventListener = new EventListener(this, this.hologramManager, this.databaseQuerier);

    MinecraftLayer.INSTANCE = this;
  }

  public EventListener getEventListener()
  {
    return (eventListener);
  }

  public HologramManager getHologramManager()
  {
    return (hologramManager);
  }

  public DatabaseQuerier getDatabaseQuerier()
  {
    return (databaseQuerier);
  }

  public static MinecraftLayer getInstance()
  {
    return (INSTANCE);
  }

  public void loadConfig()
  {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    this.configFile = new File(getDataFolder(),
                               "config.json");

    if (!this.configFile.exists())
      saveResource(this.configFile.getName(), false);

    try
    {
      this.configValues = gson.fromJson(new FileReader(this.configFile), new HashMap<String, Object>().getClass());
    }
    catch (FileNotFoundException e) // this should never happen but it's java so we have to do this i guess
    {
    }

    loadCustomModelDataMap();
  }

  private void loadCustomModelDataMap()
  {
    Object rawValue;

    rawValue = this.configValues.get("custom_model_data_map");
    if (!(rawValue instanceof Map))
    {
      this.getLogger().warning("Invalid configuration file : custom_model_data_map is not the correct format (should be an object)");
      return;
    }

    Map rawCustomModelDataMap = (Map)rawValue;
    HashMap<String, Integer> customModelDataMap = new HashMap<>(rawCustomModelDataMap.size());

    rawCustomModelDataMap.forEach((Object key, Object value) -> {
      int modelId = Integer.parseInt(key.toString());

      if (modelId == 0)
        return;

      customModelDataMap.put(value.toString(), modelId);
    });

    this.hologramManager.loadCustomModelDataMap(customModelDataMap);
  }

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this.eventListener, this);
    loadConfig();
  }

  @Override
  public void onDisable() {
    this.databaseQuerier.removeAll();
    this.hologramManager.clear();
  }
}
