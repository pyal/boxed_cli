package org.boxed.cli;
import org.boxed.cli.json.Box;

import java.io.*;

import static org.boxed.cli.ExceptionHandler.rethrow;
import static org.boxed.cli.General.bytes2strEncode;

/**
 * Object serializer
 * Define general serializer for Box objects (to / from string)
 * Define general serializer for java Serializable objects (used by spark rdd serialization)
 */
public class Serializer {
    public interface CustomSerializer<T> extends Serializable {
        byte[] toBytes(T obj);

        T fromBytes(byte[] data);
    }

    public static class BoxSerializer<T extends Box> implements CustomSerializer<T> {
        @Override
        public byte[] toBytes(T obj) {
            return Box.box2Str(obj).getBytes();
        }

        @Override
        public T fromBytes(byte[] data) {
            return Box.str2Box(new String(data));
        }
    }
    public static class StdSerializer<T extends Serializable> implements CustomSerializer<T> {
        @Override
        public byte[] toBytes(T obj) {
            return Utils.object2binary(obj);
        }

        @Override
        public T fromBytes(byte[] data) {
            return Utils.binary2object(data);
        }
    }

    public static class Utils {
        public static <T extends Serializable> String object2binarystr(T x, boolean compress) {
            byte[] bytes = object2binary(x);
            byte[] encodedBytes = compress ? rethrow(() -> General.compress(bytes)) : bytes;
            return bytes2strEncode(encodedBytes);
        }

        public static <T extends Serializable> byte[] object2binary(T x) {
            return rethrow(() -> {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(byteStream);
                out.writeObject(x);
                out.close();
                return byteStream.toByteArray();
            });
        }

        public static <T extends Serializable> T binarystr2object(String rowStr, boolean compress) {
            byte[] rowByte = General.strEncode2bytes(rowStr);
            byte[] readByte = compress ? rethrow(() -> General.uncompress(rowByte)) : rowByte;
            return binary2object(readByte);
        }

        public static <T extends Serializable> T binary2object(byte[] readByte) {
            return rethrow(() -> {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(readByte);
                ObjectInputStream input = new ObjectInputStream(byteStream);
                @SuppressWarnings("unchecked")
                T obj = (T) input.readObject();
                input.close();
                return obj;
            });
        }
    }
}
