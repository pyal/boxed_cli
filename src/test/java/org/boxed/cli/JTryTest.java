package org.boxed.cli;

import org.boxed.cli.json.JsonTools;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.boxed.cli.General.forceClc;
import static org.boxed.cli.General.mapKV;

public class JTryTest extends BaseTest {

    @Test
    public void jtryTest() {
        JTry<Integer> excRes = JTry.of(() -> {
            throw new ArrayIndexOutOfBoundsException("BAD");
        });
        JTry<Integer> nullRes = JTry.of(() -> null);
        JTry<Integer> okRes = JTry.of(() -> 1);
        Assert.assertTrue(okRes.isOk());
        Assert.assertTrue(nullRes.isOk());
        Assert.assertTrue(!excRes.isOk());
        Assert.assertTrue(nullRes.getOrThrow() == null);
        Assert.assertEquals(1L, okRes.getOrThrow().longValue());
        testMethodFailure(() -> excRes.getOrThrow(), "Have to throw exception");
        Assert.assertEquals(5L, excRes.recover(e -> 5).getOrThrow().longValue());
        Assert.assertEquals(null, nullRes.recover(e -> 5).getOrThrow());
        Assert.assertEquals(6L, nullRes.map(r -> 6).getOrThrow().longValue());

        Assert.assertEquals(5L, excRes.getOrElse(5).longValue());
        Assert.assertEquals(1L, okRes.getOrElse(10).longValue());
        Map<String, Object> m2 = mapKV("m", mapKV("a", "str"), "root", "4");
        JTry a = JTry.of(() -> forceClc(JsonTools.<String>getProperty(m2, "root")));
        Assert.assertTrue(a.isOk());
            //Enforce variable calculation
        JTry b = JTry.of(() -> forceClc(JsonTools.<Integer>getProperty(m2, "root")));
        Assert.assertTrue(!b.isOk());
            //Variable is not calculated here!!!
        JTry c = JTry.of(() -> {
            JsonTools.<Integer>getProperty(m2, "root");
        });
            //Variable is  calculated here!!!
        JTry cccc = JTry.of(() -> JsonTools.<Integer>getProperty(m2, "root"));
        Assert.assertTrue(!cccc.isOk());
        Assert.assertTrue(JsonTools.<String>getProperty(m2, "root1") == null);
        Assert.assertTrue(JsonTools.<Integer>getProperty(m2, "root1") == null);
    }
}