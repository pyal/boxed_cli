package org.boxed.cli;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.boxed.cli.PrettyNumberFormater.prettyNumber;
import static org.boxed.cli.PrettyNumberFormater.prettyTime;

public class PrettyNumberFormaterTest extends BaseTest {

    @Test
    public void formatterTest() {
        ArrayList<Long> x = Lists.newArrayList(123456678L, 0L, 100000000L);
        ArrayList<String> timeArr = Lists.newArrayList("123ms 456mks:0:100ms".split(":"));
        ArrayList<String> countArr = Lists.newArrayList("123M 456K:0:100M".split(":"));

        IntStream.range(0, x.size()).forEach(i->{
            Assert.assertEquals(timeArr.get(i), prettyTime(x.get(i), 2));
            Assert.assertEquals(countArr.get(i), prettyNumber(x.get(i), 2));
        });
    }
}