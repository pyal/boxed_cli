package org.boxed.cli.json;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.boxed.cli.JTry;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.boxed.cli.General.listT;

/**
 * <pre>
 *   Base for serializable classes
 *   Annotate serializable fields with @Expose -
 *   You will be able to write / read classes as an unknown object,
 *   overwriting default values with given in json string ones
 *   Gson - will do all the hard job of saving / restoring annotated fields in json string
 *   Added functionality (in Box class - it have to be root) - to save className -
 *   to be able to restore unknown type objects
 *
 *
 *   Add public constructor to implementations to build class from string
 *   Empty constructor is not required now (java/gson upgrade?)... still there is dangerous
 *   Drawback - if empty constructor will not  be called,
 *   All variables will be null and not given their init values!!!!
 *
 *   To build class by its simple name - will try all possible prefix package names
 *   Trying to guess subsclass package names
 *   If fails, add new packet name to CLASS_PATHS
 *   </pre>
 */
public class Box {
  @Expose
  String TYPE = this.getClass().getSimpleName();
  public Box(){}

  public String help() {
    return "no help";
  }

  /**
   * Ugly way to resolve class for given className without heavy path.
   */
  private static Set<String> CLASS_PATHS = Lists.newArrayList(
      "", // To build class by full name
      "org.boxed.cli.json"
  ).stream().collect(Collectors.toSet());

  // Register basic interface classes to build
  public static <T> void registerInterfaceClass(Class<T> clazz) {
    getGsonBuilder().registerTypeAdapter(clazz, new InterfaceAdapter<T>());
    String name = clazz.getName() + "$";
    String shortName = getSimpleName(clazz, false).getOrElse("");
    //To build class by its simple name - will try all possible prefix package names
    //Trying to guess subsclass package names
    LOG.debug("Registering full class name: |" + name + "| and short: |" + shortName + "|");
    CLASS_PATHS.add(name);
    CLASS_PATHS.add(shortName);
  }

  /**
   * Building Box object from json string
   * @param str input json string with class definition (see tests)
   * @param <T> object type we want to build (can be root Box ...)
   * @return  build object
   */
  public static <T> T str2Box(String str) {
    return str2Box(str,true);
  }

  /**
   * Building Box object from json string if object type is known, we want to enforce type, skipping class resolution
   * @param str input json string with class definition (see tests)
   * @param classOf - object type to build
   * @param <T> object type we want to build (can be root Box ...)
   * @return  build object
   */
  public static <T> T str2Box(String str, Class<T> classOf) {
    return str2Box(str, classOf, true);
  }

  /**
   * @param str
   * @param <T>
   * @return
   */
  /**
   * Building Box object from json string
   * @param str input json string with class definition (see tests)
   * @param logCreation - print generated object in logs
   * @param <T> object type we want to build (can be root Box ...)
   * @return  build object
   */
  @SuppressWarnings("unchecked")
  public static <T> T str2Box(String str, Boolean logCreation) {
    Box ret = str2Box(str, Box.class, logCreation);
    return (T)ret;
  }

  /**
   * Building Box object from json string if object type is known, we want to enforce type, skipping class resolution
   * @param str input json string with class definition (see tests)
   * @param logCreation - print generated object in logs
   * @param classOf - object type to build
   * @param <T> object type we want to build (can be root Box ...)
   * @return  build object
   */
  public static <T> T str2Box(String str, Class<T> classOf, Boolean logCreation) {
    T ret = JTry.of(() ->
      getGsonBuilder().create().fromJson(sdPatternDecode(str), classOf)
    ).recover((Throwable e) -> {
      throw new RuntimeException("Failed parsing json: <" + str + "> exception:\n", e);
    }).getOrThrow();
    T rett = ret;
    if (logCreation) LOG.info(() -> "Created object: " + box2Str(rett));
    LOG.trace(() -> "Building object from string " + str, new RuntimeException("from string debug stack"));
    return rett;
  }

  /**
   * Saving box child to json string
   * @param obj Box child object
   * @param <T> Type of object
   * @return json string which can be used to restore object
   */
  public static <T> String box2Str(T obj) {
    if(obj == null) return "null";
    return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(obj);
  }
  public static <T> String toString(T obj) {return box2Str(obj);}

  /**
   * Before reading object from string, have to register class (or base class) -
   * to save its path to the list of possible class pathes
   * @param clazz class to register
   * @param <T> boxed class to register
   */
  public static <T> void forceClassRegistration(final Class<T> clazz) {
    CLASS_PATHS.add(getSimpleName(clazz, true).getOrElse(""));
    LOG.trace(JTry.of(() -> Class.forName(clazz.getName()))
            .recover((Throwable e) -> e.toString()).getOrThrow());
  }

  /**
   * Splitting long class name to short name and path. Taking into account that delimiter can be dot, or dollar
   * @param clazz  Class type to work
   * @param getPath return path to class name of short name
   * @param <T>  Class to analyse
   * @return  path or short name
   */
  public static <T> JTry<String> getSimpleName(final Class<T>  clazz, Boolean getPath) {
    return JTry.of(() ->  {
      String name = clazz.getName();
      LOG.trace(name);
      Integer dollarId = name.lastIndexOf('$');
      Integer dotId = name.lastIndexOf('.');
      Integer lastChar = (dollarId != -1) ? dollarId : dotId;
      if (getPath) {
        String path = name.substring(0, lastChar + 1);
        LOG.debug("From name " + name + " got path: " + path);
        return path;
      } else {
        String shortName = name.substring(lastChar + 1);
        LOG.debug("From name " + name + " got short class: " + shortName);
        return shortName;
      }
    });

  }

