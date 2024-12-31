package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;

public class LoginController {

	@FXML
	private TextField txtPass;

	@FXML
	private TextField txtUserName;

	@FXML
	private Label lblResponse;

	private Main mainApp; // Tham chiếu đến Main để gọi phương thức chuyển đổi

	public void setMainApp(Main mainApp) {
		this.mainApp = mainApp; // Thiết lập tham chiếu đến Main
	}

	@FXML
	void Login(ActionEvent event) {
		String username = txtUserName.getText();
		String password = txtPass.getText();

		if (username.isEmpty() || password.isEmpty()) {
			txtUserName.clear();
			txtPass.clear();
			lblResponse.setText("Username hoặc mật khẩu bị trống");
			return;
		}

		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("Login");
			dos.writeUTF(username);
			dos.writeUTF(password);

			String message = input.readUTF();
			System.out.println("Received from server: " + message);

			if ("Login successfully".equals(message)) {
				// Lưu mã sinh viên vào UserSession hoặc truyền vào controller khác
				UserSession.getInstance().setMSSV(username);

				String root = input.readUTF();
				System.out.println("Received from server: " + root);

				if (!root.equals("0")) {
					UserSession.getInstance().setRoot(root); // Lưu root vào UserSession

					InformationController infoController = new InformationController();
					infoController.getInformationFromServer();

					// Chuyển đến giao diện chính sau khi đăng nhập thành công
					showMainInterface(event);
				} else {
					showAdminInterface(event);
				}
			} else {
				lblResponse.setText("Login Failed: " + message);
				txtUserName.clear();
				txtPass.clear();
			}

			input.close();
			dos.close();
			socket.close();

		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

	@FXML
	void Cancel(ActionEvent event) {
		txtUserName.clear();
		txtPass.clear();
		lblResponse.setText("");
	}

	@FXML
	void handleRegisterClick() {
		mainApp.showRegister(); // Chuyển về form đăng ký
	}

	@FXML
	private void showMainInterface(ActionEvent event) {
		try {
			// Tải giao diện chính từ file FXML
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Dashboard.fxml"));
			Parent mainInterface = loader.load();

			// Lấy Stage hiện tại từ event
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(mainInterface)); // Đặt scene mới cho Stage
			stage.setTitle("Giao diện chính"); // Tiêu đề cho cửa sổ
			stage.show(); // Hiển thị cửa sổ mới
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@FXML
	private void showAdminInterface(ActionEvent event) {
		try {
			// Tải giao diện chính từ file FXML
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Admin.fxml"));
			Parent adminInterface = loader.load();

			// Lấy Stage hiện tại từ event
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(adminInterface)); // Đặt scene mới cho Stage
			stage.setTitle("Giao diện admin"); // Tiêu đề cho cửa sổ
			stage.show(); // Hiển thị cửa sổ mới
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
