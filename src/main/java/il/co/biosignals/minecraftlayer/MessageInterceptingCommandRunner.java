package il.co.biosignals.minecraftlayer;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MessageInterceptingCommandRunner implements ConsoleCommandSender {
  private final List<String> _logList;

  private final ConsoleCommandSender wrappedSender;
  private final Spigot spigotWrapper;

  public void addLog(String log)
  {
    _logList.add(ChatColor.stripColor(log));
  }

  private class Spigot extends CommandSender.Spigot {
    /**
     * Sends this sender a chat component.
     *
     * @param component the components to send
     */
    public void sendMessage(@NotNull BaseComponent component) {
      addLog(BaseComponent.toLegacyText(component));
      wrappedSender.spigot().sendMessage();
    }

    /**
     * Sends an array of components as a single message to the sender.
     *
     * @param components the components to send
     */
    public void sendMessage(@NotNull BaseComponent... components) {
      addLog(BaseComponent.toLegacyText(components));
      wrappedSender.spigot().sendMessage(components);
    }
  }


  public MessageInterceptingCommandRunner(ConsoleCommandSender wrappedSender,
                                          List<String> logList) {
    this.wrappedSender = wrappedSender;
    spigotWrapper = new Spigot();
    _logList = logList;
  }

  @Override
  public void sendMessage(@NotNull String message) {
    wrappedSender.sendMessage(message);
    addLog(message);
  }

  @Override
  public void sendMessage(@NotNull String[] messages) {
    wrappedSender.sendMessage(messages);
    for (String message : messages) {
      addLog(message);;
    }
  }

  @Override
  public void sendMessage(UUID sender, String message)
  {
    wrappedSender.sendMessage(sender, message);
    addLog(message);;
  }

  @Override
  public void sendMessage(UUID sender, String... messages)
  {
    wrappedSender.sendMessage(sender, messages);
    for (String message : messages) {
      addLog(message);;
    }
  }

  @Override
  public @NotNull Server getServer() {
    return wrappedSender.getServer();
  }

  @Override
  public @NotNull String getName() {
    return "OrderFulfiller";
  }

  @Override
  public @NotNull CommandSender.Spigot spigot() {
    return spigotWrapper;
  }

  @Override
  public boolean isConversing() {
    return wrappedSender.isConversing();
  }

  @Override
  public void acceptConversationInput(@NotNull String input) {
    wrappedSender.acceptConversationInput(input);
  }

  @Override
  public boolean beginConversation(@NotNull Conversation conversation) {
    return wrappedSender.beginConversation(conversation);
  }

  @Override
  public void abandonConversation(@NotNull Conversation conversation) {
    wrappedSender.abandonConversation(conversation);
  }

  @Override
  public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent details) {
    wrappedSender.abandonConversation(conversation, details);
  }

  @Override
  public void sendRawMessage(@NotNull String message) {
    addLog(message);;
    wrappedSender.sendRawMessage(message);
  }

  @Override
  public void sendRawMessage(UUID sender, String message)
  {
    addLog(message);;
    wrappedSender.sendRawMessage(sender, message);
  }

  @Override
  public boolean isPermissionSet(@NotNull String name) {
    return wrappedSender.isPermissionSet(name);
  }

  @Override
  public boolean isPermissionSet(@NotNull Permission perm) {
    return wrappedSender.isPermissionSet(perm);
  }

  @Override
  public boolean hasPermission(@NotNull String name) {
    return wrappedSender.hasPermission(name);
  }

  @Override
  public boolean hasPermission(@NotNull Permission perm) {
    return wrappedSender.hasPermission(perm);
  }

  @Override
  public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
    return wrappedSender.addAttachment(plugin, name, value);
  }

  @Override
  public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
    return wrappedSender.addAttachment(plugin);
  }

  @Override
  public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
    return wrappedSender.addAttachment(plugin, name, value, ticks);
  }

  @Override
  public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
    return wrappedSender.addAttachment(plugin, ticks);
  }

  @Override
  public void removeAttachment(@NotNull PermissionAttachment attachment) {
    wrappedSender.removeAttachment(attachment);
  }

  @Override
  public void recalculatePermissions() {
    wrappedSender.recalculatePermissions();
  }

  @Override
  public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
    return wrappedSender.getEffectivePermissions();
  }

  @Override
  public boolean isOp() {
    return wrappedSender.isOp();
  }

  @Override
  public void setOp(boolean value) {
    wrappedSender.setOp(value);
  }
}