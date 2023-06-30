package org.boxed.cli.json;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.boxed.cli.ExceptionHandler;
import org.boxed.cli.JTry;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.boxed.cli.ExceptionHandler.exceptionToString;
import static org.boxed.cli.ExceptionHandler.rethrow;


/**
 * Class devoted to converting objects to json strings and json strings to Map<String, Object>
 * Under the hood is jackson library
 * Defined @setProperty @getProperty - helper functions to work with Maps<String, Object>
 * to extract nested structures
 */
public class JsonTools {
  public static Logger LOG = LogManager.getLogger(JsonTools.class);

  /**
   * Convert object to string using jackson library
   * @param obj src object
   * @return json formatted string with object fields
   */
  public static String obj2Str(Object obj) {
    return JTry.of(() -> OBJECT_WRITER.writeValueAsString(obj))
            .recover(e -> {
              LOG.trace("Failed obj2str", e);
              rethrow(e);
            }).getOrThrow();
  }

  /**
   * Build Map<String, Object> object from json string using jackson library
   * Use getProperty to extract internal nested fields
   * @param data Json string
   * @param <T>  Object type to build
   * @return  Generated object
   */
  //Get object from string
  public static <T> T str2Obj(String data) {
    return JTry.<T>of(() -> OBJECT_READER.readValue(data)).getOrThrow();
  }

  /** Similar to @obj2Str - with pretty format of json string
   * @param obj src object
   * @return json formatted string with object fields
   */
  public static String obj2StrPretty(Object obj) {
    if (obj == null) return "null";
    return obj2StrPrettyInt(obj2Str(obj));
  }

  /** Similar to @obj2Str - working for any classes (str2obj fails for some object types)
   * @param o src object
   * @return json formatted string with object fields
   */
  public static String obj2StrAny(Object o) {
    return JTry.of(() -> obj2StrPretty(o)).recover(e -> {
      String s = o == null ? "null" : o.toString();
      LOG.debug("For object " + s + " failed std jackson\n" + exceptionToString(e, 100));
      return obj2StrCustom(o);
    }).getOrThrow();
  }

  /** Similar to @obj2Str - working for any classes (str2obj fails for some object types)
   * @param o src object
   * @return json formatted string with object fields
   */
  public static String obj2StrCustom(Object o) { return obj2StrCustom(o, true, false);}

  /**
   * Similar to @obj2Str - working for any classes (str2obj fails for some object types)
   * Tune some params during this generation
   * @param o src object
   * @param hideUpperCaseVar - hide caps lock vars (LOG... etc)
   * @param nestedLook       - not to decode nested structures (sometimes it hits circles)
   * @return json formatted string with object fields
   */
  public static String obj2StrCustom(Object o, Boolean hideUpperCaseVar, Boolean nestedLook) {
    Map<String, Object> m = null;
    try {
      m = getObjectFields(o, nestedLook, hideUpperCaseVar, 2);
    } catch (Throwable e) {
      m = getObjectFields(o, false, hideUpperCaseVar, 0);
    }
    return obj2StrPretty(m);
  }

