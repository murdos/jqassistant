package com.buschmais.jqassistant.core.scanner.impl;

import com.buschmais.jqassistant.core.model.api.descriptor.Descriptor;
import com.buschmais.jqassistant.core.scanner.api.ArtifactScanner;
import com.buschmais.jqassistant.core.scanner.api.ArtifactScannerPlugin;
import com.buschmais.jqassistant.core.store.api.Store;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.buschmais.jqassistant.core.scanner.api.ArtifactScannerPlugin.InputStreamSource;

/**
 * Implementation of the {@link ArtifactScanner}.
 */
public class ArtifactScannerImpl implements ArtifactScanner {

    private abstract class AbstractIterable<E> implements Iterable<Descriptor> {

        protected abstract boolean hasNextElement();

        protected abstract E nextElement();

        protected abstract boolean isDirectory(E element);

        protected abstract String getFileName(E element);

        protected abstract InputStream openInputStream(String name, E element) throws IOException;

        protected abstract void close() throws IOException;

        @Override
        public Iterator<Descriptor> iterator() {
            return new Iterator<Descriptor>() {

                private Descriptor next = null;

                @Override
                public boolean hasNext() {
                    try {
                        while (next == null && hasNextElement()) {
                            E element = nextElement();
                            String fileName = getFileName(element);
                            if (isDirectory(element)) {
                                next = scanDirectory(fileName);
                            } else {
                                next = scanFile(fileName, getStreamSource(openInputStream(fileName, element)));
                            }
                        }
                        if (next != null) {
                            return true;
                        }
                        close();
                        return false;
                    } catch (IOException e) {
                        throw new IllegalStateException("Cannot iterator over elements.", e);
                    }
                }

                @Override
                public Descriptor next() {
                    if (hasNext()) {
                        Descriptor result = next;
                        next = null;
                        return result;
                    }
                    throw new NoSuchElementException("No more results.");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Cannot remove element.");
                }
            };
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactScannerImpl.class);

    private Store store;
    private Collection<ArtifactScannerPlugin> plugins;

    /**
     * Constructor.
     *
     * @param plugins The {@link ArtifactScannerPlugin}s to use for scanning.
     */
    public ArtifactScannerImpl(Store store, Collection<ArtifactScannerPlugin> plugins) {
        this.store = store;
        this.plugins = plugins;
    }

    @Override
    public Iterable<Descriptor> scanArchive(File archive) throws IOException {
        if (!archive.exists()) {
            throw new IOException("Archive '" + archive.getAbsolutePath() + "' not found.");
        }
        LOGGER.info("Scanning archive '{}'.", archive.getAbsolutePath());
        final ZipFile zipFile = new ZipFile(archive);
        final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        return new AbstractIterable<ZipEntry>() {
            @Override
            protected boolean hasNextElement() {
                return zipEntries.hasMoreElements();
            }

            @Override
            protected ZipEntry nextElement() {
                return zipEntries.nextElement();
            }

            @Override
            protected boolean isDirectory(ZipEntry element) {
                return element.isDirectory();
            }

            @Override
            protected String getFileName(ZipEntry element) {
                return element.getName();
            }

            @Override
            protected InputStream openInputStream(String fileName, ZipEntry element) throws IOException {
                return zipFile.getInputStream(element);
            }

            @Override
            protected void close() throws IOException {
                zipFile.close();
            }
        };
    }

