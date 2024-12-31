package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class InformationController {

	@FXML
	public TextField txtMSSV;

	@FXML
	public TextField txtTen;

	@FXML
	private TextField txtLop;

	@FXML
	private TextField txtData;

	private MainController mainController;
	String MSSV = UserSession.getInstance().getMSSV();

	// Setter to assign the MainController reference
	public void setMainController(MainController mainController) {
		this.mainController = mainController;
	}

	public void getInformationFromServer() {
		try {
			// Kết nối đến server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu thông tin sinh viên
			dos.writeUTF("GetInformation");
			dos.writeUTF(MSSV); // Gửi MSSV tới server

			// Nhận thông tin từ server
			String response = input.readUTF(); // Giả sử server trả về thông tin
			// Tách thông tin thành từng dòng
			String[] lines = response.split("\n");

			// Cập nhật các trường thông tin và lưu vào UserSession
			for (String line : lines) {
				if (line.startsWith("MSSV:")) {
					String mssvValue = line.split(": ")[1].trim();
					UserSession.getInstance().setMSSV(mssvValue);
				} else if (line.startsWith("Name:")) {
					String tenValue = line.split(": ")[1].trim();
					UserSession.getInstance().setTen(tenValue);
				} else if (line.startsWith("Class:")) {
					String lopValue = line.split(": ")[1].trim();
					UserSession.getInstance().setLop(lopValue);
				} else if (line.startsWith("Data:")) {
					String dataValue = line.split(": ")[1].trim();
					UserSession.getInstance().setData(dataValue);
				}
			}

			// Đóng kết nối
			input.close();
			dos.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error in getting information from server: " + e.getMessage());
		}
	}

	public void getInformationFromServer(String mssv) {
		try {
			// Kết nối đến server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu thông tin sinh viên
			dos.writeUTF("GetInformation");
			dos.writeUTF(mssv); // Gửi MSSV tới server

			// Nhận thông tin từ server
			String response = input.readUTF(); // Giả sử server trả về thông tin
			// Tách thông tin thành từng dòng
			String[] lines = response.split("\n");

			// Cập nhật các trường thông tin và lưu vào UserSession
			for (String line : lines) {
				if (line.startsWith("MSSV:")) {
					String mssvValue = line.split(": ")[1].trim();
					UserSession.getInstance().setMSSV(mssvValue);
				} else if (line.startsWith("Name:")) {
					String tenValue = line.split(": ")[1].trim();
					UserSession.getInstance().setTen(tenValue);
				} else if (line.startsWith("Class:")) {
					String lopValue = line.split(": ")[1].trim();
					UserSession.getInstance().setLop(lopValue);
				} else if (line.startsWith("Data:")) {
					String dataValue = line.split(": ")[1].trim();
					UserSession.getInstance().setData(dataValue);
				}
			}

			// Đóng kết nối
			input.close();
			dos.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error in getting information from server: " + e.getMessage());
		}

		// Sau khi nhận thông tin, cập nhật các TextField
		loadInformation();
	}

	public void loadInformation() {
		// Nếu giao diện đã khởi tạo, cập nhật các TextField
		txtMSSV.setText(UserSession.getInstance().getMSSV());
		txtTen.setText(UserSession.getInstance().getTen());
		txtLop.setText(UserSession.getInstance().getLop());
		txtData.setText(UserSession.getInstance().getData());
	}

	@FXML
	void handleChangeInfo(ActionEvent event) {
		// Cho phép chỉnh sửa trường tên và lớp
		txtTen.setEditable(true);
		txtLop.setEditable(true);

		// Đặt focus vào ô tên để người dùng dễ dàng chỉnh sửa ngay lập tức
		txtTen.requestFocus();

	}

	@FXML
	void handleSaveInfo(ActionEvent event) {
		// Lấy giá trị mới từ các trường TextField
		String newName = txtTen.getText();
		String newClass = txtLop.getText();
		String MSSV = UserSession.getInstance().getMSSV(); // Lấy MSSV từ UserSession

		try {
			// Kết nối đến server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu cập nhật thông tin
			dos.writeUTF("UpdateInformation");
			dos.writeUTF(MSSV);
			dos.writeUTF(newName);
			dos.writeUTF(newClass);
			dos.writeUTF("1");

			// Nhận kết quả từ server
			String response = input.readUTF();
			Alert alert = new Alert(AlertType.INFORMATION);
			if ("Update successfull".equals(response)) {
				alert.setTitle("Thành công");
				alert.setHeaderText(null);
				alert.setContentText("Cập nhật thông tin thành công.");
				UserSession.getInstance().setTen(newName);
				UserSession.getInstance().setLop(txtLop.getText());
				MainController mainController = new MainController();
				if (mainController != null && mainController.lbTen != null) {
					mainController.lbTen.setText(newName); // Update the label with the new name
				}
			} else {
				alert.setAlertType(AlertType.ERROR);
				alert.setTitle("Thất bại");
				alert.setHeaderText(null);
				alert.setContentText("Cập nhật thông tin không thành công: " + response);
			}

			// Hiển thị hộp thoại
			alert.showAndWait();

			// Đóng kết nối
			input.close();
			dos.close();
			socket.close();

			// Đặt lại chế độ không chỉnh sửa cho các trường TextField
			txtTen.setEditable(false);
			txtLop.setEditable(false);

		} catch (IOException e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Lỗi");
			alert.setHeaderText(null);
			alert.setContentText("Lỗi khi cập nhật thông tin: " + e.getMessage());
			alert.showAndWait();
		}
	}

	@FXML
	void showChangePasswordForm(ActionEvent event) {
		try {
			// Load FXML form đổi mật khẩu
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/application/ChangePw.fxml"));
			Parent changePasswordRoot = fxmlLoader.load();

			// Tạo một Stage mới để hiển thị form đổi mật khẩu
			Stage stage = new Stage();
			stage.setTitle("Đổi mật khẩu");
			stage.initModality(Modality.APPLICATION_MODAL); // Chặn các cửa sổ khác đến khi đóng form này
			stage.setScene(new Scene(changePasswordRoot));
			stage.showAndWait(); // Đợi form đổi mật khẩu đóng lại

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
