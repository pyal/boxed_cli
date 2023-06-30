package org.boxed.cli;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Simple helpers to work with list (init, zip, etc) maps (init)
 * get resources
 * force lazy variable calculation
 */
public class General {

    public static <T> T forceClc(T forceIt) {return forceIt;}

    //List init helper
    @SafeVarargs
    public static <T> List<T> listT(T... data) {
        return Lists.newArrayList(data);
    }
    public static <T> List<T> listTOf(Integer size, T value) {
        return new ArrayList<>(Collections.nCopies(size, value));
    }
    public static <T> List<T> listTOf(Integer size) {
        return new ArrayList<>(size);
    }

    //Map init helper
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapKV(Object... data) {
        Preconditions.checkArgument(data.length % 2 == 0);
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < data.length - 1; i += 2) {
            if (data[i + 1] == null) { continue; }
            map.put((K) data[i], (V) data[i + 1]);
        }
        return map;
    }
    public static <A,B,C> List<C> zip(List<A> lst1, List<B> lst2, BiFunction<A,B,C> zipper) {
        Iterator<A> it1 = lst1.iterator();
        Iterator<B> it2 = lst2.iterator();
        List<C> res = listT();
        while (it1.hasNext() && it2.hasNext()) {
            res.add(zipper.apply(it1.next(), it2.next()));
        }
        return res;
    }
    public static <T, U> List<Pair<T, U>> zip(List<T> list1, List<U> list2) {
        return zip(list1, list2, (a,b) -> Pair.of(a,b));
    }

    //Reading resourses
    public static <T> String getResource(String resource, Class<T> klass) {
        try {
            return Resources.toString(
                    klass.getClassLoader().getResource(resource),
                    com.google.common.base.Charsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Get resource " + resource + " failed: ", e);
        }
    }
    private static <R, T> T doIfNotNull(R data, Supplier<T> supplier) {
        if (data == null)  return null;
        return supplier.get();
    }
    public static String bytes2strEncode(byte[] data) {
        return doIfNotNull(data, () -> Base64.getEncoder().encodeToString(data));
    }
    public static byte[] strEncode2bytes(String str) {
        return doIfNotNull(str, () -> Base64.getDecoder().decode(str));
    }

    public static String encodebytes2utfstr(byte[] data) {
        return doIfNotNull(data, () -> new String(data, Charset.forName("UTF8")));
    }
    public static byte[] compress(byte[] data) throws IOException {
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
                GZIPOutputStream gos = new GZIPOutputStream(bos);
        ) {
            gos.write(data, 0, data.length);
            gos.finish();
            gos.flush();
            bos.flush();
            return bos.toByteArray();
        }
    }

    public static byte[] uncompress(byte[] data) throws IOException {
        try (
                ByteArrayInputStream bos = new ByteArrayInputStream(data);
                GZIPInputStream gos = new GZIPInputStream(bos);
        ) {
            return ByteStreams.toByteArray(gos);
        }
    }

    @SuppressWarnings("unchecked")
    public static Logger LOG = LogManager.getLogger(General.class);

}
