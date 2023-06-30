package org.boxed.cli.json;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.boxed.cli.BaseTest;
import org.junit.Test;

import java.util.Map;

import static org.boxed.cli.json.JsonTools.getProperty;
import static org.boxed.cli.General.mapKV;
import static org.boxed.cli.SupLog.setDebugTest;
import static org.boxed.cli.JsonCvtScala.obj2StrScala;
import static org.boxed.cli.json.JsonTools.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonToolsTest extends BaseTest {

    @Override
    public void before() {
            setDebugTest("INFO");
            Configurator.setAllLevels("org.boxed.cli.json", Level.INFO);

    }

     static class Data {
        public Map<Integer, Integer> obj = mapKV(10,20,20,30);
        public String objStrSimple = "{\"10\":20,\"20\":30}";
        public String objStrPretty = """
                {
                  "10" : 20,
                  "20" : 30
                }""";

    }
    @Test
    public void testStr2Obj() {
        Data d = new Data();
        Map<Integer, Integer> o = str2Obj(d.objStrSimple);
        assertEquals(obj2Str(d.obj), obj2Str(o));
        Map<Integer, Integer> o1 = str2Obj(d.objStrPretty);
        assertEquals(obj2Str(d.obj), obj2Str(o1));
    }

    @Test
    public void testObj2StrPretty() {
        Data d = new Data();
        LOG.debug(obj2Str(d.obj));
        assertEquals(d.objStrSimple, obj2Str(d.obj));
        LOG.debug(obj2StrPretty(d.obj));
        assertEquals(d.objStrPretty, obj2StrPretty(d.obj));
        LOG.debug(obj2StrScala(d.obj));
        assertEquals(d.objStrSimple, obj2StrScala(d.obj));
    }

    @Test
    public void testLogCvt() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(this.getClass().getClassLoader(), false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("org.boxed.cli.json");
        LOG.info("Class config: " + obj2StrAny(loggerConfig));
        config.getLoggers().entrySet().stream().forEach((y) ->
                LOG.info("Logger name " + y.getKey() + " -> " + JsonTools.<Map<String, Object>>str2Obj(obj2StrCustom(y.getValue())).get("name"))
        );

    }

    public static class TestStruct {
        public String x = "x", y = "y";
    }
    @Test
    public void testTestStr2Obj() {
        Data d = new Data();
        d.obj.put(100, -100);
        String dStr = obj2Str(d);
        LOG.debug("Got object\n" + obj2StrPretty(dStr));
        Map<String, Object> t = str2Obj(dStr);
        LOG.debug("Got object\n" + obj2StrPretty(t));
        assertTrue(dStr.length() > 20);
        assertTrue(t.size() == 3);
        Map<String, Object> obj = getProperty(t, "obj");
        assertTrue(obj.size() == 3);

    }



}