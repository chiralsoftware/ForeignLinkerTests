/** This does require (for now) Project Loom builds:
 https://jdk.java.net/loom/
 */

module LinkerWebp {
    requires jdk.incubator.foreign;
    requires java.logging;
    requires java.desktop;
    requires java.base;
    exports chiralsoftware.linkerwebp;
    uses javax.imageio.spi.ImageReaderSpi;
}
