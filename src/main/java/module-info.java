module br.com.brunosalata.videoscreemrecord {
    requires javafx.controls;
    requires javafx.fxml;
    requires webcam.capture;
    requires java.desktop;
    requires javafx.swing;


    opens br.com.brunosalata.videoscreemrecord to javafx.fxml;
    exports br.com.brunosalata.videoscreemrecord;
}