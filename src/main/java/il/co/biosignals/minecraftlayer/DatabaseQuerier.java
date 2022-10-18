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
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DatabaseQuerier
{
  private Map pickedupLogs = new HashMap();

  public abstract class HologramData
  {
    public final Location position;

    public HologramData(Location _position)
    {
      this.position = _position;
    }

    protected boolean _equals(HologramData other)
    {
      return (position.equals(other.position));
    }
  }

  public class HologramTextData extends HologramData
  {
    public final String text;
    public final String color;

    public HologramTextData(Location _position, String _text, String _color)
    {
      super(_position);
      this.text = _text;
      this.color = _color;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (!(obj instanceof HologramTextData))
        return (false);

      HologramTextData other = (HologramTextData) obj;

      if (!super._equals(other))
        return (false);
      if (!this.text.contentEquals(other.text))
        return (false);
      if (!this.color.contentEquals(other.text))
        return (false);
      return (true);
    }
  }

  public class HologramPictureData extends HologramData
  {
    public final String picturePath;

    public HologramPictureData(Location _position, String _picturePath)
    {
      super(_position);
      this.picturePath = _picturePath;
    }
  }

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
      this.newData = new PlayerRealTimeData(new HologramTextData(new Location(null, 0.0, 0.0, 0.0), "", ""),
                                            new HologramTextData(new Location(null, 0.0, 0.0, 0.0), "", ""),
                                            new HologramPictureData(new Location(null, 0.0, 0.0, 0.0), ""),
                                            "", "", -1);
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
    public final HologramTextData topText;
    public final HologramTextData bottomText;
    public final HologramPictureData picture;
    public final String soundPath;
    public final String command;
    public final int commandNumber;
    public boolean commandResponse = false;
    public String commandLogs = "";

    public PlayerRealTimeData(HologramTextData _topText,
                              HologramTextData _bottomText,
                              HologramPictureData _picture,
                              String _soundPath,
                              String _command,
                              int _serialNumber)
    {
      this.topText = _topText;
      this.bottomText = _bottomText;
      this.picture = _picture;
      this.soundPath = _soundPath;
      this.command = _command;
      this.commandNumber = _serialNumber;
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

  public String sendRequest(UUID playerUUID, String playerName, String request) throws IOException, InterruptedException
  {
    request = request.substring(0, Math.min(request.length(), 255));
    HttpClient httpClient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost(databaseServer);
    List<NameValuePair> params = new ArrayList<>(2);
    params.add(new BasicNameValuePair("storeprocedure",
                                      "minecraft_player_request"));
    params.add(new BasicNameValuePair("param1",
                                      playerUUID.toString().replace("-", "")));
    params.add(new BasicNameValuePair("param2", playerName));
    params.add(new BasicNameValuePair("param3", request));
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
    return (jsonData.get("reply").getAsString());
  }

  <T>T getJsonValue(JsonObject object, String key, T defaultValue, Function<JsonElement, T> getter)
  {
    JsonElement element = object.get(key);

    if (element == null)
    {
      MinecraftLayer.getInstance().getLogger().warning("Could not find Json Element \"" + key + "\" in server response");
      return (defaultValue);
    }
    if (element.isJsonNull())
    {
      MinecraftLayer.getInstance().getLogger().fine("Server sent null for \"" + key + "\"");
      return (defaultValue);
    }
    return (getter.apply(element));
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
    String response;

    HttpClient httpClient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost(databaseServer);
    List<NameValuePair> params = new ArrayList<>(2);
    HttpResponse httpResponse;
    HttpEntity hentity;

    params.add(new BasicNameValuePair("storeprocedure",
                                      pData.realTimeProcedureName));
    params.add(new BasicNameValuePair("param1",
                                      String.valueOf(pData.userNumber)));
    params.add(new BasicNameValuePair("param2",
                                      (pData.newData != null && !pData.newData.command.isEmpty() ? pData.newData.commandLogs : "")));
    params.add(new BasicNameValuePair("param3",
                                      String.valueOf(pData.newData != null ? pData.newData.commandNumber : -1)));

    httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    if ((this.testState & 0x40) == 0)
    {
      MinecraftLayer.getInstance().getLogger().fine("Querying the server for " +
                                                    "real time data...");

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

      response = EntityUtils.toString(hentity);

      jsonRoot = JsonParser.parseString(response);
    }
    else
    {
      this.plugin.getLogger().warning("TEST MODE ON");
      this.plugin.getLogger().info("Data to be sent to the server : " + params.toString());
      File testFile = new File(this.plugin.getDataFolder(), "testrealtimedata.json");
      jsonRoot = JsonParser.parseReader(new FileReader(testFile));
      response = "";
    }

    if (jsonRoot.isJsonArray()) // See comment in queryPlayerData
      jsonRoot = jsonRoot.getAsJsonArray().get(0);
    if (!jsonRoot.isJsonObject())
    {
      this.plugin.getLogger().warning("Retrieving real-time for " + playerUUID.toString() + " failed :");
      this.plugin.getLogger().warning("Server sent an invalid JSON object :");
      this.plugin.getLogger().warning("\"" + response + "\"");
      return (null);
    }

    JsonObject jsonData = jsonRoot.getAsJsonObject();

    Location topLocation = new Location(
            null,
            getJsonValue(jsonData, "top_text_x", 0.0, e -> e.getAsDouble()),
            getJsonValue(jsonData, "top_text_y", MinecraftLayer.getInstance().getHologramManager().topOffset + 0.25, e -> e.getAsDouble()),
            getJsonValue(jsonData, "top_text_z", 0.0, e -> e.getAsDouble())
    );
    Location bottomLocation = new Location(
            null,
            getJsonValue(jsonData, "bottom_text_x", 0.0, e -> e.getAsDouble()),
            getJsonValue(jsonData, "bottom_text_y", MinecraftLayer.getInstance().getHologramManager().bottomOffset, e -> e.getAsDouble()),
            getJsonValue(jsonData, "bottom_text_z", 0.0, e -> e.getAsDouble())
    );
    Location pictureLocation = new Location(
            null,
            getJsonValue(jsonData, "picture_url_x", topLocation.getX(), e -> e.getAsDouble()),
            getJsonValue(jsonData, "picture_url_y", topLocation.getY() + 0.75,  e -> e.getAsDouble()),
            getJsonValue(jsonData, "picture_url_z", topLocation.getZ(), e -> e.getAsDouble())
    );
    PlayerRealTimeData pRTData = new PlayerRealTimeData(
            new HologramTextData(
                    topLocation,
                    getJsonValue(jsonData, "top_text", "", e -> e.getAsString()),
                    getJsonValue(jsonData, "top_text_color", "", e -> e.getAsString())
            ),
            new HologramTextData(
                    bottomLocation,
                    getJsonValue(jsonData, "bottom_text", "", e -> e.getAsString()),
                    getJsonValue(jsonData, "bottom_text_color", "", e -> e.getAsString())
            ),
            new HologramPictureData(
                    pictureLocation,
                    getJsonValue(jsonData, "picture_url", "", e -> e.getAsString())
            ),
            getJsonValue(jsonData, "audio_url", "", e -> e.getAsString()),
            getJsonValue(jsonData, "game_command", "", e -> e.getAsString()),
            getJsonValue(jsonData, "command_serial_number", -1, e -> e.getAsInt())
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
          if (!this.playerData.newData.command.isEmpty())
          {
            List<String> finalLogList = new LinkedList<>();
            List<String> logList = new LinkedList<>();
            MinecraftLayer.getInstance().getLogger().warning(this.playerData.newData.command);
            for (String command : this.playerData.newData.command.split(
                    "\\\\\\\\"))
            {

              MinecraftLayer.getInstance()
                            .getLogger()
                            .info("Executing command \"" + command +
                                  "\" for player " + player.getName());
              this.playerData.newData.commandResponse = MinecraftLayer.getInstance()
                                                                      .executeCommand(
                                                                              command,
                                                                              player, logList);
              String combinedLogs = String.join("\n", logList);
              if (!this.playerData.newData.commandResponse)
                MinecraftLayer.getInstance()
                              .getLogger()
                              .warning("Command failed to parse : " + combinedLogs);
              finalLogList.add(combinedLogs);
              logList.clear();
            }
            this.playerData.newData.commandLogs = String.join("\\\\", finalLogList);
          }


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
