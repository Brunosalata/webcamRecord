package br.com.brunosalata.videoscreemrecord;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamResolution;
import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.HBox;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ResourceBundle;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2BGRA;

public class HelloControllerOpenCV implements Initializable {
    @FXML
    private ImageView imgView;
    private static Webcam webcam;
    @FXML
    private static HBox hbOutputRecArea;
    @FXML
    private TextField txtCounting, txtTime;
    @FXML
    private ComboBox<Webcam> cbWebcamOptions;
    private static volatile boolean videoRecording = false;
    private volatile boolean webcamOpened = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());
//        webcam.setViewSize(new Dimension(640,480));
//        webcam.open();

        webcamList();

        cbWebcamOptions.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                webcam = newValue; // Define a webcam selecionada como a referência da webcam
                System.out.println("Webcam selecionada: " + webcam.getName());
            }
        });

        try {
            frameGrabber.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            while (getCameraActive()) {
                try {
                    Frame frame = frameGrabber.grab();
                    setVideoView(frame);
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                frameGrabber.release();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    Mat javaCVMat = new Mat();

    /**
     * create buffer only once saves much time!
     */
    WritablePixelFormat<ByteBuffer> formatByte = PixelFormat.getByteBgraPreInstance();

    OpenCVFrameConverter<Mat> javaCVConv = new OpenCVFrameConverter.ToMat();

    /**
     * controls if application closes
     */
    SimpleBooleanProperty cameraActiveProperty = new SimpleBooleanProperty(true);

    OpenCVFrameGrabber frameGrabber = new OpenCVFrameGrabber(0);

    ByteBuffer buffer;

    protected void updateView(Frame frame) {
        int w = frame.imageWidth;
        int h = frame.imageHeight;

        Mat mat = javaCVConv.convert(frame);
        opencv_imgproc.cvtColor(mat, javaCVMat, COLOR_BGR2BGRA);
        if (buffer == null) {
            buffer = javaCVMat.createBuffer();
        }

        PixelBuffer<ByteBuffer> pb = new PixelBuffer<ByteBuffer>(w, h, buffer, formatByte);
        final WritableImage wi = new WritableImage(pb);
        Platform.runLater(() -> imgView.setImage(wi));

    }


    public void setCameraActive(Boolean isActive) {
        cameraActiveProperty.set(isActive);
    }

    public Boolean getCameraActive() {
        return cameraActiveProperty.get();
    }

    public void shutdown() {
        setCameraActive(false);
    }

    void setVideoView(Frame mat) {
        updateView(mat);
    }



    private void webcamList(){
        List<Webcam> webcamOptions = Webcam.getWebcams();
        cbWebcamOptions.getItems().addAll(webcamOptions);
    }

    @FXML
    private void openWebcam() throws IOException, InterruptedException, AWTException {

        webcam.open();
        webcamOpened = true;

        AtomicLong frameCounter = new AtomicLong(0);
        AtomicLong lastFPSCheckTime = new AtomicLong(System.currentTimeMillis());
        long startTime = System.currentTimeMillis();

        Platform.runLater(() -> {
            Thread videoRec = new Thread(() -> {
                while (webcamOpened) {
                    Image image = SwingFXUtils.toFXImage(webcam.getImage(), null);
//                    Image image = convertToFxImage(webcam.getImage());
                    imgView.setImage(image);
                    frameCounter.incrementAndGet();

                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - lastFPSCheckTime.get();
                    if (elapsedTime >= 1000) { // Calcula o FPS a cada segundo
                        double fps = (frameCounter.get() * 1000.0) / elapsedTime;
                        System.out.println("FPS: " + fps);
                        frameCounter.set(0);
                        lastFPSCheckTime.set(currentTime);
                    }

                    Platform.runLater(()->{
                        txtCounting.setText(frameCounter.toString());
                        txtTime.setText(String.valueOf(((currentTime - startTime) / 1000)));
                    });

                    try {
                        Thread.sleep(15); // Aguarda 15ms para o próximo quadro
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                webcam.close();
            });
            videoRec.setDaemon(true);
            videoRec.start();
        });

    }

    @FXML
    private void closeWebcam(){
        webcamOpened = false;
    }

    /**
     * Method without Swing library
     */
//    private Image convertToFxImage(BufferedImage img){
//        long convInitTime = System.currentTimeMillis();
//        //converting to a good type, read about types here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
//        BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
//        newImg.createGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
//
//        //converting the BufferedImage to an IntBuffer
//        int[] type_int_agrb = ((DataBufferInt) newImg.getRaster().getDataBuffer()).getData();
//        IntBuffer buffer = IntBuffer.wrap(type_int_agrb);
//
//        //converting the IntBuffer to an Image, read more about it here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
//        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
//        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer(newImg.getWidth(), newImg.getHeight(), buffer, pixelFormat);
//        System.out.println("conv time: " + (System.currentTimeMillis() - convInitTime));
//        return new WritableImage(pixelBuffer);
//    }

    @FXML
    protected void takePic() {

        boolean notOpenedWebcamMode = false;
        if (!webcamOpened) {
            Webcam webcam = Webcam.getDefault();
            notOpenedWebcamMode = true;
        }

        if (webcam != null) {
            webcam.addWebcamListener(new WebcamListener() {
                @Override
                public void webcamOpen(WebcamEvent webcamEvent) {
                    System.out.println("Webcam opened");
                }

                @Override
                public void webcamClosed(WebcamEvent webcamEvent) {
                    System.out.println("Webcam closed");
                }

                @Override
                public void webcamDisposed(WebcamEvent webcamEvent) {
                    System.out.println("Webcam Disposed");
                }

                @Override
                public void webcamImageObtained(WebcamEvent webcamEvent) {
                    System.out.println("Image taken ");
                }
            });

            webcam.setViewSize(new Dimension(640, 480));
            webcam.setViewSize(WebcamResolution.VGA.getSize());

            if(notOpenedWebcamMode){
                webcam.open();
            }
            try {
                ImageIO.write(webcam.getImage(), "PNG", new File("firstCapture.png"));
                if(notOpenedWebcamMode){
                    webcam.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Webcam disconnected");
        }
    }

//    @FXML
//    private void startRecord() {
//
//        videoRecording = true;
//
//        new Thread(() -> {
//            try {
//                recordScreen("output.mp4", null, null, 0, 15);
//            } catch (AWTException | IOException | InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }).start();
//    }

    /**
     * Records the screen
     */
//    private static void recordScreen(String filename, String formatname, String codecname, int duration, int snapsPerSecond) throws AWTException, InterruptedException, IOException {
//        /**
//         * Set up the AWT infrastructure to take screenshots of the desktop.
//         */
//        Rectangle size = new Rectangle(580,300, 730, 240);
////        final Rectangle size = new Rectangle(webcam.getViewSize());
//        final Rational framerate = Rational.make(1, snapsPerSecond);
//
//        /** First we create a muxer using the passed in filename and formatname if given. */
//        final Muxer muxer = Muxer.make(filename, null, formatname);
//
//        /**
//         * Now, we need to decide what type of codec to use to encode video. Muxers have limited
//         * sets of codecs they can use. We're going to pick the first one that works, or if the user
//         * supplied a codec name, we're going to force-fit that in instead.
//         */
//        final MuxerFormat format = muxer.getFormat();
//        final Codec codec;
//        if (codecname != null) {
//            codec = Codec.findEncodingCodecByName(codecname);
//        } else {
//            codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());
//        }
//
//        /**
//         * Now that we know what codec, we need to create an encoder
//         */
//        Encoder encoder = Encoder.make(codec);
//
//        /**
//         * Video encoders need to know at a minimum: width height pixel format Some also need to
//         * know frame-rate (older codecs that had a fixed rate at which video files could be written
//         * needed this). There are many other options you can set on an encoder, but we're going to
//         * keep it simpler here.
//         */
//        encoder.setWidth(730);
//        encoder.setHeight(240);
//        // We are going to use 420P as the format because that's what most video formats these days
//        // use
//        final PixelFormat.Type pixelformat = PixelFormat.Type.PIX_FMT_YUV420P;
//        encoder.setPixelFormat(pixelformat);
//        encoder.setTimeBase(framerate);
//
//        /**
//         * An annoynace of some formats is that they need global (rather than per-stream) headers,
//         * and in that case you have to tell the encoder. And since Encoders are decoupled from
//         * Muxers, there is no easy way to know this beyond
//         */
//        if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER))
//            encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
//
//        /** Open the encoder. */
//        encoder.open(null, null);
//
//        /** Add this stream to the muxer. */
//        muxer.addNewStream(encoder);
//
//        /** And open the muxer for business. */
//        muxer.open(null, null);
//
//        /**
//         * Next, we need to make sure we have the right MediaPicture format objects to encode data
//         * with. Java (and most on-screen graphics programs) use some variant of Red-Green-Blue
//         * image encoding (a.k.a. RGB or BGR). Most video codecs use some variant of YCrCb
//         * formatting. So we're going to have to convert. To do that, we'll introduce a
//         * MediaPictureConverter object later. object.
//         */
//        MediaPictureConverter converter = null;
//        final MediaPicture picture = MediaPicture
//                .make(
//                        encoder.getWidth(),
//                        encoder.getHeight(),
//                        pixelformat);
//        picture.setTimeBase(framerate);
//
//        /**
//         * Open webcam so we can capture video feed.
//         */
//
//        // Already opened
////        webcam.open();
//
//        /**
//         * Now begin our main loop of taking screen snaps. We're going to encode and then write out
//         * any resulting packets.
//         */
//        final MediaPacket packet = MediaPacket.make();
////        for (int i = 0; i < duration / framerate.getDouble(); i++) {
//        int i = 0;
//        Robot robot = new Robot();
//
//        BufferedImage image = null;
//        while(videoRecording){
//
//            /**
//             * Make the screen capture && convert image to TYPE_3BYTE_BGR
//             */
////            final BufferedImage image = webcam.getImage();
//
//
//                image = robot.createScreenCapture(size);
//
//            }
//        assert image != null;
//        final BufferedImage frame = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);
//
//            System.out.println("Record frame " + frame);
//
//            /**
//             * This is LIKELY not in YUV420P format, so we're going to convert it using some handy
//             * utilities.
//             */
//            if (converter == null) {
//                converter = MediaPictureConverterFactory.createConverter(frame, picture);
//            }
//            converter.toPicture(picture, frame, i);
//
//            do {
//                encoder.encode(packet, picture);
//                if (packet.isComplete()) {
//                    muxer.write(packet, false);
//                }
//            } while (packet.isComplete());
//
//            i++;
//
//            /** now we'll sleep until it's time to take the next snapshot. */
//            Thread.sleep((long) (1000 * framerate.getDouble()));
//
//
//        /**
//         * Encoders, like decoders, sometimes cache pictures so it can do the right key-frame
//         * optimizations. So, they need to be flushed as well. As with the decoders, the convention
//         * is to pass in a null input until the output is not complete.
//         */
//        do {
//            encoder.encode(packet, null);
//            if (packet.isComplete()) {
//                muxer.write(packet, false);
//            }
//        } while (packet.isComplete());
//
//        /**
//         * Finally, let's clean up after ourselves.
//         */
//
////        webcam.close();
//        muxer.close();
//    }

    @FXML
    private void stopRecord(){
        videoRecording = false;
    }

    /**
     * Convert a {@link BufferedImage} of any type, to {@link BufferedImage} of a specified type. If
     * the source image is the same type as the target type, then original image is returned,
     * otherwise new image of the correct type is created and the content of the source image is
     * copied into the new image.
     *
     * @param sourceImage the image to be converted
     * @param targetType the desired BufferedImage type
     * @return a BufferedImage of the specifed target type.
     * @see BufferedImage
     */
    public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
        BufferedImage image;

        // if the source image is already the target type, return the source image

        if (sourceImage.getType() == targetType)
            image = sourceImage;

            // otherwise create a new image of the target type and draw the new
            // image

        else {
            image = new BufferedImage(
                    sourceImage.getWidth(),
                    sourceImage.getHeight(), targetType);
            image.getGraphics().drawImage(sourceImage, 0, 0, null);
        }

        return image;
    }
}