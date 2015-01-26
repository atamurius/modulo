package ua.atamurius.modulo.fs;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.zip.ZipFile.OPEN_READ;

/**
 * Module files iterator.
 */
public class ModuleContentEnumerator implements Iterable<String> {

    private final File module;

    public ModuleContentEnumerator(File module) {
        this.module = module;
    }

    public static Iterable<String> contentsOf(File module) {
        return new ModuleContentEnumerator(module);
    }

    private Enumeration<? extends ZipEntry> moduleEntries() {
        try {
            return new ZipFile(module, OPEN_READ).entries();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot open module file "+ module, e);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {

            Enumeration<? extends ZipEntry> entries = moduleEntries();

            @Override
            public boolean hasNext() {
                return entries.hasMoreElements();
            }

            @Override
            public String next() {
                if (! hasNext())
                    throw new NoSuchElementException();
                else
                    return entries.nextElement().getName();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
