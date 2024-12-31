/**
 * 
 */
/**
 * 
 */
module pbl_tree {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;
	requires java.desktop;
	requires javafx.base;
	
    opens application to javafx.base, javafx.fxml, javafx.graphics; // Mở cho javafx.graphics
}