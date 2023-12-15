package br.com.brunosalata.videoscreemrecord;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    @FXML
    private ImageView imgView;
    private Webcam webcam;
    private boolean videoStoped = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();
    }

    @FXML
    private void takeWebcam(){

        BufferedImage bufferedImage = webcam.getImage();
//        Image image = convertToFxImage(webcam.getImage());
        videoStoped = false;
        Platform.runLater(()->{
            Thread videoRec = new Thread(() -> {
                while(!videoStoped){
                    long takeInitTime = System.currentTimeMillis();
                    Image image = SwingFXUtils.toFXImage(webcam.getImage(), null);
                    imgView.setImage(image);
                    System.out.println("FPS: " + (1000/(System.currentTimeMillis() - takeInitTime)));
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });videoRec.start();
        });

    }

    @FXML
    private void stopWebcam(){
        videoStoped = true;
    }

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
//    protected void takePic() {
//        Webcam webcam = Webcam.getDefault();
//
//        if(webcam!=null){
//            webcam.addWebcamListener(new WebcamListener() {
//                @Override
//                public void webcamOpen(WebcamEvent webcamEvent) {
//                    System.out.println("Webcam opened");
//                }
//
//                @Override
//                public void webcamClosed(WebcamEvent webcamEvent) {
//                    System.out.println("Webcam closed");
//                }
//
//                @Override
//                public void webcamDisposed(WebcamEvent webcamEvent) {
//                    System.out.println("Webcam Disposed");
//                }
//
//                @Override
//                public void webcamImageObtained(WebcamEvent webcamEvent) {
//                    System.out.println("Image taken ");
//                }
//            });
//
//            webcam.setViewSize(new Dimension(640, 480));
//            webcam.setViewSize(WebcamResolution.VGA.getSize());
//
//            webcam.open();
//            try {
//                ImageIO.write(webcam.getImage(), "PNG", new File("firstCapture.png"));
//                webcam.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        } else{
//            System.out.println("Webcam disconnected");
//        }
//    }
}