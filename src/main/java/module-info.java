module com.nexusscan {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires java.desktop;
    requires java.net.http;
    requires java.sql;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens com.nexusscan to javafx.fxml;
    opens com.nexusscan.controller to javafx.fxml;
    exports com.nexusscan;
    exports com.nexusscan.controller;
}