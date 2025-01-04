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

	@FXML
	private TextField txtDataMax;

	private MainController mainController;
	String MSSV = UserSession.getInstance().getMSSV();

	public void setMainController(MainController mainController) {
		this.mainController = mainController;
	}

	public void getInformationFromServer() {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("GetInformation");
			dos.writeUTF(MSSV);

			String response = input.readUTF();
			System.out.println("Server response: " + response);
			String[] lines = response.split("\n");

			for (String line : lines) {
				if (line.startsWith("MSSV:")) {
					String mssvValue = line.split(": ")[1].trim();
					UserSession.getInstance().setMSSV(mssvValue);
				} else if (line.startsWith("Password:")) {
					String pwValue = line.split(": ")[1].trim();
				} else if (line.startsWith("Name:")) {
					String tenValue = line.split(": ")[1].trim();
					UserSession.getInstance().setTen(tenValue);
				} else if (line.startsWith("Class:")) {
					String lopValue = line.split(": ")[1].trim();
					UserSession.getInstance().setLop(lopValue);
				} else if (line.startsWith("Data:")) {
					String dataValue = line.split(": ")[1].trim();
					UserSession.getInstance().setData(dataValue);
				} else if (line.startsWith("UserRoleID:")) {
					String userRoleID = line.split(": ")[1].trim();
				} else if (line.startsWith("UserRoleID:")) {
					String userRoleID = line.split(": ")[1].trim();
				} else if (line.startsWith("RoleName:")) {
					String roleName = line.split(": ")[1].trim();
				} else if (line.startsWith("DataMax:")) {
		            String dataMaxValue = line.split(": ")[1].trim();
		            UserSession.getInstance().setDataMax(dataMaxValue); // Sửa ở đây
		        }	
			}

			input.close();
			dos.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Loi: " + e.getMessage());
		}
	}

	public void getInformationFromServer(String mssv) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("GetInformation");
			dos.writeUTF(mssv);

			String response = input.readUTF();
			String[] lines = response.split("\n");

			for (String line : lines) {
				if (line.startsWith("MSSV:")) {
					String mssvValue = line.split(": ")[1].trim();
					UserSession.getInstance().setMSSV(mssvValue);
				} else if (line.startsWith("Password:")) {
					String pwValue = line.split(": ")[1].trim();
				} else if (line.startsWith("Name:")) {
					String tenValue = line.split(": ")[1].trim();
					UserSession.getInstance().setTen(tenValue);
				} else if (line.startsWith("Class:")) {
					String lopValue = line.split(": ")[1].trim();
					UserSession.getInstance().setLop(lopValue);
				} else if (line.startsWith("Data:")) {
					String dataValue = line.split(": ")[1].trim();
					UserSession.getInstance().setData(dataValue);
				} else if (line.startsWith("UserRoleID:")) {
					String userRoleID = line.split(": ")[1].trim();
				} else if (line.startsWith("UserRoleID:")) {
					String userRoleID = line.split(": ")[1].trim();
				} else if (line.startsWith("RoleName:")) {
					String roleName = line.split(": ")[1].trim();
				} else if (line.startsWith("DataMax:")) {
		            String dataMaxValue = line.split(": ")[1].trim();
		            UserSession.getInstance().setDataMax(dataMaxValue); // Sửa ở đây
		        }
			}

			input.close();
			dos.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Loi: " + e.getMessage());
		}

		loadInformation();
	}

	public void loadInformation() {
		txtMSSV.setText(UserSession.getInstance().getMSSV());
		txtTen.setText(UserSession.getInstance().getTen());
		txtLop.setText(UserSession.getInstance().getLop());
		txtData.setText(UserSession.getInstance().getData());
		txtDataMax.setText(UserSession.getInstance().getDataMax());
	}

	@FXML
	void handleChangeInfo(ActionEvent event) {
		txtTen.setEditable(true);
		txtLop.setEditable(true);
		txtTen.requestFocus();

	}

	@FXML
	void handleSaveInfo(ActionEvent event) {
		String newName = txtTen.getText();
		String newClass = txtLop.getText();
		String MSSV = UserSession.getInstance().getMSSV();

		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("UpdateInformation");
			dos.writeUTF(MSSV);
			dos.writeUTF(newName);
			dos.writeUTF(newClass);
			dos.writeUTF("1");

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
					mainController.lbTen.setText(newName);
				}
			} else {
				alert.setAlertType(AlertType.ERROR);
				alert.setTitle("Thất bại");
				alert.setHeaderText(null);
				alert.setContentText("Cập nhật thông tin không thành công: " + response);
			}

			alert.showAndWait();

			input.close();
			dos.close();
			socket.close();

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
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/application/ChangePw.fxml"));
			Parent changePasswordRoot = fxmlLoader.load();

			Stage stage = new Stage();
			stage.setTitle("Đổi mật khẩu");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setScene(new Scene(changePasswordRoot));
			stage.showAndWait();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
