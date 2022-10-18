package il.co.biosignals.minecraftlayer;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class BiosignalsCommand implements CommandExecutor
{
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label,
			   String[] args)
  {
    if (args.length < 1)
    {
      return (false);
    }
    if (args[0].contentEquals("uuid"))
    {
      if (!(sender instanceof Player))
        return (false);

      if (!sender.hasPermission("biosignals.biosignals_command.uuid"))
        return (false);
      String playerUUID = ((Player) sender).getUniqueId().toString().replace("-", "");

      BaseComponent[] component =
              new ComponentBuilder("UUID for " + sender.getName() + " : ")
                      .append(playerUUID)
                      .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, playerUUID))
                      .color(ChatColor.AQUA).underlined(true)
                      .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("<click to copy>").color(ChatColor.AQUA).italic(true).create())).create();
      ((Player)sender).spigot().sendMessage(component);
      return (true);
    }
    if (args.length < 2)
      return (false);
    if (args[0].contentEquals("request"))
    {
      if (!(sender instanceof Player))
        return (false);
      if (!sender.hasPermission("biosignals.biosignals_command.request"))
        return (false);
      final String[] requestArray = Arrays.copyOfRange(args, 1, args.length);
      final String request = String.join(" ", requestArray);
      final UUID playerUUID = ((Player) sender).getUniqueId();
      final String playerName = ((Player) sender).getName();

      MinecraftLayer.getInstance().getLogger().info("Player " + playerName + " is sending request \"" + request + "\"");

      Bukkit.getScheduler().runTaskAsynchronously(MinecraftLayer.getInstance(), () -> {
        try
        {
          final String response = MinecraftLayer.getInstance().getDatabaseQuerier().sendRequest(playerUUID, playerName, request);
          Bukkit.getScheduler().runTask(
            MinecraftLayer.getInstance(),
            () -> {
              MinecraftLayer.getInstance().getLogger().info("Player request from " + playerName + " has gotten response \"" + response + "\"");

              if (response.isEmpty())
                return;
              MinecraftLayer.getInstance().handlePlayerRequestResponse(
                playerUUID,
                response
              );
            }
          );
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      });
      return (true);
    }
    if (args[0].contentEquals("test"))
    {
      if (!sender.hasPermission("biosignals.biosignals_command.test"))
        return (false);
      MinecraftLayer.getInstance().getDatabaseQuerier().testState = Integer.parseInt(args[1]);
      sender.sendMessage("Test state set to " + MinecraftLayer.getInstance().getDatabaseQuerier().testState);
      return (true);
    }
    return (false);
  }
}