  private static final Logger LOG = LogManager.getLogger(Box.class);

  private static GsonBuilder stringConfigurationGsonBuilder;
  private static GsonBuilder getGsonBuilder() {
    if (stringConfigurationGsonBuilder == null) {
      stringConfigurationGsonBuilder = new GsonBuilder();
      stringConfigurationGsonBuilder.excludeFieldsWithoutExposeAnnotation();
      stringConfigurationGsonBuilder.registerTypeAdapter(Box.class, new InterfaceAdapter<Box>());
    }
    return stringConfigurationGsonBuilder;
  }

  /**
   * Field present in all Box objects - describing saved object type
   */
//  @@VisibleForTesting
  public static final String classNameField = "TYPE";

  /**
   * Building field with  short class name - used in tests
   * @param clazz  Class type to generate field
   * @return field with  short class name -
   */
  public static final String typeField(final Class<?> clazz) {
    return classNameField + ":" + getSimpleName(clazz, false).getOrThrow();
  };

  /**
   * Adapter for gson to properly create / save boxed objects
   */
  public static  class InterfaceAdapter<T> implements JsonDeserializer<T> {

    public T deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext jsonDeserializationContext) {
      JsonPrimitive tName = (JsonPrimitive) jsonElement.getAsJsonObject().get(classNameField);
      Class klass = buildClass(tName.getAsString());
      T result = jsonDeserializationContext.deserialize(jsonElement, klass);
      return result;
    }

    private static Class buildClass(String className) {
      Exception lastException = null;
      for (String prefix: CLASS_PATHS) {
        try {
          Class klass = Class.forName(prefix + className);
          return klass;
        } catch (Exception e) {
          lastException = e;
        }
      }
      throw new RuntimeException("Failed building class from: " + className, lastException);
    }
  }

  public static String buildHelp(List<String> classNames) {
    String canBuild = classNames.stream().map(name -> {
      Box obj = Box.str2Box("{" + classNameField + ":" + name + "}");
        return Box.toString(obj);
    }).collect(Collectors.joining("\n"));
    String details = classNames.stream().map(name -> {
      Box obj = Box.str2Box("{" + classNameField + ":" + name + "}");
      return "===============\n" + Box.toString(obj) + "\n" + obj.help();
    }).collect(Collectors.joining("\n"));
    return "Objects present: \n" + canBuild + "\n===============\n\nDetailed help \n" + details;
  }

  public static <T> String buildHelp(Class<T> pClazz) {
    List<Class> classes = loadAllSubClasses(pClazz);
    return buildHelp(classes.stream().map(x -> x.getCanonicalName()).filter(x -> x != null).collect(
        Collectors.toList()));
  }
  //Not working under spark
  public static <T> List<Class> loadAllSubClasses(Class<T> pClazz) {
    ClassLoader classLoader = pClazz.getClassLoader();
    assert classLoader != null;
    String packageName = pClazz.getPackage().getName();
    String dirPath = packageName.replace(".", "/");
    Enumeration<URL> srcList =
        JTry.of(() -> classLoader.getResources(dirPath)).getOrElse(null);
    if (srcList == null) return listT();

    List<Class> subClassList = new ArrayList<>();
    while (srcList.hasMoreElements()) {
      File dirFile = new File(srcList.nextElement().getFile());
      File[] files = dirFile.listFiles();
      if (files != null) {
        for (File file : files) {
          String subClassName = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
          if (! subClassName.equals(pClazz.getName())) {
            JTry.of(() -> {
              Class<?> x = Class.forName(subClassName);
              if (pClazz.cast(x.getConstructor().newInstance()) != null) {
                subClassList.add(x);
              }
              return null;
            });
          }
        }
      }
    }
    return subClassList;
  }
  public static String sdPatternDecode(String str) {
    Pattern open = Pattern.compile("([SD])(\\d)\\{");
    Pattern close = Pattern.compile("\\}([SD])(\\d)");
    int lastIndex = 0;
    StringBuilder output = new StringBuilder();
    Matcher openMatch = open.matcher(str);
    Matcher closeMatch = close.matcher(str);
    while (openMatch.find()) {
      if (openMatch.start() < lastIndex) continue;
      output.append(str, lastIndex, openMatch.start());
      String quote = "\"";
      if (openMatch.group(1).equals("S")) quote = "'";
      output.append(quote);
      lastIndex = openMatch.end();
      while(true) {
        if  (!closeMatch.find()) throw new RuntimeException(
            "Failed matching input pattern. String " + str + " openPattern: " +
                openMatch.group(1) + openMatch.group(2) + "{  position: " + lastIndex);
        if (closeMatch.start() > lastIndex) {
          if (openMatch.group(1).equals(closeMatch.group(1)) &&
              openMatch.group(2).equals(closeMatch.group(2))) break;
        }
      }
      output.append(str, lastIndex, closeMatch.start()).append(quote);
      lastIndex = closeMatch.end();
    }

    if (lastIndex < str.length()) {
      output.append(str, lastIndex, str.length());
    }
    return output.toString();
  }

}

