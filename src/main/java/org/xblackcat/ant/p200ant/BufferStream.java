package org.xblackcat.ant.p200ant;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BufferStream extends OutputStream {
    static final int PAGE_SIZE = 4096;
    private Page head;
    private Page cur;
    private int curPos;

    BufferStream() {
        this.cur = this.head = new Page();
    }

    public void write(int value) {
        if (this.curPos == PAGE_SIZE) {
            this.newPage();
        }
        this.cur.buffer[this.curPos++] = (byte) value;
    }

    public void write(byte[] b, int off, int len) {
        while (len > 0) {
            int copyCnt = PAGE_SIZE - this.curPos;
            if (copyCnt == 0) {
                this.newPage();
                copyCnt = PAGE_SIZE;
            }
            if (copyCnt > len) {
                copyCnt = len;
            }
            System.arraycopy(b, off, this.cur.buffer, this.curPos, copyCnt);
            this.curPos += copyCnt;
            off += copyCnt;
            len -= copyCnt;
        }
    }

    InputStream getInputStream() {
        return new BufferIS(this.head, this.curPos);
    }

    private void newPage() {
        this.cur = this.cur.next = new Page();
        this.curPos = 0;
    }

    static class BufferIS
            extends InputStream {
        Page cur;
        int lastPageSize;
        int offset;

        BufferIS(Page head, int lastPageSize) {
            this.cur = head;
            this.lastPageSize = lastPageSize;
        }

        public int read() throws IOException {
            if (!this.nextPage()) {
                return -1;
            }
            return this.cur.buffer[this.offset++] & 0xFF;
        }

        public int available() throws IOException {
            if (!this.nextPage()) {
                return 0;
            }
            if (this.cur.next == null) {
                return this.lastPageSize - this.offset;
            }
            return PAGE_SIZE - this.offset;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (len <= 0) {
                return 0;
            }
            int avail = this.available();
            if (len > avail) {
                len = avail;
            }
            if (len == 0) {
                return -1;
            }
            System.arraycopy(this.cur.buffer, this.offset, b, off, len);
            this.offset += len;
            return len;
        }

        public long skip(long n) throws IOException {
            int skip = (int) Math.min(n, (long) this.available());
            if (skip > 0) {
                this.offset += skip;
                return skip;
            }
            return 0;
        }

        private boolean nextPage() {
            if (this.cur != null && (this.offset == PAGE_SIZE || this.offset == this.lastPageSize && this.cur.next == null)) {
                this.offset = 0;
                this.cur = this.cur.next;
            }
            return this.cur != null;
        }
    }

    static class Page {
        final byte[] buffer = new byte[PAGE_SIZE];
        Page next;

        Page() {
        }
    }

}
