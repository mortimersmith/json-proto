package com.github.mortimersmith.jsonproto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GsonProto
{
    public static <T extends Message> T fromJson(JsonObject json, Class<T> cls) throws Error
    {
        return fromJson(json, getBuilder(cls)).as(cls);
    }

    private static Castable fromJson(JsonObject json, Message.Builder b) throws Error
    {
        for (FieldDescriptor fd : b.getDescriptorForType().getFields()) {
            System.out.println(fd.getName());
            if (fd.isRepeated()) {
                JsonElement e = json.get(fd.getName());
                if (!e.isJsonArray()) throw Error.typeMismatch();
                JsonArray a = e.getAsJsonArray();
                for (int i = 0; i < a.size(); ++i)
                    b.addRepeatedField(fd, fieldFromJson(b, fd, Indirection.of(e.getAsJsonArray(), i)));
            } else {
                Indirection i = Indirection.of(json, fd.getName());
                if (i.has())
                    b.setField(fd, fieldFromJson(b, fd, i));
            }
        }
        return Castable.of(b.build());
    }

    public static JsonObject toJson(MessageOrBuilder msg) throws Error
    {
        JsonObject json = new JsonObject();
        for (FieldDescriptor fd : msg.getAllFields().keySet()) {
            if (fd.isRepeated()) {
                JsonArray array = new JsonArray();
                for (int i = 0; i < msg.getRepeatedFieldCount(fd); ++i)
                    fieldToJson(fd, msg.getRepeatedField(fd, i), Indirection.of(array));
                json.add(fd.getName(), array);
            } else {
                if (msg.hasField(fd))
                    fieldToJson(fd, msg.getField(fd), Indirection.of(json, fd.getName()));
            }
        }
        return json;
    }

    private static Object fieldFromJson(Message.Builder b, FieldDescriptor fd, Indirection json) throws Error
    {
        switch (fd.getType()) {
            case BOOL:
                return json.getBoolean();
            case INT32:
            case FIXED32:
            case SFIXED32:
            case SINT32:
            case UINT32:
                return json.getNumber().intValue();
            case INT64:
            case FIXED64:
            case SFIXED64:
            case SINT64:
            case UINT64:
                return json.getNumber().longValue();
            case FLOAT:
                return json.getNumber().floatValue();
            case DOUBLE:
                return json.getNumber().doubleValue();
            case STRING:
                return json.getString();
            case BYTES:
                return ByteString.copyFrom(unhex(json.getString()));
            case ENUM:
                return fd.getEnumType().findValueByName(json.getString());
            case GROUP:
                break;
            case MESSAGE:
                return fromJson(json.getObject(), b.getFieldBuilder(fd)).msg();
        }
        throw Error.typeMismatch();
    }

    private static void fieldToJson(FieldDescriptor fd, Object value, Indirection json) throws Error
    {
        switch (fd.getType()) {
            case BOOL:
                json.add((Boolean)value);
                break;
            case INT32:
            case INT64:
            case FIXED32:
            case FIXED64:
            case SFIXED32:
            case SFIXED64:
            case SINT32:
            case SINT64:
            case UINT32:
            case UINT64:
                json.add(((Number)value).longValue());
                break;
            case FLOAT:
            case DOUBLE:
                json.add(((Number)value).doubleValue());
                break;
            case STRING:
                json.add((String)value);
                break;
            case BYTES:
                json.add(hex(((ByteString)value).toByteArray()));
                break;
            case ENUM:
                json.add(((EnumValueDescriptor)value).getName());
                break;
            case GROUP:
                throw Error.typeMismatch();
            case MESSAGE:
                json.add(toJson((MessageOrBuilder)value));
                break;
        }
    }

    private static Message.Builder getBuilder(Class<? extends Message> cls) throws Error
    {
        try {
            Method m = cls.getMethod("newBuilder");
            return (Message.Builder)m.invoke(m);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw Error.typeMismatch();
        }
    }

    private static final char[] HEX = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private static String hex(byte[] bytes) {
        char[] c = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; ++i, j += 2) {
            c[j] = HEX[(bytes[i] >>> 4) & 0xf];
            c[j+1] = HEX[bytes[i] & 0xf];
        }
        return new String(c);
    }

    private static byte[] unhex(String s) throws Error {
        if (s.length() % 2 != 0) throw Error.misc("hexstring must be of even length");
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0, j = 0; i < bytes.length; ++i, j += 2) {
            char cu = s.charAt(j), cl = s.charAt(j+1);
            int bu = cu >= '0' && cu <= '9' ? cu - '0' : cu >= 'a' && cu <= 'f' ? cu - 'a' + 10 : cu >= 'A' && cu <= 'F' ? cu - 'A' + 10 : -1;
            int bl = cl >= '0' && cl <= '9' ? cl - '0' : cl >= 'a' && cl <= 'f' ? cl - 'a' + 10 : cl >= 'A' && cl <= 'F' ? cl - 'A' + 10 : -1;
            if (bu < 0 || bl < 0) throw Error.misc("hexstring must only contain hex chars");
            bytes[i] = (byte)((bu << 4) | bl);
        }
        return bytes;
    }

    public static class Error extends Exception {
        private Error(String msg) { super(msg); }
        public static Error typeMismatch() { return new Error("type error"); }
        public static Error misc(String msg) { return new Error(msg); }
    }

    public static class Castable
    {
        private final Message _msg;
        private Castable(Message m) { _msg = m; }
        public static Castable of(Message m) { return new Castable(m); }
        public Message msg() { return _msg; }
        @SuppressWarnings("unchecked") <T> T as(Class<T> cls) { return (T)_msg; };
    }

    private interface Indirection
    {
        void add(Boolean value);
        void add(Character value);
        void add(Number value);
        void add(String value);
        void add(JsonElement value);

        boolean has();
        Boolean getBoolean();
        Character getCharacter();
        Number getNumber();
        String getString();
        JsonObject getObject();

        public static Indirection of(JsonObject json, String name) {
            return new IndirectObject(json, name);
        }

        public static Indirection of(JsonArray json) {
            return new IndirectArray(json);
        }

        public static Indirection of(JsonArray json, int index) {
            return new IndirectArray(json, index);
        }
    }

    private static class IndirectObject implements Indirection
    {
        private final JsonObject _json;
        private final String _name;
        public IndirectObject(JsonObject json, String name) { _json = json; _name = name; }
        @Override public void add(Boolean value) { _json.addProperty(_name, value); }
        @Override public void add(Character value) { _json.addProperty(_name, value); }
        @Override public void add(Number value) { _json.addProperty(_name, value); }
        @Override public void add(String value) { _json.addProperty(_name, value); }
        @Override public void add(JsonElement value) { _json.add(_name, value); }
        @Override public boolean has() { return _json.has(_name); }
        @Override public Boolean getBoolean() { return _json.get(_name).getAsBoolean(); }
        @Override public Character getCharacter() { return _json.get(_name).getAsCharacter(); }
        @Override public Number getNumber() { return _json.get(_name).getAsNumber(); }
        @Override public String getString() { return _json.get(_name).getAsString(); }
        @Override public JsonObject getObject() { return _json.get(_name).getAsJsonObject(); }
    }

    private static class IndirectArray implements Indirection
    {
        private final JsonArray _json;
        private final int _index;
        public IndirectArray(JsonArray json) { _json = json; _index = 0; }
        public IndirectArray(JsonArray json, int index) { _json = json; _index = index; }
        @Override public void add(Boolean value) { add(new JsonPrimitive(value)); }
        @Override public void add(Character value) { add(new JsonPrimitive(value)); }
        @Override public void add(Number value) { add(new JsonPrimitive(value)); }
        @Override public void add(String value) { add(new JsonPrimitive(value)); }
        @Override public void add(JsonElement value) { _json.add(value); }
        @Override public boolean has() { return _index < _json.size(); }
        @Override public Boolean getBoolean() { return _json.get(_index).getAsBoolean(); }
        @Override public Character getCharacter() { return _json.get(_index).getAsCharacter(); }
        @Override public Number getNumber() { return _json.get(_index).getAsNumber(); }
        @Override public String getString() { return _json.get(_index).getAsString(); }
        @Override public JsonObject getObject() { return _json.get(_index).getAsJsonObject(); }
    }
}