    @Override
    public Iterable<Descriptor> scanDirectory(File directory) throws IOException {
        final List<File> files = new ArrayList<>();
        new DirectoryWalker<File>() {

            @Override
            protected boolean handleDirectory(File directory, int depth, Collection<File> results) throws IOException {
                results.add(directory);
                return true;
            }

            @Override
            protected void handleFile(File file, int depth, Collection<File> results) throws IOException {
                results.add(file);
            }

            public void scan(File directory) throws IOException {
                super.walk(directory, files);
            }
        }.scan(directory);
        LOGGER.info("Scanning directory '{}' [{} files].", directory.getAbsolutePath(), files.size());
        final URI directoryURI = directory.toURI();
        final Iterator<File> iterator = files.iterator();
        return new AbstractIterable<File>() {
            @Override
            protected boolean hasNextElement() {
                return iterator.hasNext();
            }

            @Override
            protected File nextElement() {
                return iterator.next();
            }

            @Override
            protected boolean isDirectory(File element) {
                return element.isDirectory();
            }

            @Override
            protected String getFileName(File element) {
                String name = directoryURI.relativize(element.toURI()).toString();
                if (element.isDirectory()) {
                    if (!StringUtils.isEmpty(name)) {
                        return name.substring(0, name.length() - 1);
                    }
                    return name;
                } else {
                    return name;
                }
            }

            @Override
            protected InputStream openInputStream(String fileName, File element) throws IOException {
                return new FileInputStream(element);
            }

            @Override
            protected void close() throws IOException {
            }
        };
    }

    @Override
    public Iterable<Descriptor> scanClasses(final Class<?>... classes) throws IOException {
        return new AbstractIterable<Class<?>>() {
            int index = 0;

            @Override
            protected boolean hasNextElement() {
                return index < classes.length;
            }

            @Override
            protected Class<?> nextElement() {
                return classes[index++];
            }

            @Override
            protected boolean isDirectory(Class<?> element) {
                return false;
            }

            @Override
            protected String getFileName(Class<?> element) {
                return "/" + element.getName().replace('.', '/') + ".class";
            }

            @Override
            protected InputStream openInputStream(String fileName, Class<?> element) throws IOException {
                return element.getResourceAsStream(fileName);
            }

            @Override
            protected void close() throws IOException {
            }
        };
    }

    @Override
    public Iterable<Descriptor> scanURLs(final URL... urls) throws IOException {
        return new AbstractIterable<URL>() {
            int index = 0;

            @Override
            protected boolean hasNextElement() {
                return index < urls.length;
            }

            @Override
            protected URL nextElement() {
                return urls[index++];
            }

            @Override
            protected boolean isDirectory(URL element) {
                return false;
            }

            @Override
            protected String getFileName(URL element) {
                return element.getPath() + "/" + element.getFile();
            }

            @Override
            protected InputStream openInputStream(String fileName, URL element) throws IOException {
                return element.openStream();
            }

            @Override
            protected void close() throws IOException {
            }
        };
    }

    /**
     * Return a {@link InputStreamSource} for the given input stream.
     *
     * @param inputStream The input stream.
     * @return The {@link InputStreamSource}.
     */

    private InputStreamSource getStreamSource(final InputStream inputStream) {
        return new InputStreamSource() {
            @Override
            public InputStream openStream() throws IOException {
                return new BufferedInputStream((inputStream));
            }
        };
    }

    /**
     * Scans the given stream source                                          .
     *
     * @param name         The name of the file, relative to the artifact root directory.
     * @param streamSource The stream source.
     * @throws IOException If scanning fails.
     */
    private Descriptor scanFile(String name, InputStreamSource streamSource) throws IOException {
        for (ArtifactScannerPlugin plugin : this.plugins) {
            if (plugin.matches(name, false)) {
                LOGGER.info("Scanning file '{}'", name);
                return plugin.scanFile(store, streamSource);
            }
        }
        return null;
    }

    /**
     * Scans the given stream source                                          .
     *
     * @param name The name of the file, relative to the artifact root directory.
     * @throws IOException If scanning fails.
     */
    private Descriptor scanDirectory(String name) throws IOException {
        for (ArtifactScannerPlugin plugin : this.plugins) {
            if (plugin.matches(name, true)) {
                LOGGER.info("Scanning directory '{}'", name);
                return plugin.scanDirectory(store, name);
            }
        }
        return null;
    }
}
