package ec.util.completion;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileAutoCompletionSource implements AutoCompletionSource {

    protected final boolean strict;
    protected final FileFilter fileFilter;
    protected final File[] paths;

    public FileAutoCompletionSource() {
        this(false, null, new File[0]);
    }

    public FileAutoCompletionSource(boolean strict, FileFilter fileFilter, File[] paths) {
        this.strict = strict;
        this.fileFilter = fileFilter;
        this.paths = paths;
    }

    @Override
    public Behavior getBehavior(String term) {
        return Behavior.ASYNC;
    }

    @Override
    public String toString(Object value) {
        return ((File) value).getPath();
    }

    @Override
    public List<File> getValues(final String term) throws IOException {
        // case 1: absolute path
        {
            File file = new File(term);
            // is a directory -> get all children
            if (file.exists() && file.isDirectory()) {
                return children(file, fileFilter);
            }
            File parent = file.getParentFile();
            // is not a directory but has parent -> get siblings
            if (parent != null && parent.exists()) {
                return children(parent, normalizedFilter(term));
            }
        }
        // case 2: relative path
        {
            for (File path : paths) {
                File file = new File(path, term);
                // is a directory -> get all children
                if (file.exists() && file.isDirectory()) {
                    return toRelativeFiles(children(file, fileFilter), path);
                }
                File parent = file.getParentFile();
                // is not a directory but has parent -> get siblings
                if (parent != null && parent.exists()) {
                    return toRelativeFiles(children(parent, normalizedFilter(file.getAbsolutePath())), path);
                }
            }
        }
        return Collections.emptyList();
    }

    List<File> toRelativeFiles(List<File> files, File path) {
        for (int i = 0; i < files.size(); i++) {
            String tmp = files.get(i).getPath().substring(path.getPath().length() + 1);
            files.set(i, new File(tmp));
        }
        return files;
    }

    FileFilter normalizedFilter(String term) {
        final String normalizedTerm = getNormalizedString(term);
        return new FileFilter() {
            @Override
            public boolean accept(File o) {
                return (fileFilter == null || fileFilter.accept(o)) && getNormalizedString(o.getPath()).startsWith(normalizedTerm);
            }
        };
    }

    String getNormalizedString(String input) {
        return strict ? input : AutoCompletionSources.normalize(input);
    }

    static List<File> children(File folder, FileFilter fileFilter) {
        File[] result = folder.listFiles(fileFilter);
        // result == null => An I/O exception occured
        return result != null ? Arrays.asList(result) : Collections.<File>emptyList();
    }
}
