package org.boxed.cli.json;

import com.google.gson.annotations.Expose;
import org.apache.commons.lang3.tuple.Pair;
import org.boxed.cli.BaseTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.boxed.cli.General.listT;
import static org.boxed.cli.json.Box.classNameField;
import static org.boxed.cli.json.Box.sdPatternDecode;
import static org.junit.Assert.assertEquals;

public class BoxTest extends BaseTest {


    public static class Cfg extends Box {
        @Expose
        Integer B = 15;
        @Expose
        Integer A = 10;
        Integer b = 20;
    }
    @Test
    public void strCfgTest() {
        String str = "{A:20," + Box.typeField(Cfg.class) + "}";
        String strCmp = "{\"B\":15,\"A\":20,\"" + classNameField + "\":\"Cfg\"}";

        Box.forceClassRegistration(Cfg.class);
        Cfg cfg = Box.str2Box(str);
        LOG.info(Box.box2Str(cfg));
        assertEquals(strCmp, Box.box2Str(cfg));
    }
    @Test
    public void testEncode() {
        List<Pair<String, String>> testCase = listT(
                Pair.of("", ""),
                Pair.of("{sdf,sdf}", "{sdf,sdf}"),
                Pair.of("{s1f:S1{{aa:bb,b:c}}S1}", "{s1f:'{aa:bb,b:c}'}"),
                Pair.of("{s1f:S1{{aa:S2{wes%:,}S2,b:c}}S1}", "{s1f:'{aa:S2{wes%:,}S2,b:c}'}"),
                Pair.of("{s1f:S1{{aa:D2{wes%:,}D2,b:c}}S1}", "{s1f:'{aa:D2{wes%:,}D2,b:c}'}"),
                Pair.of("{s1f:D1{{aa:S1{wes%:,}S1,b:c}}D1}", "{s1f:\"{aa:S1{wes%:,}S1,b:c}\"}")
        );
        testCase.stream().forEach(x -> {
            String res = sdPatternDecode(x.getKey());
            LOG.info("Encode: |" + x.getKey() + "| res: |" + res  + "|");
            Assert.assertEquals(x.getValue(), res);
        });
    }

}