package il.co.biosignals.minecraftlayer;

import com.example.nms.accessors.CommandSourceAccessor;
import com.example.nms.accessors.ComponentAccessor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public class CommandSourceProxy implements InvocationHandler
{
  private final Object _source;
  private final List<String> _logList;

  private static final Method sendMessageMethod = CommandSourceAccessor.getMethodSendSystemMessage1();

  public static Object newInstance(Object obj, List<String> logList) {
    Class beep[] = {CommandSourceAccessor.getType()};
    return java.lang.reflect.Proxy.newProxyInstance(
            obj.getClass().getClassLoader(),
            beep,
            new CommandSourceProxy(obj, logList));
  }

  public CommandSourceProxy(Object source, List<String> logList)
  {
    _source = source;
    _logList = logList;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args)
	  throws Throwable
  {
    Object result;

    try
    {
      if (method.equals(sendMessageMethod))
      {
        Object component = ComponentAccessor.getType().cast(args[0]);
        String string = (String) ComponentAccessor.getMethodGetString1().invoke(component);
        _logList.add(string);
      }
      result = method.invoke(_source, args);
    }
    catch (InvocationTargetException e)
    {
      throw e.getTargetException();
    }
    catch (Exception e)
    {
      throw new RuntimeException("unexpected invocation exception: " +
				 e.getMessage());
    }
    return result;
  }
}
