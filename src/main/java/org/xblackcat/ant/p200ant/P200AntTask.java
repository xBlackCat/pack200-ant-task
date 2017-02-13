package org.xblackcat.ant.p200ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class P200AntTask
        extends Task {
    private final Engine engine = new Engine();
    private final List<FileSet> filesets = new ArrayList<>();
    private boolean repack;
    private File srcfile;
    private boolean createPACK;
    private boolean createGZ = true;

    public void setKeepOrder(boolean enabled) {
        this.engine.setKeepOrder(enabled);
    }

    public void setKeepModificationTime(boolean enabled) {
        this.engine.setKeepModificationTime(enabled);
    }

    public void setSingleSegment(boolean enabled) {
        this.engine.setSingleSegment(enabled);
    }

    public void setSegmentLimit(int size) {
        this.engine.setSegmentLimit(size);
    }

    public void setConfigFile(File file) throws IOException {
        this.engine.loadProperties(file);
    }

    public void setRepack(boolean repack) {
        this.repack = repack;
    }

    public void setSrcfile(File file) {
        this.srcfile = file;
    }

    public void setDestdir(File dir) {
        this.engine.setDestDir(dir);
    }

    public void setPack(boolean createPACK) {
        this.createPACK = createPACK;
    }

    public void setGzip(boolean createGZ) {
        this.createGZ = createGZ;
    }

    public void addFileset(FileSet set) {
        this.filesets.add(set);
    }

    public void execute() throws BuildException {
        this.validate();
        List<File> files = new ArrayList<>();
        if (this.srcfile != null) {
            files.add(this.srcfile);
        } else {
            for (FileSet fs : this.filesets) {
                DirectoryScanner ds = fs.getDirectoryScanner(this.getProject());
                File dir = fs.getDir(this.getProject());
                for (String fileName : ds.getIncludedFiles()) {
                    files.add(new File(dir, fileName));
                }
            }
        }
        for (File f : files) {
            if (f.canRead() && f.isFile()) {
                continue;
            }
            throw new BuildException("File does not exist or can't be read: " + f);
        }
        ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (File file : files) {
            exe.submit(() -> {
                try {
                    if (repack) {
                        System.out.println("Repacking JAR: " + file);
                        engine.repack(file);
                    } else {
                        System.out.println("Packing JAR: " + file);
                        engine.pack(file, createPACK, createGZ);
                    }
                } catch (IOException ex) {
                    throw new BuildException("Error while processing file: " + file, ex);
                }
            });
        }
        exe.shutdown();
        try {
            exe.awaitTermination(3600, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void validate() throws BuildException {
        if (this.srcfile == null && this.filesets.size() == 0) {
            throw new BuildException("need to specify either file or fileset");
        }
        if (this.srcfile != null && this.filesets.size() > 0) {
            throw new BuildException("can't specify both file and fileset");
        }
    }

}
