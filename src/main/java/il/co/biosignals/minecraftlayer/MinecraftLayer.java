package il.co.biosignals.minecraftlayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.ConfigurationException;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MinecraftLayer extends JavaPlugin
{
  private static final int CONFIG_VERSION = 2;

  private final EventListener   eventListener;
  private final HologramManager hologramManager;
  private final DatabaseQuerier databaseQuerier;

  private static MinecraftLayer INSTANCE;

  private File configFile;
  private Map<String, Object> configValues = new HashMap<>();
  private boolean backedupConfig = false;

  private class ConfigurationException extends RuntimeException
  {
    public ConfigurationException(String errorMessage, Throwable err)
    {
      super(errorMessage, err);
    }
  }

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

  private void parseConfig()
  {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    try (FileReader reader = new FileReader(this.configFile))
    {
      this.configValues = gson.fromJson(reader, new HashMap<String, Object>().getClass());
    }
    catch (Exception e)
    {
      MinecraftLayer.getInstance().getLogger().warning("Error while loading configuration file " + this.configFile.getPath() + " : " + e.getMessage());
    }
  }

  public void backupConfig()
  {
    if (this.backedupConfig)
    return;

    this.backedupConfig = true;
    File backup = new File(getDataFolder(), "config.json.backup");

    try (
      InputStream in = new BufferedInputStream(new FileInputStream(this.configFile));
      OutputStream out = new BufferedOutputStream(new FileOutputStream(backup))
    )
    {
      byte[] buffer = new byte[1024];
      int lengthRead;
      while ((lengthRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, lengthRead);
        out.flush();
      }
    }
    catch (IOException e)
    {
      MinecraftLayer.getInstance().getLogger().warning("Error while backing up configuration file " + backup.getPath() + " : " + e.getMessage());
    }
  }

  public void saveConfig()
  {
    this.configFile = new File(getDataFolder(),"config.json");

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonObject object = new JsonObject();

    object.addProperty("config_version", CONFIG_VERSION);
    this.databaseQuerier.saveConfigToJson(object);
    this.hologramManager.saveConfigToJson(object);
    try (Writer writer = new FileWriter(this.configFile))
    {
      gson.toJson(object, writer);
    }
    catch (Exception e)
    {
      MinecraftLayer.getInstance().getLogger().warning("Error while saving configuration file " + this.configFile.getPath() + " : " + e.getMessage());
    }
  }

  public void loadConfig()
  {
    this.configFile = new File(getDataFolder(),"config.json");

    if (!this.configFile.exists())
      saveResource(this.configFile.getName(), false);
    parseConfig();

    String rawConfigVersion = this.configValues.getOrDefault("config_version", "").toString();
    int configVersion;

    try
    {
      configVersion = NumberFormat.getInstance().parse(rawConfigVersion).intValue();
    }
    catch (ParseException e)
    {
      configVersion = 0;
      MinecraftLayer.getInstance().getLogger().warning("Could not parse config version in " + this.configFile.getName() + " : " + e.getMessage());
    }

    if (configVersion != 2)
    {
      this.getLogger().warning("Wrong config file version detected, backing up.");
      backupConfig();
      this.backedupConfig = true;
    }

    BiFunction<String, Double, Double> getDouble = (String key, Double defaultValue) -> {
      double ret = (Optional.ofNullable(this.configValues.get(key)).map(v -> v.toString()).map(v -> {
        try
        {
          return DecimalFormat.getInstance().parse(v).doubleValue();
        }
        catch (ParseException e)
        {
          MinecraftLayer.getInstance().getLogger().info("Could not parse " + key + " : " + e.getMessage());
          return (null);
        }
      }).orElse(defaultValue));

      return (ret);
    };

    this.hologramManager.setItemOverride(this.configValues.getOrDefault("material_override", "BARRIER").toString());
    this.hologramManager.setTopOffset(getDouble.apply("top_offset", 2.10));
    this.hologramManager.setBottomOffset(getDouble.apply("bottom_offset", -0.25));

    this.databaseQuerier.setDatabaseServerURL(this.configValues.getOrDefault("database_server_url", "http://www.il-cn.com/BFA/Data/DBInterface.ashx").toString());

    loadCustomModelDataMap();
  }

  private void loadCustomModelDataMap()
  {
    Object rawValue;

    rawValue = this.configValues.get("custom_model_data_map");
    if (!(rawValue instanceof Map))
    {
      this.getLogger().warning("Invalid configuration file : custom_model_data_map is not the correct format (should be an object). Backing up configuration.");
      backupConfig();
      return;
    }

    Map rawCustomModelDataMap = (Map)rawValue;
    HashMap<String, Integer> customModelDataMap = new HashMap<>(rawCustomModelDataMap.size());

    rawCustomModelDataMap.forEach((Object key, Object value) -> {
      int modelId = 0;

      try
      {
        modelId = NumberFormat.getInstance().parse(value.toString()).intValue();
      }
      catch (ParseException e)
      {
        MinecraftLayer.getInstance().getLogger().warning("Ill-formed config.json when trying to parse " + value + " for model " + key + " : " + e.getMessage());
        return;
      }

      if (modelId == 0)
        return;

      customModelDataMap.put(key.toString(), modelId);
      MinecraftLayer.getInstance().getLogger().info("Registered custom model \"" + key.toString() + "\" => " + modelId);
    });

    this.hologramManager.loadCustomModelDataMap(customModelDataMap);
  }

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this.eventListener, this);
    getCommand("biosignals").setExecutor(new BiosignalsCommand());
    loadConfig();
    saveConfig();
  }

  @Override
  public void onDisable() {
    this.databaseQuerier.removeAll();
    this.hologramManager.clear();
  }
}
