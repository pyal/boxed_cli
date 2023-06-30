package org.boxed.cli;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.boxed.cli.General.*;

/**
 * Add number formatter - adding to time / numbers description
 * Convert 123456678L -> 123ms 456mks for time
 * Convert 123456678L -> 123M  456K   for number
 */
public class PrettyNumberFormater {

    public static String prettyTime(Long dt_nanoseconds, int numNamedGroups) {
        List<String> name = listT("ns", "mks", "ms", "s", "m", "h", "d", "m", "y");
        List<Integer> factor = listT(1000, 1000, 1000, 60, 60, 24, 30, 12, 10000000);
        return prettyFormatter(dt_nanoseconds, numNamedGroups, zip(name, factor));
    }
    public static String prettyNumber(Long x, int numNamedGroups) {
        List<String> name = listT("", "K", "M", "G", "T", "P", "E", "Z");
        List<Integer> factor = listTOf(name.size(), 1000);
        return prettyFormatter(x, numNamedGroups, zip(name, factor));
    }

    private static String prettyFormatter(Long l, int printGroups, List<Pair<String, Integer>> units) {
        AtomicLong d = new AtomicLong(Math.abs(l));
        if(d.get() == 0) return "0";
        Function<List<Pair<String, Integer>>, List<Pair<String, Integer>>> removeLeadingZero = (List<Pair<String, Integer>> lst) -> {
            AtomicBoolean done = new AtomicBoolean(false);
            return lst.stream().filter(x-> {
                if (done.get()) return true;
                if (x.getValue() == 0) return false;
                done.set(true);
                return true;
            }).collect(Collectors.toList());
        };
        List<Pair<String, Integer>> lst = units.stream().map(x-> {
            Pair<String, Integer> ret = Pair.of(x.getKey(), ((Long)(d.get() % x.getValue())).intValue());
            d.set(d.get() / x.getValue());
            return ret;
        }).collect(Collectors.toList());
        lst = removeLeadingZero.apply(lst);
        Collections.reverse(lst);
        lst = removeLeadingZero.apply(lst);
        String str = lst.stream()
                .map(x->x.getValue().toString() + x.getKey()).limit(printGroups).collect(Collectors.joining(" "));
        if(l < 0) str = "-(" + str + ")";
        return str;
    }
}
