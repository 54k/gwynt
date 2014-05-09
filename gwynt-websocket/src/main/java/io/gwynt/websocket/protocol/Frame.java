package io.gwynt.websocket.protocol;

public class Frame {

    protected int opcode;
    protected boolean fin;
    protected byte[] payload;

    public Frame() {
    }

    public Frame(int opcode, boolean fin, byte[] payload) {
        this.opcode = opcode;
        this.fin = fin;
        this.payload = payload;
    }

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public boolean isFin() {
        return fin;
    }

    public void setFin(boolean fin) {
        this.fin = fin;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}
