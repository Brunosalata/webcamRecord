package br.com.brunosalata.videoscreemrecord;

import com.github.sarxos.webcam.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

public class HelloController implements Initializable {
    @FXML
    private ImageView imgView;
    private Webcam webcam;
    @FXML
    private ComboBox<Webcam> cbWebcamOptions;
    private boolean videoStoped = true;

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
    private void takeWebcam(){

        webcam.open();
        videoStoped = false;

        AtomicLong frameCounter = new AtomicLong(0);
        AtomicLong lastFPSCheckTime = new AtomicLong(System.currentTimeMillis());

        Platform.runLater(() -> {
            Thread videoRec = new Thread(() -> {
                while (!videoStoped) {
                    long takeInitTime = System.currentTimeMillis();
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

                    try {
                        Thread.sleep(10); // Aguarda 50ms para o próximo quadro
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
    private void stopWebcam(){
        videoStoped = true;
    }

    /**
     * Method without Swing library
     * @param img
     * @return
     */
    private Image convertToFxImage(BufferedImage img){
        long convInitTime = System.currentTimeMillis();
        //converting to a good type, read about types here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        BufferedImage newImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        newImg.createGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);

        //converting the BufferedImage to an IntBuffer
        int[] type_int_agrb = ((DataBufferInt) newImg.getRaster().getDataBuffer()).getData();
        IntBuffer buffer = IntBuffer.wrap(type_int_agrb);

        //converting the IntBuffer to an Image, read more about it here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer(newImg.getWidth(), newImg.getHeight(), buffer, pixelFormat);
        System.out.println("conv time: " + (System.currentTimeMillis() - convInitTime));
        return new WritableImage(pixelBuffer);
    }

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
}