package org.xblackcat.ant.p200ant;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class TeeOutputStream
        extends OutputStream {
    private final List<OutputStream> sinks = new ArrayList<>();

    public void addSink(OutputStream sink) {
        this.sinks.add(sink);
    }

    public void write(int b) throws IOException {
        for (OutputStream sink : this.sinks) {
            sink.write(b);
        }
    }

    public void close() {
        for (OutputStream sink : this.sinks) {
            try {
                sink.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void flush() throws IOException {
        for (OutputStream sink : this.sinks) {
            sink.flush();
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (OutputStream sink : this.sinks) {
            sink.write(b, off, len);
        }
    }
}
