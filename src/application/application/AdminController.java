package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class AdminController {

	@FXML
	private TableView<User> userTable;

	@FXML
	private TableColumn<User, String> mssvColumn;

	@FXML
	private TableColumn<User, String> nameColumn;

	@FXML
	private TableColumn<User, Void> actionColumn;

	@FXML
	private TextField search;

	String mssv = UserSession.getInstance().getMSSV();
	private String currentFunction;

	public void initialize() {
		LoadAccount();
//		// Khởi tạo cột MSSV và Name với dữ liệu
		mssvColumn.setCellValueFactory(new PropertyValueFactory<>("mssv"));
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
//
//		// Cấu hình cột Actions để hiển thị các nút
//		actionColumn.setCellFactory(param -> new TableCell<User, Void>() {
//			private final Button viewButton = new Button("Xem");
//			private final Button editButton = new Button("Sửa");
//			private final Button changePassButton = new Button("Đặt lại mật khẩu");
//
//			@Override
//			protected void updateItem(Void item, boolean empty) {
//				super.updateItem(item, empty);
//
//				if (empty) {
//					setGraphic(null);
//				} else {
//					// Tạo một HBox chứa các nút và thêm vào cột
//					HBox hBox = new HBox(10, viewButton, editButton, changePassButton);
//					setGraphic(hBox);
//
//					// Cấu hình sự kiện cho các nút
//					viewButton.setOnAction(event -> viewUser(getTableRow().getItem()));
//					editButton.setOnAction(event -> editUser(getTableRow().getItem()));
//					changePassButton.setOnAction(event -> changePassword(getTableRow().getItem()));
//				}
//			}
//		});
	}

	@FXML
	public void LoadAccount() {
		currentFunction = "quanlytaikhoan";
		configureForAccounts(); // Cấu hình cột cho chức năng quản lý tài khoản
		getAllUsers();
	}

	@FXML
	public void LoadInfor() {
		currentFunction = "quanlythongtin";
		configureForInformation(); // Cấu hình cột cho chức năng đặt lại mật khẩu
		getAllUsers();
	}

	private void configureForAccounts() {
		mssvColumn.setVisible(true);
		nameColumn.setVisible(true);
		actionColumn.setText("Actions");
		actionColumn.setCellFactory(param -> new TableCell<User, Void>() {
			private final Button viewButton = new Button("Xem");
			private final Button editButton = new Button("Sửa");

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					HBox hBox = new HBox(10, viewButton, editButton);
					setGraphic(hBox);
					viewButton.setOnAction(event -> viewUser(getTableRow().getItem()));
					editButton.setOnAction(event -> editUser(getTableRow().getItem()));
				}
			}
		});
	}

	private void configureForInformation() {
		mssvColumn.setVisible(true);
		nameColumn.setVisible(false);
		actionColumn.setText("Reset Password");
		actionColumn.setCellFactory(param -> new TableCell<User, Void>() {
			private final Button changePassButton = new Button("Đặt lại mật khẩu");

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					setGraphic(changePassButton);
					changePassButton.setOnAction(event -> changePassword(getTableRow().getItem()));
				}
			}
		});
	}

	private void getAllUsers() {
		String response = "";
		try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				DataInputStream dis = new DataInputStream(socket.getInputStream())) {

			dos.writeUTF("GetAllUser");
			dos.writeUTF(mssv); // Gửi MSSV
			System.out.println(mssv);

			// Đợi phản hồi từ server
			response = dis.readUTF();
			System.out.println("Server response: " + response);
			if (response.isEmpty()) {
				userTable.setItems(FXCollections.observableArrayList()); // Xóa bảng nếu không có dữ liệu
				return;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Xử lý dữ liệu trả về
		ObservableList<User> userList = FXCollections.observableArrayList();
		String[] users = response.split(";"); // Mỗi user được phân cách bằng dấu ';'
		for (String userInfo : users) {
			String[] details = userInfo.split(","); // Mỗi thông tin user được phân cách bằng dấu ','
			if (details.length == 2) {
				String mssv = details[0];
				String name = details[1];
				userList.add(new User(mssv, name));
			}
		}
		// Gán danh sách vào bảng
		userTable.setItems(userList);
	}

	// Các phương thức để xử lý hành động
	public void viewUser(User user) {
		try {
			// Tạo FXMLLoader và load FXML của Information.fxml
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/application/Information.fxml"));
			Parent root = fxmlLoader.load();

			// Lấy đối tượng controller của Information.fxml
			InformationController informationController = fxmlLoader.getController();

			// Gửi MSSV tới InformationController để lấy thông tin
			informationController.getInformationFromServer(user.getMssv());

			// Tạo một Stage mới để hiển thị thông tin
			Stage stage = new Stage();
			stage.setTitle("Thông tin người dùng");
			stage.setScene(new Scene(root));
			stage.setOnCloseRequest(event -> {
				// Gọi lại phương thức getAllUsers() khi cửa sổ đóng
				getAllUsers();
			});
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void editUser(User user) {
		try {
			// Tạo FXMLLoader và load FXML của Information.fxml
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/application/Information.fxml"));
			Parent root = fxmlLoader.load();

			// Lấy đối tượng controller của Information.fxml
			InformationController informationController = fxmlLoader.getController();

			// Gửi MSSV tới InformationController để lấy thông tin
			informationController.getInformationFromServer(user.getMssv());

			// Tạo một Stage mới để hiển thị thông tin
			Stage stage = new Stage();
			stage.setTitle("Thông tin người dùng");
			stage.setScene(new Scene(root));

			// Lắng nghe sự kiện khi cửa sổ đóng lại
			stage.setOnCloseRequest(event -> {
				// Gọi lại phương thức getAllUsers() khi cửa sổ đóng
				getAllUsers();
			});

			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void changePassword(User user) {
		try {
			// Tạo một Socket kết nối tới server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu reset mật khẩu và MSSV
			dos.writeUTF("ResetPassword");
			dos.writeUTF(user.getMssv()); // Gửi MSSV của người dùng cần đổi mật khẩu

			// Nhận phản hồi từ server về kết quả reset mật khẩu
			String response = dis.readUTF();
			if ("Reset successfully".equals(response)) {
				String pass = dis.readUTF();

				showAlert(Alert.AlertType.INFORMATION, "Đặt lại mật khẩu thành công", "Mật khẩu mới là: " + pass);
			} else {
				showAlert(Alert.AlertType.INFORMATION, "Đặt lại mật khẩu thất bại", "Không thể reset mật khẩu!");
			}

			// Đóng kết nối
			dis.close();
			dos.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Lỗi khi gửi yêu cầu đổi mật khẩu: " + e.getMessage());
		}
	}

	@FXML
	private void Add(ActionEvent event) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/application/AddForm.fxml"));
			Parent root = fxmlLoader.load();

			// Tạo Stage mới để hiển thị form
			Stage stage = new Stage();
			stage.setTitle("Đăng ký người dùng");
			stage.setScene(new Scene(root));
			stage.setOnCloseRequest(closeEvent -> {
				// Gọi lại phương thức getAllUsers() khi cửa sổ đóng
				getAllUsers();
			});

			stage.show();

			// Hiển thị cửa sổ
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void Exit(ActionEvent event) {
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		stage.close();
	}

	@FXML
	private void searchUsers(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			String keyword = search.getText().trim().toLowerCase(); // Lấy từ khóa tìm kiếm
			ObservableList<User> filteredList = FXCollections.observableArrayList();

			// Lọc danh sách người dùng theo từ khóa
			for (User user : userTable.getItems()) {
				if (user.getMssv().toLowerCase().contains(keyword) || user.getName().toLowerCase().contains(keyword)) {
					filteredList.add(user);
				}
			}

			// Cập nhật bảng với danh sách đã lọc
			userTable.setItems(filteredList);
		}
	}

	@FXML
	public void LoadUsers(ActionEvent event) {
		getAllUsers();
	}

	private void showAlert(AlertType type, String title, String message) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

}
