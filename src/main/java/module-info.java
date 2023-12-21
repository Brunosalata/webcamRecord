module br.com.brunosalata.videoscreemrecord {
    requires javafx.controls;
    requires javafx.fxml;
    requires webcam.capture;
    requires java.desktop;
    requires javafx.swing;
    requires humble.video.noarch;
    requires uk.co.caprica.vlcj;
    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;


    opens br.com.brunosalata.videoscreemrecord to javafx.fxml;
    exports br.com.brunosalata.videoscreemrecord;
}