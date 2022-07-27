package il.co.biosignals.minecraftlayer;

import org.bukkit.plugin.java.JavaPlugin;

public class MinecraftLayer extends JavaPlugin
{
  private final EventListener   eventListener;
  private final HologramManager hologramManager;
  private final DatabaseQuerier databaseQuerier;

  private static MinecraftLayer INSTANCE;

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

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this.eventListener, this);
  }

  @Override
  public void onDisable() {
    this.databaseQuerier.removeAll();
    this.hologramManager.clear();
  }
}
