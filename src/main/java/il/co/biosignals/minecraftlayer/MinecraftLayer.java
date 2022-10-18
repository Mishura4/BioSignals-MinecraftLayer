package il.co.biosignals.minecraftlayer;

import com.example.nms.accessors.CommandSourceAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.ConfigurationException;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import com.example.nms.accessors.CommandSourceStackAccessor;


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

  public List<String> collectedLogs = new LinkedList<>();

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
    //this.logHandler = new LogHandler();

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

  public boolean executeCommand(String command, Player player, List<String> logList)
  {
    ConsoleCommandSender sender = Bukkit.getConsoleSender();
    MessageInterceptingCommandRunner cmdRunner = new MessageInterceptingCommandRunner(Bukkit.getConsoleSender(), logList);

    try
    {
      String[] args = command.split(" ");

      if (args.length == 0)
        return (false);

      Field f;
      Method m;
      Class bukkitServerClass = Bukkit.getServer().getClass();
      String[] modules = bukkitServerClass.getName().split("\\.");
      modules = Arrays.copyOf(modules, modules.length - 1);
      String bukkitPackage = String.join(".", modules);

      m = Bukkit.getServer().getClass().getDeclaredMethod("getServer");
      m.setAccessible(true);
      Object dedicatedServer = m.invoke(Bukkit.getServer());
      Class commandListenerWrapperClass = Class.forName("net.minecraft.commands.CommandListenerWrapper");

      m = dedicatedServer.getClass().getSuperclass().getDeclaredMethod("aD"); // createCommandSourceStack
      Object sourceStack = CommandSourceStackAccessor.getType().cast(m.invoke(dedicatedServer)); // CommandSourceStack
      f = CommandSourceStackAccessor.getFieldSource();
      Object source = f.get(sourceStack);
      m = CommandSourceStackAccessor.getMethodWithSource1();
      Object myProxy = CommandSourceProxy.newInstance(source, logList);
      Object modifiedSourceStack = m.invoke(sourceStack,
                                            CommandSourceAccessor.getType().cast(myProxy));
      Object proxiedCommandSender = Class.forName(bukkitPackage + ".command.ProxiedNativeCommandSender")
                                         .getConstructor(commandListenerWrapperClass, CommandSender.class, CommandSender.class)
                                            .newInstance(modifiedSourceStack, cmdRunner, cmdRunner);

      f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      f.setAccessible(true);
      SimpleCommandMap commandMap = (SimpleCommandMap)f.get(Bukkit.getServer());

      String sentCommandLabel = args[0].toLowerCase(java.util.Locale.ENGLISH);
      Command c = commandMap.getCommand(sentCommandLabel);

      if (c == null)
      {
        logList.add("Unknown command.");
        return (false);
      }
      if (!c.testPermission(sender))
      {
        logList.add("Permission denied.");
        return (false);
      }

      boolean ret = c.execute((CommandSender) proxiedCommandSender, sentCommandLabel, Arrays.copyOfRange(args, 1, args.length));
      return (ret);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return (false);
  }

  public void handlePlayerRequestResponse(UUID playerUUID, String response)
  {
    String[] commands = response.split("\\\\\\\\");
    List<String> logList = new LinkedList<>();

    for(String command : commands)
      executeCommand(command, getServer().getPlayer(playerUUID), logList);
    String combinedLogs = String.join("\\\\", logList);
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
