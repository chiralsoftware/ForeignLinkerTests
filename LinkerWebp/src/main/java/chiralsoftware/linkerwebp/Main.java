package chiralsoftware.linkerwebp;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import jdk.incubator.foreign.LibraryLookup;

/**
 *
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    
    private static final String libraryPath = "/usr/lib/x86_64-linux-gnu/libwebp.so";
    
    public static void main(String[] args) {
        LOG.info("Let's test linking!");
        
        final Path path = Path.of(libraryPath);

        final LibraryLookup libraryLookup = LibraryLookup.ofPath(path);
        Optional<LibraryLookup.Symbol> optionalSymbol = libraryLookup.lookup("printHello");
        if(optionalSymbol.isPresent()) LOG.info("I loaded it!");
        else LOG.info("no i didn't load it.");
    }
    
}
