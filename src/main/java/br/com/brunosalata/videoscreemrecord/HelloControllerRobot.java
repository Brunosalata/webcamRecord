package br.com.brunosalata.videoscreemrecord;

import com.github.sarxos.webcam.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import io.humble.video.Codec;
import io.humble.video.Encoder;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.Muxer;
import io.humble.video.MuxerFormat;
import io.humble.video.PixelFormat;
import io.humble.video.Rational;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

public class HelloControllerRobot implements Initializable {
    @FXML
    private ImageView imgView;
    private static Webcam webcam;
    @FXML
    private static HBox hbOutputRecArea;
    @FXML
    private TextField txtCounting, txtTime;
    @FXML
    private ComboBox<Webcam> cbWebcamOptions;
    private boolean videoStoped = true;
    private volatile boolean videoRecording = false;
    private DecimalFormat decimal = new DecimalFormat("HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webcam = Webcam.getDefault();
//        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.setViewSize(new Dimension(640,480));
//        webcam.open();

        webcamList();

        cbWebcamOptions.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                webcam = newValue; // Define a webcam selecionada como a referência da webcam
                System.out.println("Webcam selecionada: " + webcam.getName());
            }
        });
    }

    private void webcamList(){
        List<Webcam> webcamOptions = Webcam.getWebcams();
        cbWebcamOptions.getItems().addAll(webcamOptions);
    }

    @FXML
    private void openWebcam() throws IOException, InterruptedException, AWTException {

        webcam.open();
        videoStoped = false;

        AtomicLong frameCounter = new AtomicLong(0);
        AtomicLong lastFPSCheckTime = new AtomicLong(System.currentTimeMillis());
        long startTime = System.currentTimeMillis();

        Platform.runLater(() -> {
            Thread videoRec = new Thread(() -> {
                while (!videoStoped) {
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
                        Thread.sleep(15); // Aguarda 50ms para o próximo quadro
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
        videoStoped = true;
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

        if (videoStoped) {
            Webcam webcam = Webcam.getDefault();

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

                webcam.open();
                try {
                    ImageIO.write(webcam.getImage(), "PNG", new File("firstCapture.png"));
                    webcam.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Webcam disconnected");
            }
        }
    }

    @FXML
    private void startRecord() throws AWTException {
        Robot robot = new Robot();
        Rectangle rectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage image = robot.createScreenCapture(rectangle);

        Image myImage = SwingFXUtils.toFXImage(image, null);
        imgView.setImage(myImage);
    }

    @FXML
    private void startRecParcial() throws AWTException {
        Robot robot = new Robot();
//        Rectangle rectangle = new Rectangle(0,0, (int) hbOutputRecArea.getWidth(), (int) hbOutputRecArea.getHeight());
        Rectangle rectangle = new Rectangle(580,300, 730, 240);
        BufferedImage image = robot.createScreenCapture(rectangle);

        Image myImage = SwingFXUtils.toFXImage(image, null);
        imgView.setImage(myImage);
    }

    /**
     * Records the screen
     */
    private void recordScreen(String filename, String formatname, String codecname, int duration, int snapsPerSecond) throws AWTException, InterruptedException, IOException {

//        if(hbOutputRecArea!=null){
//            Rectangle captureRect = new Rectangle((int) hbOutputRecArea.getBoundsInParent().getWidth(),
//                    (int) hbOutputRecArea.getBoundsInParent().getHeight());
//        } else{
//        }

//        Rectangle captureRect = new Rectangle(Window.getWindows().size(), 500);
        Rectangle captureRect = new Rectangle(webcam.getViewSize());

        Robot robot = new Robot();
        Muxer muxer = Muxer.make(filename, null, formatname);

        Rational framerate = Rational.make(1, snapsPerSecond);

        Encoder encoder = Encoder.make(Codec.findEncodingCodecByName(codecname));

        encoder.setWidth(captureRect.width);
        encoder.setHeight(captureRect.height);
        encoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUV420P);
        encoder.setTimeBase(framerate);

        if (muxer.getFormat().getFlag(MuxerFormat.Flag.GLOBAL_HEADER)) {
            encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
        }

        encoder.open(null, null);
        muxer.addNewStream(encoder);
        muxer.open(null, null);

        MediaPictureConverter converter = null;
        MediaPicture picture = MediaPicture.make(encoder.getWidth(), encoder.getHeight(), PixelFormat.Type.PIX_FMT_BGR24);

        picture.setTimeBase(framerate);

        MediaPacket packet = MediaPacket.make();

        long startTime = System.nanoTime();

        while (videoRecording) {
            BufferedImage frame = robot.createScreenCapture(captureRect);
            if (converter == null) {
                converter = MediaPictureConverterFactory.createConverter(frame, picture);
            }
            converter.toPicture(picture, frame, System.nanoTime() - startTime);

            do {
                encoder.encode(packet, picture);
                if (packet.isComplete()) {
                    muxer.write(packet, false);
                }
            } while (packet.isComplete());

            Thread.sleep((long) (1000 * framerate.getDouble()));
        }

        do {
            encoder.encode(packet, null);
            if (packet.isComplete()) {
                muxer.write(packet, false);
            }
        } while (packet.isComplete());

        muxer.close();
    }

    @FXML
    private void stopRecord(){

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