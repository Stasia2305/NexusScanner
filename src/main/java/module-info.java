module com.nexusscan {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires java.net.http;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;

    opens com.nexusscan to javafx.fxml;
    opens com.nexusscan.controller to javafx.fxml;
    opens com.nexusscan.model to javafx.base;
    exports com.nexusscan;
    exports com.nexusscan.controller;
    exports com.nexusscan.model;
    exports com.nexusscan.service;
    exports com.nexusscan.dal;
    exports com.nexusscan.dal.interfaces;
}