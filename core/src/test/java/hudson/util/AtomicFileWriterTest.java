package hudson.util;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.core.StringContains.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AtomicFileWriterTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    File af;
    AtomicFileWriter afw;
    String expectedContent = "hello world";

    @Before
    public void setUp() throws IOException {
        af = tmp.newFile();
        afw = new AtomicFileWriter(af.toPath(), Charset.defaultCharset());
    }

    @Test
    public void symlinkToDirectory() throws Exception {
        final File folder = tmp.newFolder();
        final File containingSymlink = tmp.newFolder();
        final Path zeSymlink = Files.createSymbolicLink(Paths.get(containingSymlink.getAbsolutePath(), "ze_symlink"),
                                                         folder.toPath());


        final Path childFileInSymlinkToDir = Paths.get(zeSymlink.toString(), "childFileInSymlinkToDir");

        new AtomicFileWriter(childFileInSymlinkToDir, Charset.forName("UTF-8"));
    }

    @Test
    public void createFile() throws Exception {
        // Verify the file we created exists
        assertTrue(Files.exists(afw.getTemporaryPath()));
    }

    @Test
    public void writeToAtomicFile() throws Exception {
        // Given
        afw.write(expectedContent, 0, expectedContent.length());

        // When
        afw.flush();

        // Then
        assertEquals("File writer did not properly flush to temporary file",
                expectedContent.length(), Files.size(afw.getTemporaryPath()));
    }

    @Test
    public void commitToFile() throws Exception {
        // Given
        afw.write(expectedContent, 0, expectedContent.length());

        // When
        afw.commit();

        // Then
        assertEquals(expectedContent.length(), Files.size(af.toPath()));
    }

    @Test
    public void abortDeletesTmpFile() throws Exception {
        // Given
        afw.write(expectedContent, 0, expectedContent.length());

        // When
        afw.abort();

        // Then
        assertTrue(Files.notExists(afw.getTemporaryPath()));
    }

    @Test
    public void badPath() throws Exception {
        final File newFile = tmp.newFile();
        File parentExistsAndIsAFile = new File(newFile, "badChild");

        assertTrue(newFile.exists());
        assertFalse(parentExistsAndIsAFile.exists());

        try {
            new AtomicFileWriter(parentExistsAndIsAFile.toPath(), Charset.forName("UTF-8"));
            fail("Expected a failure");
        } catch (IOException e) {
            assertThat(e.getMessage(),
                       containsString("exists and is neither a directory nor a symlink to a directory"));
        }
    }
}
