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
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DatabaseQuerier
{
  private String databaseServer = "http://www.il-cn.com/BFA/Data/DBInterface.ashx";

  private final JavaPlugin plugin;

  private Map<UUID, PlayerData> playerDataMap;

  public int testState = 0;

  public class PlayerData
  {
    public final UUID uuid;
    public final String firstName;
    public final String lastName;
    public final int userNumber;
    public final String realTimeProcedureName;
    public final long realTimeTimer;

    public final RealTimeDataFetcher dataFetcher;
    public PlayerRealTimeData oldData;
    public PlayerRealTimeData newData;
    public boolean hasPackLoaded = false;

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
      this.newData = new PlayerRealTimeData("", "", "", "", "", "");
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

  public void setLoadedPack(UUID playerUUID)
  {
    PlayerData data = this.playerDataMap.get(playerUUID);

    if (data == null)
      return;
    data.hasPackLoaded = true;
  }

  public void addPlayer(UUID playerUUID, String playerName, BukkitScheduler scheduler)
  {
    try
    {
      PlayerData data = queryPlayerData(playerUUID, playerName);

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
      return;
    pData.dataFetcher.cancel();
  }

  public void removeAll()
  {
    this.playerDataMap.forEach((UUID playerUUID, PlayerData data) -> {
      data.dataFetcher.cancel();
    });
    this.playerDataMap.clear();
  }

  public PlayerData queryPlayerData(UUID playerUUID, String playerName) throws IOException, InterruptedException
  {
    HttpClient httpClient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost(databaseServer);
    List<NameValuePair> params = new ArrayList<>(2);
    params.add(new BasicNameValuePair("storeprocedure",
                                      "Get_minecraft_user_details"));
    params.add(new BasicNameValuePair("param1",
                                      playerUUID.toString().replace("-", "")));
    params.add(new BasicNameValuePair("param2", playerName));
    httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    HttpResponse httpResponse;

    try
    {
      httpResponse = httpClient.execute(httpPost);
    }
    catch (IOException e)
    {
      MinecraftLayer.getInstance().getLogger().warning("Error during sending request to server at \"" + this.databaseServer + "\" : "
                                                       + Optional.ofNullable(e.getMessage()).orElse(e.getCause().getMessage()));
      return (null);
    }

    HttpEntity hentity = httpResponse.getEntity();

    if (hentity == null)
    {
      this.plugin.getLogger().warning("Retrieving information for " + playerUUID.toString() + " failed :");
      this.plugin.getLogger().warning(httpResponse.getStatusLine().toString());
      return (null);
    }

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

    JsonElement jsonRoot;

    if ((this.testState & 0x40) == 0)
    {
      MinecraftLayer.getInstance().getLogger().fine("Querying the server for " +
                                                    "real time data...");

      HttpClient httpClient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(databaseServer);
      List<NameValuePair> params = new ArrayList<>(2);
      HttpResponse httpResponse;
      HttpEntity hentity;

      params.add(new BasicNameValuePair("storeprocedure",
                                        pData.realTimeProcedureName));
      params.add(new BasicNameValuePair("param1",
                                        String.valueOf(pData.userNumber)));
      httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

      try
      {
        httpResponse = httpClient.execute(httpPost);
      }
      catch (Exception e)
      {
        MinecraftLayer.getInstance()
                      .getLogger()
                      .warning("Error during sending request to server : " + e.getMessage());
        return (null);
      }
      hentity = httpResponse.getEntity();

      if (hentity == null)
      {
        this.plugin.getLogger()
                   .warning("Retrieving real-time data for " + playerUUID.toString() + " failed :");
        this.plugin.getLogger().warning(httpResponse.getStatusLine().toString());
        return (null);
      }

      String response = EntityUtils.toString(hentity);
      jsonRoot = JsonParser.parseString(response);
    }
    else
    {
      File testFile = new File(this.plugin.getDataFolder(), "testrealtimedata.json");
      jsonRoot = JsonParser.parseReader(new FileReader(testFile));
    }

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
            ((this.testState & 0x1) != 0 ? "" : jsonData.get("top_text").getAsString()),
            ((this.testState & 0x2) != 0 ? "" : jsonData.get("top_text_color").getAsString()),
            ((this.testState & 0x4) != 0 ? "" : jsonData.get("bottom_text").getAsString()),
            ((this.testState & 0x8) != 0 ? "" : jsonData.get("bottom_text_color").getAsString()),
            ((this.testState & 0x10) != 0 ? "" : jsonData.get("picture_url").getAsString()),
            ((this.testState & 0x20) != 0 ? "" : jsonData.get("audio_url").getAsString())
    );

    return (pRTData);
  }

  public void setDatabaseServerURL(String url)
  {
    this.databaseServer = url;
  }

  public void saveConfigToJson(JsonObject object)
  {
    object.addProperty("database_server_url", this.databaseServer);
  }

  public void playSound(Player player)
  {
    PlayerData data = this.playerDataMap.get(player.getUniqueId());

    if (data == null)
      return;

    if (!data.newData.soundPath.isEmpty())
    {
      try
      {
        player.playSound(player.getLocation(),"biosignals:" + data.newData.soundPath, 16.0f, 1.0f);
      }
      catch (Exception e)
      {
        MinecraftLayer.getInstance().getLogger().warning("Error while trying to play sound " + data.newData.soundPath + " : ");
        MinecraftLayer.getInstance().getLogger().warning(e.getMessage());
      }
    }
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
        pRTData = MinecraftLayer.getInstance().getDatabaseQuerier().queryPlayerRealTimeData(this.playerData.uuid);

        if (this.isCancelled()) // We need to check for this in case the query started BEFORE we cancelled but finished AFTER
        {
          MinecraftLayer.getInstance().getLogger().fine("Server responded after cancelling - aborting");
          return;
        }
        if (pRTData == null)
          return;

        this.playerData.oldData = this.playerData.newData;
        this.playerData.newData = pRTData;
        BukkitScheduler scheduler = Bukkit.getScheduler();
        this.task = scheduler.runTask(MinecraftLayer.getInstance(), () -> {
          Player player = Bukkit.getPlayer(this.playerData.uuid);

          if (player == null)
            return;

          MinecraftLayer.getInstance().getHologramManager().updateHologramDataForPlayer(player, this.playerData);

          if (this.playerData.hasPackLoaded == true && !this.playerData.newData.soundPath.contentEquals(this.playerData.oldData.soundPath))
            playSound(player);
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
