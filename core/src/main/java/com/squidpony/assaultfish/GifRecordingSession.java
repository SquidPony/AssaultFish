package com.squidpony.assaultfish;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

final class GifRecordingSession {
    private final String stamp;
    private final Path recordingFramesDir;
    private int frameIndex;
    private boolean captureRequested;
    private boolean captureOnlyIfChanged;
    private boolean hasFrameHash;
    private int lastFrameHash;

    private GifRecordingSession(String stamp, Path recordingFramesDir) {
        this.stamp = stamp;
        this.recordingFramesDir = recordingFramesDir;
    }

    static GifRecordingSession start(Path screenshotDir, String stamp) throws IOException {
        Files.createDirectories(screenshotDir);
        Path recordingFramesDir = screenshotDir.resolve("recording-" + stamp + "-frames");
        Files.createDirectories(recordingFramesDir);
        return new GifRecordingSession(stamp, recordingFramesDir);
    }

    void requestCapture(boolean onlyIfChanged) {
        if (!captureRequested) {
            captureRequested = true;
            captureOnlyIfChanged = onlyIfChanged;
            return;
        }

        captureOnlyIfChanged &= onlyIfChanged;
    }

    void flushCapture(Pixmap frame) throws IOException {
        if (!captureRequested) {
            return;
        }

        boolean onlyIfChanged = captureOnlyIfChanged;
        captureRequested = false;
        captureOnlyIfChanged = false;

        int frameHash = hashPixmap(frame);
        if (onlyIfChanged && hasFrameHash && lastFrameHash == frameHash) {
            return;
        }

        writeFrame(frame);
        lastFrameHash = frameHash;
        hasFrameHash = true;
    }

    Path finish(Path screenshotDir, String outputNamePrefix, int gifFrameDelayMs, boolean loopForever) throws IOException {
        if (frameIndex == 0) {
            discard();
            return null;
        }

        Files.createDirectories(screenshotDir);
        Path target = screenshotDir.resolve(outputNamePrefix + "-" + stamp + ".gif");
        try {
            List<Path> frameFiles = listFrameFiles(recordingFramesDir);
            if (frameFiles.isEmpty()) {
                return null;
            }
            writeAnimatedGif(frameFiles, target, gifFrameDelayMs, loopForever);
            return target;
        } finally {
            discard();
        }
    }

    void discard() {
        deleteDirectory(recordingFramesDir.toFile());
        resetCaptureState();
    }

    private void resetCaptureState() {
        frameIndex = 0;
        captureRequested = false;
        captureOnlyIfChanged = false;
        hasFrameHash = false;
        lastFrameHash = 0;
    }

    private void writeFrame(Pixmap frame) throws IOException {
        Files.createDirectories(recordingFramesDir);
        frameIndex++;
        Path frameFile = recordingFramesDir.resolve(String.format("frame-%06d.png", frameIndex));
        PixmapIO.writePNG(Gdx.files.absolute(frameFile.toAbsolutePath().toString()), frame);
    }

    private static int hashPixmap(Pixmap pixmap) {
        ByteBuffer pixels = pixmap.getPixels();
        int hash = 1;
        for (int i = 0, limit = pixels.limit(); i < limit; i++) {
            hash = 31 * hash + pixels.get(i);
        }
        return hash;
    }

    private static List<Path> listFrameFiles(Path frameDir) throws IOException {
        List<Path> files = new ArrayList<>();
        if (frameDir == null || !Files.exists(frameDir) || !Files.isDirectory(frameDir)) {
            return files;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(frameDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".png"))
                    .sorted()
                    .forEach(files::add);
        }
        return files;
    }

    private static void writeAnimatedGif(List<Path> frameFiles, Path outputFile, int delayMs, boolean loopForever)
            throws IOException {
        if (frameFiles.isEmpty()) {
            return;
        }

        BufferedImage first = ImageIO.read(frameFiles.get(0).toFile());
        if (first == null) {
            throw new IOException("Unable to read first recording frame");
        }
        int imageType = first.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : first.getType();

        try (ImageOutputStream outputStream = new FileImageOutputStream(outputFile.toFile());
                GifSequenceWriter gifWriter = new GifSequenceWriter(outputStream, imageType, delayMs, loopForever)) {
            gifWriter.writeToSequence(first);
            for (int i = 1; i < frameFiles.size(); i++) {
                BufferedImage frame = ImageIO.read(frameFiles.get(i).toFile());
                if (frame != null) {
                    gifWriter.writeToSequence(frame);
                }
            }
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDirectory(child);
            }
        }
        dir.delete();
    }

    private static final class GifSequenceWriter implements AutoCloseable {
        private final ImageWriter gifWriter;
        private final ImageWriteParam imageWriteParam;
        private final IIOMetadata imageMetaData;

        private GifSequenceWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesMs,
                boolean loopContinuously) throws IOException {
            gifWriter = getWriter();
            imageWriteParam = gifWriter.getDefaultWriteParam();
            ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

            imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

            String metaFormatName = imageMetaData.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

            IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
            graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
            graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(Math.max(1, timeBetweenFramesMs / 10)));
            graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

            IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
            commentsNode.setAttribute("CommentExtension", "Created by AssaultFish");

            IIOMetadataNode applicationExtensionsNode = getNode(root, "ApplicationExtensions");
            IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
            child.setAttribute("applicationID", "NETSCAPE");
            child.setAttribute("authenticationCode", "2.0");
            int loop = loopContinuously ? 0 : 1;
            child.setUserObject(new byte[] {
                    0x1,
                    (byte) (loop & 0xFF),
                    (byte) ((loop >> 8) & 0xFF)
            });
            applicationExtensionsNode.appendChild(child);

            imageMetaData.setFromTree(metaFormatName, root);

            gifWriter.setOutput(outputStream);
            gifWriter.prepareWriteSequence(null);
        }

        private void writeToSequence(BufferedImage img) throws IOException {
            gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
        }

        @Override
        public void close() throws IOException {
            gifWriter.endWriteSequence();
        }

        private static ImageWriter getWriter() throws IOException {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
            if (!iter.hasNext()) {
                throw new IOException("No GIF Image Writers Exist");
            }
            return iter.next();
        }

        private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
            int nNodes = rootNode.getLength();
            for (int i = 0; i < nNodes; i++) {
                if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                    return (IIOMetadataNode) rootNode.item(i);
                }
            }
            IIOMetadataNode node = new IIOMetadataNode(nodeName);
            rootNode.appendChild(node);
            return node;
        }
    }
}