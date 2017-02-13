package org.xblackcat.ant.p200ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            List<File> files = new ArrayList<>();
            boolean repack = false;
            boolean createPACK = false;
            boolean createGZ = true;
            Engine e = new Engine();
            int i = 0;
            while (i < args.length) {
                String arg;
                if ("--repack".equals(arg = args[i++])) {
                    repack = true;
                    continue;
                }
                if ("--gzip".equals(arg)) {
                    createGZ = true;
                    continue;
                }
                if ("--no-gzip".equals(arg)) {
                    createGZ = false;
                    continue;
                }
                if ("--pack".equals(arg)) {
                    createPACK = true;
                    continue;
                }
                if ("--no-pack".equals(arg)) {
                    createPACK = false;
                    continue;
                }
                if ("--config".equals(arg)) {
                    Main.needsArg(args, i);
                    e.loadProperties(new File(args[i++]));
                    continue;
                }
                if ("--keep-order".equals(arg)) {
                    e.setKeepOrder(true);
                    continue;
                }
                if ("--keep-modification-time".equals(arg)) {
                    e.setKeepModificationTime(true);
                    continue;
                }
                if ("--single-segment".equals(arg)) {
                    e.setSingleSegment(true);
                    continue;
                }
                if ("--segment-limit".equals(arg)) {
                    Main.needsArg(args, i);
                    e.setSegmentLimit(Integer.parseInt(args[i++]));
                    continue;
                }
                if (arg.startsWith("--")) {
                    Main.help();
                    continue;
                }
                File f = new File(arg);
                if (!f.canRead() || !f.isFile()) {
                    System.err.println("File does not exist or can't be read: " + arg);
                    System.exit(1);
                }
                files.add(f);
            }
            if (files.size() == 0) {
                Main.help();
            }
            for (File f : files) {
                if (repack) {
                    e.repack(f);
                    continue;
                }
                e.pack(f, createPACK, createGZ);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void needsArg(String[] args, int i) {
        if (i == args.length) {
            System.err.println(args[i - 1] + " needs a parameter");
            System.exit(1);
        }
    }

    private static void help() {
        System.out.println("Usage: java -jar P200Ant [<option> ...] <file> [<file> ... ]");
        System.out.println("Possible options:");
        System.out.println("  --repack                  do repacking");
        System.out.println("  --pack                    write a .pack file");
        System.out.println("  --no-pack                 don't write a .pack file (default)");
        System.out.println("  --gzip                    write a .pack.gz file (default)");
        System.out.println("  --no-gzip                 don't write a .pack.gz file");
        System.out.println("  --config config.file      read properties from config.file");
        System.out.println("  --keep-order              keep file order");
        System.out.println("  --keep-modification-time  keep class modification time");
        System.out.println("  --single-segment          create only one big segment");
        System.out.println("  --segment-limit nnn       sets the segment limit to nnn bytes");
        System.exit(1);
    }
}
