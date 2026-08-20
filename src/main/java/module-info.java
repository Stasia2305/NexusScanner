module com.nexusscan {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires java.net.http;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;

    opens com.nexusscan to javafx.fxml;
    opens com.nexusscan.GUI to javafx.fxml;
    opens com.nexusscan.model to javafx.base;
    exports com.nexusscan;
    exports com.nexusscan.GUI;
    exports com.nexusscan.model;
    exports com.nexusscan.logic;
    exports com.nexusscan.dal;
    exports com.nexusscan.dal.interfaces;
}