package il.co.biosignals.minecraftlayer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class BiosignalsCommand implements CommandExecutor
{
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label,
			   String[] args)
  {
    MinecraftLayer.getInstance().getLogger().info(command.toString());
    MinecraftLayer.getInstance().getLogger().info(Arrays.stream(args).toList().toString());
    if (args.length < 1)
    {
      return (false);
    }
    if (args[0].contentEquals("uuid"))
    {
      if (!(sender instanceof Player))
        return (false);
      sender.sendMessage("UUID for " + sender.getName() + " : " + ((Player) sender).getUniqueId());
      return (true);
    }
    /*if (args.length < 2)
      return (false);
    if (args[0].contentEquals("test"))
    {
      MinecraftLayer.getInstance().getDatabaseQuerier().testState = Integer.parseInt(args[1]);
      sender.sendMessage("Test state set to " + MinecraftLayer.getInstance().getDatabaseQuerier().testState);
      return (true);
    }*/
    return (false);
  }
}
