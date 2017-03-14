package org.xblackcat.ant.p200ant;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Engine {
    private static final String PACK_KEEP_FILE_ORDER = "pack.keep.file.order";
    private static final String PACK_MODIFICATION_TIME = "pack.modification.time";
    private static final String PACK_SEGMENT_LIMIT = "pack.segment.limit";
    private static final String PACK_EFFORT = "pack.effort";
    private static final String PACK_CODE_ATTRIBUTE_LOCAL_VARIABLE_TABLE = "pack.code.attribute.LocalVariableTable";

    private final Properties props = new Properties();
    private File destDir;
    private Level level;
    private Method setLevel;
    private Method getLogger;

    public Engine() {
        try {
            Class<?> lsc = Class.forName("sun.util.logging.LoggingSupport");
            setLevel = lsc.getMethod("setLevel", Object.class, Object.class);
            getLogger = lsc.getMethod("getLogger", String.class);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        this.setProperty(Engine.PACK_KEEP_FILE_ORDER, "false");
        this.setProperty(Engine.PACK_MODIFICATION_TIME, "latest");
        this.setProperty(Engine.PACK_EFFORT, "9");
        this.setProperty(Engine.PACK_CODE_ATTRIBUTE_LOCAL_VARIABLE_TABLE, "strip");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void loadProperties(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            this.props.load(fis);
        }
    }

    public void setKeepOrder(boolean enabled) {
        this.setProperty(Engine.PACK_KEEP_FILE_ORDER, enabled ? "true" : "false");
    }

    public void setKeepModificationTime(boolean enabled) {
        this.setProperty(Engine.PACK_MODIFICATION_TIME, enabled ? "keep" : "latest");
    }

    public void setSingleSegment(boolean enabled) {
        if (enabled) {
            this.setProperty(Engine.PACK_SEGMENT_LIMIT, "-1");
        } else {
            this.props.remove(Engine.PACK_SEGMENT_LIMIT);
        }
    }

    public void setSegmentLimit(int size) {
        this.setProperty(Engine.PACK_SEGMENT_LIMIT, Integer.toString(size));
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    private void setProperty(String key, String value) {
        this.props.put(key, value);
    }

    public void setDestDir(File dir) {
        if (dir != null && !dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + dir);
        }
        this.destDir = dir;
    }

    private void copyTo(Map<String, String> target) {
        if (getLogger != null && setLevel != null) {
            // Try to set logging level
            try {
                Object logger = getLogger.invoke(null, "java.util.jar.Pack200");
                setLevel.invoke(null, logger, level);
            } catch (ReflectiveOperationException e) {
                // ignore:
            }
        }
        for (Map.Entry e : this.props.entrySet()) {
            if (e.getKey().getClass() != String.class || e.getValue().getClass() != String.class) {
                continue;
            }
            target.put((String) e.getKey(), (String) e.getValue());
        }
    }

    private Pack200.Packer createPacker() {
        Pack200.Packer p = Pack200.newPacker();
        this.copyTo(p.properties());
        return p;
    }

    private Pack200.Unpacker createUnpacker() {
        Pack200.Unpacker p = Pack200.newUnpacker();
        this.copyTo(p.properties());
        return p;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void repack(File jarFile) throws IOException {
        try (BufferStream bs = new BufferStream()) {
            try (JarFile file = new JarFile(jarFile)) {
                this.createPacker().pack(file, bs);
                try (FileOutputStream fos = new FileOutputStream(jarFile)) {
                    try (JarOutputStream jos = new JarOutputStream(fos)) {
                        Pack200.Unpacker unpacker = this.createUnpacker();
                        unpacker.unpack(bs.getInputStream(), jos);
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void pack(File jarFile, boolean createPACK, boolean createGZ) throws IOException {
        File dir = this.destDir != null ? this.destDir : jarFile.getParentFile();
        String jarFileName = jarFile.getName();
        String fileName = jarFileName.toLowerCase(Locale.ROOT).endsWith(".jar") ?
                jarFileName.substring(0, jarFileName.length() - 4) :
                jarFileName;
        String packName = fileName.concat(".pack");
        try (TeeOutputStream tee = new TeeOutputStream()) {
            if (createPACK) {
                tee.addSink(new FileOutputStream(new File(dir, packName)));
            }
            if (createGZ) {
                tee.addSink(new GZIPOutputStream(new FileOutputStream(new File(dir, packName.concat(".gz")))));
            }

            try (BufferedOutputStream bos = new BufferedOutputStream(tee)) {
                try (JarFile file = new JarFile(jarFile)) {
                    this.createPacker().pack(file, bos);
                    bos.flush();
                }
            }
        }
    }
}
