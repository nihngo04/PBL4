package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

public class Main extends Application {

	private StackPane rootPane;
	private Parent loginRoot;
	private Parent registerRoot;
//	private Parent mainRoot;

	@Override
	public void start(Stage primaryStage) {
		try {
			rootPane = new StackPane();

			// Tạo instance cho Login và Register
			FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
			loginRoot = loginLoader.load();

			FXMLLoader registerLoader = new FXMLLoader(getClass().getResource("Register.fxml"));
			registerRoot = registerLoader.load();

			// Thiết lập tham chiếu mainApp cho các controller
			LoginController loginController = loginLoader.getController();
			loginController.setMainApp(this);

			RegisterController registerController = registerLoader.getController();
			registerController.setMainApp(this);

			// Thêm form đăng nhập vào StackPane
			rootPane.getChildren().add(loginRoot);

			Scene scene = new Scene(rootPane, 977, 720);
			primaryStage.setScene(scene);
			primaryStage.setTitle("Đăng nhập");
			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void showLogin() {
		rootPane.getChildren().clear(); // Xóa tất cả các node
		rootPane.getChildren().add(loginRoot); // Thêm form đăng nhập
	}

	public void showRegister() {
		rootPane.getChildren().clear(); // Xóa tất cả các node
		rootPane.getChildren().add(registerRoot); // Thêm form đăng ký
	}

	public static void main(String[] args) {
		launch(args);
	}
}
