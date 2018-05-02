//
// $Id$

package com.threerings.bang.game.tools;

import com.samskivert.util.CollectionUtil;
import com.threerings.bang.bang.client.BangDesktop;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.piece.Unit;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates a graph of unit versus unit damage.
 */
public class GenerateFileSystem
{
    public static void main (String[] args)
    {
        try {
            try (FileSystem fileSystem = BangDesktop.dataFiles) {

                for(String fn : getFileNames(null, new File("input").toPath()))
                {
                    InputStream inputStream = new FileInputStream(fn);

                    // obtain a path to a test file
                    Path writeFile = fileSystem.getPath(fn);

                    // create all parent directories
                    Files.createDirectories(writeFile.getParent());

                    Files.copy(
                            inputStream,
                            writeFile,
                            StandardCopyOption.REPLACE_EXISTING);

                    IOUtils.closeQuietly(inputStream);


                    // List all files present in a directory
                    try (Stream<Path> listing = Files.list(writeFile.getParent())) {
                        listing.forEach(System.out::println);
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getFileNames(List<String> fileNames, Path dir) {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if(path.toFile().isDirectory()) {
                    getFileNames(fileNames, path);
                } else {
                    fileNames.add(path.toAbsolutePath().toString());
                    System.out.println(path.getFileName());
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return fileNames;
    }
}
