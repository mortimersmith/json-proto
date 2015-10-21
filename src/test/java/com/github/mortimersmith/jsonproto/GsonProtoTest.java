package com.github.mortimersmith.jsonproto;

import com.github.mortimersmith.jsonproto.protos.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static org.junit.Assert.assertEquals;

public class GsonProtoTest
{
    @org.junit.Test
    public void toJson() throws Exception
    {
        JsonObject json = GsonProto.toJson(
            Test.A
                .newBuilder()
                .setA(Test.B.newBuilder().setA(1))
                .setB(true)
                .setC(1)
                .setD(1)
                .setE(1)
                .setF(1)
                .setG(1)
                .setH(1)
                .setI(1)
                .setJ(1)
                .setK(1)
                .setL(1)
                .setM("one"));

        assertEquals(1, json.get("a").getAsJsonObject().get("a").getAsInt());
        assertEquals(true, json.get("b").getAsBoolean());
        assertEquals(1, json.get("c").getAsInt());
        assertEquals(1, json.get("d").getAsInt());
        assertEquals(1, json.get("e").getAsInt());
        assertEquals(1, json.get("f").getAsInt());
        assertEquals(1, json.get("g").getAsInt());
        assertEquals(1, json.get("h").getAsLong());
        assertEquals(1, json.get("i").getAsLong());
        assertEquals(1, json.get("j").getAsLong());
        assertEquals(1, json.get("k").getAsLong());
        assertEquals(1, json.get("l").getAsLong());
        assertEquals("one", json.get("m").getAsString());
    }

    @org.junit.Test
    public void fromJson() throws Exception
    {
        JsonObject json = new JsonParser().parse("{ 'a' : { 'a' : 1 }, 'b' : true, 'c' : 1, 'd' : 1, 'e' : 1, 'f' : 1, 'g' : 1, 'h' : 1, 'i' : 1, 'j' : 1, 'k' : 1, 'l' : 1, 'm' : 'one' }").getAsJsonObject();
        Test.A msg = GsonProto.fromJson(json, Test.A.class);

        assertEquals(1, msg.getA().getA());
        assertEquals(true, msg.getB());
        assertEquals(1, msg.getC());
        assertEquals(1, msg.getD());
        assertEquals(1, msg.getE());
        assertEquals(1, msg.getF());
        assertEquals(1, msg.getG());
        assertEquals(1, msg.getH());
        assertEquals(1, msg.getI());
        assertEquals(1, msg.getJ());
        assertEquals(1, msg.getK());
        assertEquals(1, msg.getL());
        assertEquals("one", msg.getM());
    }
}
