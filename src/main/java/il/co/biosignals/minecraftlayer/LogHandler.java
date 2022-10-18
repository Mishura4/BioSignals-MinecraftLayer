package il.co.biosignals.minecraftlayer;

/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogHandler
{
  private Player player = null;
  private StringBuilder logs = new StringBuilder();

  public LogHandler()
  {
    ((Logger)LogManager.getLogger()).addAppender(new ApacheAppender("MinecraftLayerAppender", null));
    java.util.logging.LogManager.getLogManager().getLogger("").addHandler(new JavaHandler());
  }

  class JavaHandler extends Handler
  {
    @Override
    public void publish(LogRecord record)
    {
      System.out.println("Seeing log " + record.getMessage());
      if (player == null)
        return;
      logs.append(record.getMessage());
    }

    @Override
    public void flush()
    {

    }

    @Override
    public void close() throws SecurityException
    {

    }
  }

  class ApacheAppender extends AbstractAppender
  {
    protected ApacheAppender(String name, Filter filter)
    {
      super(name, filter, null);
    }

    @Override
    public void append(LogEvent event)
    {
      System.out.println("Seeing apache log " + event.getMessage());
    }
  }

  public void attachPlayer(Player player)
  {
    this.player = player;
  }

  public void detachPlayer(Player player)
  {
    if (this.player != player)
      return;
    this.player = null;
  }

  public String popLogs()
  {
    String ret = this.logs.toString();

    this.logs.setLength(0);
    return (ret);
  }
}
*/