  private static String obj2StrPrettyInt(String jsonString) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
              mapper.readValue(jsonString, Object.class));
    } catch (Exception ex) {
      if(jsonString == null) return null;
      return "not a json string: " + jsonString;
    }
  }


  private static List<Field> getAllClassFields(List<Field> fields, Class<?> type) {
    fields.addAll(Arrays.asList(type.getDeclaredFields()));
    if (type.getSuperclass() != null) {
      getAllClassFields(fields, type.getSuperclass());
    }
    return fields;
  }
  private static Map<String, Object> getObjectFields(Object o, Boolean nestedLook,
                                                     Boolean hideUpperCaseVar, Integer maxLevel) {
    Boolean nextLook = (maxLevel <= 0) ? false : nestedLook;
    List<Field> objectField = getAllClassFields(Lists.newArrayList(), o.getClass());
    Map<String, Object> result = Maps.newHashMap();
    Sets.newHashSet(objectField).stream()
            .filter(f -> {
              String s = JTry.of(() -> f.getName()).getOrElse("bad");
              return !(hideUpperCaseVar && s.toUpperCase().equals(s));
            })
            .forEach(x -> {
              String name = x.getName();
              if (!result.containsKey(name)) {
                JTry.of(() -> {
                  x.setAccessible(true);
                  Object val = null;
                  val = x.get(o);
                  Object res = null;
                  try {
                    obj2StrPretty(val);
                    res = val;
                  } catch (Exception e) {
                    if (val != o && val != null && nextLook) {
                      res = getObjectFields(val, nestedLook, hideUpperCaseVar, maxLevel - 1);
                    } else {
                      res = val.toString();
                    }
                  }
                  result.put(name, res);
                }).recover((Throwable e) -> {
                  result.put(name, "got exception:\n" + ExceptionHandler.exceptionToString(e, 100));
                }).getOrThrow();
              }
            });
    return  result;
  }

  private static final ObjectWriter OBJECT_WRITER = new ObjectMapper()
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).writer();
  private static final ObjectReader OBJECT_READER = new ObjectMapper()
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).readerFor(
                  new TypeReference<Map<String, Object>>() {});


  /**
   * @param obj input of class Map<String, Object>
   * @param value to set given value to the field
   * @param path  path to the field aka root, child1, child2, field
   */
  public static void setProperty(Object obj, Object value, String... path) {
    Map<String, Object> base = (Map<String, Object>) obj;
    for (int i = 0; i < path.length - 1; ++i) {
      base = (Map<String, Object>) base.get(path[i]);
    }
    base.put(path[path.length - 1], value);
  }

  /**
   * @param obj  input of class Map<String, Object>
   * @param path path to the field aka root, child1, child2, field
   * @param <T>  to cast extracted value to the type
   * @return     extracted value from the object
   */
  @SuppressWarnings("unchecked")
  public static <T> T getProperty(Object obj, String... path) {
    Map<String, Object> base = (Map<String, Object>) obj;
    for (int i = 0; i < path.length - 1; ++i) {
      if (base == null) return null;
      base = (Map<String, Object>) base.get(path[i]);
    }
    if (base == null) return null;
    Map<String, Object> b = base;
    return JTry.of(() -> (T) b.get(path[path.length - 1])).recover(e -> {
      LOG.error("Failed parsing object: " + obj2StrPretty(obj) + " path: " +
              Lists.newArrayList(path).stream().collect(Collectors.joining(", ")), e);
      rethrow(e);
    }).getOrThrow();
  }

  /**
   * Specializing @getProperty - for Long type, using template @getProperty for long generates errors
   */
  public static Long getPropertyLong(Object obj, String... path) {
    Number num = JTry.of(() ->
            JTry.of(() -> JsonTools.<Number>getProperty(obj, path))
                    .getOrSet(() -> Long.valueOf(getProperty(obj, path)))
    ).recover(e -> {
      LOG.debug("Failed extracting num from object: " + obj2StrPretty(obj) +
              " path" + " " + Lists.newArrayList(path).stream().collect(Collectors.joining(
              ".")), e);
      return null;
    }).getOrThrow();
    return num == null ? null : num.longValue();
  }

  /**
   * Specializing @getProperty - for Boolean type, using template @getProperty for Boolean generates errors
   */
  public static Boolean getPropertyBoolean(Object obj, String... path) {
    return JTry.of(() ->
            JTry.of(() -> JsonTools.<Boolean>getProperty(obj, path))
                    .getOrSet(() -> {
                      Boolean ret = JsonTools.<String>getProperty(obj, path).equalsIgnoreCase(
                              "true");
                      return ret;
                    })
    ).recover(e -> {
      LOG.debug("Failed extracting Boolean from object: " + obj2StrPretty(obj) + " path"
                      + " " + Lists.newArrayList(path).stream().collect(Collectors.joining(".")),
              e);
      return null;
    }).getOrThrow();
  }

}