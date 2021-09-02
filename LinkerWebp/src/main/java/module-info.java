module LinkerWebp {
    requires jdk.incubator.foreign;
    requires java.logging;
    requires java.desktop;
    exports chiralsoftware.linkerwebp;
    uses javax.imageio.spi.ImageReaderSpi;
}
