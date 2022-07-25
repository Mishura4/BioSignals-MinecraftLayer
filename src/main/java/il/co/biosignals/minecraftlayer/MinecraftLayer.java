package il.co.biosignals.minecraftlayer;

import org.bukkit.plugin.java.JavaPlugin;

public class MinecraftLayer extends JavaPlugin
{
  EventListener listener;
  HologramManager hologramManager;

  @Override
  public void onEnable() {
    this.hologramManager = new HologramManager(this);
    this.listener = new EventListener(this, this.hologramManager);
    getServer().getPluginManager().registerEvents(this.listener, this);
  }
  @Override
  public void onDisable() {
    this.hologramManager.clear();
  }
}
