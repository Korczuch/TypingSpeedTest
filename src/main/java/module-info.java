module pl.mickor.typyingtest {
    requires javafx.controls;
    requires javafx.fxml;


    opens pl.mickor.typyingtest to javafx.fxml;
    exports pl.mickor.typyingtest;
}