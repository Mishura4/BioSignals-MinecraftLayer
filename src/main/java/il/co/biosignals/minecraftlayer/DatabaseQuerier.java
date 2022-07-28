package il.co.biosignals.minecraftlayer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class DatabaseQuerier
{
  private static final String SERVER_INTERFACE_URL = "http://www.il-cn.com/BFA/Data/DBInterface.ashx";

  private final JavaPlugin plugin;

  private Map<UUID, PlayerData> playerDataMap;

  public class PlayerData
  {
    public final UUID uuid;
    public final String firstName;
    public final String lastName;
    public final int userNumber;
    public final String realTimeProcedureName;
    public final long realTimeTimer;

    public final RealTimeDataFetcher dataFetcher;

    protected PlayerData(UUID _uuid, String _firstName, String _lastName, int _userNumber,
                         String _realTimeProcedureName, int _realTimeTimer)
    {
      this.uuid = _uuid;
      this.firstName = _firstName;
      this.lastName = _lastName;
      this.userNumber = _userNumber;
      this.realTimeProcedureName = _realTimeProcedureName;
      this.realTimeTimer = _realTimeTimer;
      this.dataFetcher = new RealTimeDataFetcher(this);
    }

    @Override
    public String toString()
    {
      return("{UUID: " + this.uuid + "; Name: " + this.firstName + " "
             + this.lastName + "; User Number: " + this.userNumber + "; Procedure : "
             + this.realTimeProcedureName + "; Timer: " + String.valueOf(this.realTimeTimer)+ "}");
    }
  }

  public class PlayerRealTimeData
  {
    public final String topText;
    public final String topTextColor;
    public final String bottomText;
    public final String bottomTextColor;
    public final String texturePath;
    public final String soundPath;

    public PlayerRealTimeData(String _topText, String _topTextColor,
                              String _bottomText, String _bottomTextColor,
                              String _texturePath, String _soundPath)
    {
      this.topText = _topText;
      this.topTextColor = _topTextColor;
      this.bottomText = _bottomText;
      this.bottomTextColor = _bottomTextColor;
      this.texturePath = _texturePath;
      this.soundPath = _soundPath;
    }
  }

  DatabaseQuerier(JavaPlugin _plugin)
  {
    this.plugin = _plugin;
    this.playerDataMap = new HashMap<>();
  }

  public void addPlayer(UUID playerUUID, BukkitScheduler scheduler)
  {
    try
    {
      PlayerData data = queryPlayerData(playerUUID);

      if (data != null)
      {
        data.dataFetcher.runTaskTimerAsynchronously(MinecraftLayer.getInstance(),
                                              0,
                                             data.realTimeTimer * 20);
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  public void removePlayer(UUID playerUUID)
  {
    PlayerData pData = this.playerDataMap.remove(playerUUID);

    if (pData == null)
    {
      this.plugin.getLogger().warning("Internal data for player " + playerUUID.toString() + " could not be found.");
      return;
    }
    pData.dataFetcher.cancel();
  }

  public void removeAll()
  {
    this.playerDataMap.forEach((UUID playerUUID, PlayerData data) -> {
      data.dataFetcher.cancel();
    });
    this.playerDataMap.clear();
  }

  public PlayerData queryPlayerData(UUID playerUUID) throws IOException, InterruptedException
  {
    HttpClient httpClient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost(SERVER_INTERFACE_URL);
    List<NameValuePair> params = new ArrayList<>(2);
    params.add(new BasicNameValuePair("storeprocedure",
                                      "Get_minecraft_user_details"));
    params.add(new BasicNameValuePair("param1",
                                      playerUUID.toString().replace("-", "")));
    httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    this.plugin.getLogger().info(httpPost.toString());
    Arrays.stream(httpPost.getAllHeaders()).toList().forEach((Header header) -> {this.plugin.getLogger().info(header.toString());});

    this.plugin.getLogger().info(EntityUtils.toString(httpPost.getEntity()));

    HttpResponse httpResponse = httpClient.execute(httpPost);
    HttpEntity hentity = httpResponse.getEntity();

    if (hentity == null)
    {
      this.plugin.getLogger().warning("Retrieving information for " + playerUUID.toString() + " failed :");
      this.plugin.getLogger().warning(httpResponse.getStatusLine().toString());
      return (null);
    }

    /*
    [
      {
        "first_name":"????? ??",
        "family_name":"?? ????? ???????",
        "user_number":1,
        "real_time_procedure_name":"Get_minecraft_real_time_data",
        "real_time_timer":1
      }
     ]
     */

    String response = EntityUtils.toString(hentity);
    JsonElement jsonRoot = JsonParser.parseString(response);
    if (jsonRoot.isJsonArray()) // The server does this at the time of writing this code, so
      jsonRoot = jsonRoot.getAsJsonArray().get(0);
    if (!jsonRoot.isJsonObject())
    {
      this.plugin.getLogger().warning("Retrieving information for " + playerUUID.toString() + " failed :");
      this.plugin.getLogger().warning("Server sent an invalid JSON  :");
      this.plugin.getLogger().warning(jsonRoot.toString());
      return (null);
    }
    JsonObject jsonData = jsonRoot.getAsJsonObject();

    PlayerData playerData = new PlayerData(
            playerUUID,
            jsonData.get("first_name").getAsString(),
            jsonData.get("family_name").getAsString(),
            jsonData.get("user_number").getAsInt(),
            jsonData.get("real_time_procedure_name").getAsString(),
            jsonData.get("real_time_timer").getAsInt()
    );
    this.playerDataMap.put(playerUUID, playerData);
    return (playerData);
  }

  public PlayerRealTimeData queryPlayerRealTimeData(UUID playerUUID) throws IOException, InterruptedException
  {
    PlayerData pData = this.playerDataMap.get(playerUUID);

    if (pData == null)
    {
      this.plugin.getLogger().warning("Internal data for player " + playerUUID.toString() + " could not be found during real time data query.");
      return (null);
    }
    MinecraftLayer.getInstance().getLogger().fine("Querying the server for real time data...");

    HttpClient httpClient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost(SERVER_INTERFACE_URL);
    List<NameValuePair> params = new ArrayList<>(2);
    HttpResponse httpResponse;
    HttpEntity hentity;

    params.add(new BasicNameValuePair("storeprocedure",
                                      pData.realTimeProcedureName));
    params.add(new BasicNameValuePair("param1",
                                      String.valueOf(pData.userNumber)));
    httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    httpResponse = httpClient.execute(httpPost);
    hentity = httpResponse.getEntity();

    if (hentity == null)
    {
      this.plugin.getLogger().warning("Retrieving real-time data for " + playerUUID.toString() + " failed :");
      this.plugin.getLogger().warning(httpResponse.getStatusLine().toString());
      return (null);
    }

    String response = EntityUtils.toString(hentity);
    JsonElement jsonRoot = JsonParser.parseString(response);
    if (jsonRoot.isJsonArray()) // See comment in queryPlayerData
      jsonRoot = jsonRoot.getAsJsonArray().get(0);
    if (!jsonRoot.isJsonObject())
    {
      this.plugin.getLogger().warning("Retrieving real-time for " + playerUUID.toString() + " failed :");
      this.plugin.getLogger().warning("Server sent an invalid JSON object");
      return (null);
    }

    JsonObject jsonData = jsonRoot.getAsJsonObject();

    PlayerRealTimeData pRTData = new PlayerRealTimeData(
            jsonData.get("top_text").getAsString(),
            jsonData.get("top_text_color").getAsString(),
            jsonData.get("bottom_text").getAsString(),
            jsonData.get("bottom_text_color").getAsString(),
            jsonData.get("picture_url").getAsString(),
            jsonData.get("audio_url").getAsString()
    );

    return (pRTData);
  }

  public class RealTimeDataFetcher extends BukkitRunnable
  {
    private BukkitTask task;
    private final PlayerData playerData;

    public RealTimeDataFetcher(PlayerData playerData)
    {
      this.playerData = playerData;
    }

    public void query()
    {

      DatabaseQuerier.PlayerRealTimeData pRTData;



      try
      {
        pRTData = MinecraftLayer.getInstance().getDatabaseQuerier().queryPlayerRealTimeData(playerData.uuid);

        if (this.isCancelled()) // We need to check for this in case the query started BEFORE we cancelled but finished AFTER
        {
          MinecraftLayer.getInstance().getLogger().fine("Server responded after cancelling - aborting");
          return;
        }
        BukkitScheduler scheduler = Bukkit.getScheduler();
        this.task = scheduler.runTask(MinecraftLayer.getInstance(), () -> {
          Player player = Bukkit.getPlayer(this.playerData.uuid);

          MinecraftLayer.getInstance().getHologramManager().updateHologramDataForPlayer(player,
                                                                                        pRTData.topText, pRTData.topTextColor,
                                                                                        pRTData.bottomText, pRTData.bottomTextColor,
                                                                                        pRTData.texturePath);
          MinecraftLayer.getInstance().getHologramManager().playSoundAroundPlayer(player, 32, pRTData.soundPath);
        });
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException
    {
      this.task.cancel();
      super.cancel();
    }

    @Override
    public void run()
    {
      query();
    }
  }
}
