package application;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

public class MainController {
	private FileManager fileManager = new FileManager(); // Khởi tạo trực tiếp

	String mssv = UserSession.getInstance().getMSSV();
	String root = UserSession.getInstance().getRoot();
	private Popup currentPopup;

	@FXML
	private GridPane Centerpane;
	@FXML
	private Label lbMssv;
	@FXML
	public Label lbTen;

	@FXML
	private Button btnUp;

	@FXML
	private HBox labelPath;

	private Stack<FileNode> historyStack = new Stack<>();

	private String currentFunction; // Variable to track the current display mode

	public String currentFolderId = root;

	public String getCurrentFolderId() {
		return currentFolderId;
	}

	public void setCurrentFolderId(String currentFolderId) {
		this.currentFolderId = currentFolderId;
	}

	@FXML
	public void initialize() {
		lbMssv.setText(UserSession.getInstance().getMSSV());
		lbTen.setText(UserSession.getInstance().getTen());
		handleTatCa();
	}
//	private Main mainApp;  // Thêm biến để lưu trữ tham chiếu đến Main

	// Phương thức để thiết lập mainApp
//    public void setMainApp(Main mainApp) {
//        this.mainApp = mainApp;
//    }

	@FXML
	public void handleTatCa() {
		currentFunction = "GetAllFileName";
		resetNavigation();
		loadFiles(currentFunction);
	}

	@FXML
	public void handleCuaToi() {
		currentFunction = "GetMyFileName";
		resetNavigation();
		loadFiles(currentFunction);
	}

	@FXML
	public void handleDuocChiaSe() {
		currentFunction = "GetGuestFileName";
		resetNavigation();
		loadFiles(currentFunction);
	}

	private void resetNavigation() {
		fileManager.reset();
		historyStack.clear();
	}

	@FXML
	public void handleThongTinCaNhan() {
		try {
			// Tải giao diện thông tin cá nhân
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Information.fxml"));
			Node newContent = loader.load();

			// Lấy controller để gọi phương thức loadInformation
			InformationController controller = loader.getController();
			controller.setMainController(this);

			controller.loadInformation(); // Gọi phương thức tải thông tin

			// Xóa nội dung hiện tại và thêm nội dung mới vào Centerpane
			Centerpane.getChildren().clear();
			Centerpane.add(newContent, 0, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void handleDangXuat(ActionEvent event) {
//	    // Xóa dữ liệu phiên làm việc của người dùng
//	    UserSession.getInstance().clearSession(); // Giả sử bạn có phương thức để xóa dữ liệu phiên
//
//	    // Quay lại màn hình đăng nhập
//	    mainApp.showLogin();  // Gọi phương thức showLogin từ instance hiện tại của Main
//
//	    // Đóng cửa sổ hiện tại
//	    Stage currentStage = (Stage) lbMssv.getScene().getWindow(); // Lấy cửa sổ hiện tại
//	    currentStage.close(); // Đóng cửa sổ hiện tại
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		stage.close();
	}

	public void loadFiles(String requestType) {
		currentFolderId = root;
		labelPath.getChildren().clear();

		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu
			dos.writeUTF(requestType);
			dos.writeUTF(mssv);

			// Nhận danh sách thư mục
			String folderResponse = dis.readUTF();
			System.out.println("Folders: " + folderResponse);

			// Nhận danh sách file
			String fileResponse = dis.readUTF();
			System.out.println("Files: " + fileResponse);

//			// Reset dữ liệu cũ trong FileManager
//			fileManager.getFolders(root).clear();
//			fileManager.getFiles(root).clear();

			// Lưu trữ dữ liệu mới
			storeData(fileManager, folderResponse, fileResponse);
//			System.out.println("Folders in FileManager: " + fileManager.getFolders(root));
//			System.out.println("Files in FileManager: " + fileManager.getFiles(root));

//			System.out.println("Displaying files and folders...");
			// Tạo và hiển thị cấu trúc
			Platform.runLater(() -> {
				displayFiles(fileManager);
			});

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void storeData(FileManager fileManager, String folderResponse, String fileResponse) {
		// Phân tách và lưu trữ thư mục
		String[] folderEntries = folderResponse.split(";");
		for (String entry : folderEntries) {
			if (!entry.trim().isEmpty()) {
				String[] parts = entry.split(",");
				if (parts.length == 2) { // Đảm bảo dữ liệu đầy đủ
					String id = parts[0].trim();
					String name = parts[1].trim();
					FileNode folder = new FileNode(id, name, true); // Đây là thư mục
					String parentId = root; // Hoặc id của thư mục cha nếu có

					// Kiểm tra xem thư mục đã tồn tại chưa trước khi thêm
					if (!fileManager.isFolderExists(id)) {
						fileManager.addFolder(parentId, folder);
//						System.out.println("Added folder: " + name);
					}
				}
			}
		}

		// Phân tách và lưu trữ file
		String[] fileEntries = fileResponse.split(";");
		for (String entry : fileEntries) {
			if (!entry.trim().isEmpty()) {
				String[] parts = entry.split(",");
				if (parts.length == 2) { // Đảm bảo dữ liệu đầy đủ
					String id = parts[0].trim();
					String name = parts[1].trim();
					FileNode file = new FileNode(id, name, false); // Đây là file
					String parentId = root; // Hoặc id của thư mục cha nếu có

					// Kiểm tra xem file đã tồn tại chưa trước khi thêm
					if (!fileManager.isFileExists(id)) {
						fileManager.addFile(parentId, file);
//						System.out.println("Added file: " + name);
					}
				}
			}
		}

		// Đánh dấu thư mục là đã tải (nếu cần thiết)
		fileManager.markAsLoaded(root);

		// In ra danh sách thư mục và file đã lưu
//		System.out.println("Folders in FileManager: " + fileManager.getFolders(root));
//		System.out.println("Files in FileManager: " + fileManager.getFiles(root));
	}

	private void displayFiles(FileManager fileManager) {
		// Tạo GridPane để hiển thị thư mục và file
		GridPane contentGrid = createGridPane();
		List<FileNode> folders = fileManager.getFolders(root);
		List<FileNode> files = fileManager.getFiles(root);

		// Đảm bảo GridPane có đủ không gian để chứa các mục
		int row = 0, col = 0;

		// Hiển thị các thư mục
		for (FileNode folder : folders) {
			displayFileNode(folder, contentGrid, col, row, fileManager);
			col++;
			if (col >= 8) { // Chuyển sang hàng mới khi cột đủ
				col = 0;
				row++;
			}
		}

		// Hiển thị các file
		for (FileNode file : files) {
			displayFileNode(file, contentGrid, col, row, fileManager);
			col++;
			if (col >= 8) { // Chuyển sang hàng mới khi cột đủ
				col = 0;
				row++;
			}
		}

		// Đưa GridPane vào VBox để hiển thị và cuộn
		VBox contentBox = new VBox(20);
		contentBox.getChildren().add(contentGrid);

		// Tạo ScrollPane để có thể cuộn khi cần
		ScrollPane scrollPane = new ScrollPane(contentBox);
		scrollPane.setFitToWidth(true); // Đảm bảo ScrollPane không bị tràn ra ngoài

		// Hiển thị nội dung lên Centerpane
		Centerpane.getChildren().clear();
		Centerpane.getChildren().add(scrollPane);
	}

	private void displayFileNode(FileNode node, GridPane contentGrid, int col, int row, FileManager fileManager) {
		// Chọn icon phù hợp với loại file hoặc thư mục
		String iconPath = node.isFolder() ? "/image/File.png" : getIconPath(node.getName());
		ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
		icon.setFitWidth(90);
		icon.setFitHeight(90);

		// Tạo label với tên của node
		Label label = new Label(node.getName());
		label.setId(node.getId());

		// Tạo label nhỏ chứa dấu ⋮
		Label menuIcon = new Label("⋮");
		menuIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: gray; -fx-padding: 0 5px;");

		VBox box = new VBox(6, icon, label);
		box.setAlignment(Pos.CENTER); // Căn giữa các phần tử trong VBox

		HBox labelBox = new HBox(5, label, menuIcon);
		labelBox.setAlignment(Pos.CENTER); // Căn giữa label và dấu ⋮ trong HBox
		labelBox.setSpacing(5); // Khoảng cách giữa label và dấu ⋮

		menuIcon.setMaxWidth(20);

		box.getChildren().add(labelBox);

		// Gán sự kiện chuột trái cho icon (ImageView)
		icon.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY) { // Nhấp chuột trái vào icon
//				System.out.println("Icon clicked: " + node.getName());
				if (node.isFolder()) {
					handleFolderClick(node.getId()); // Gọi handleFolderClick với id của thư mục
				}
			}
		});

		// Gán sự kiện cho dấu ⋮
		menuIcon.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY) { // Nhấp chuột trái vào ⋮
//				System.out.println("Click vào dấu ⋮ của: " + node.getName());

				ContextMenu contextMenu;
				if (node.isFolder()) {
					contextMenu = createFolderContextMenu(node, label);
				} else {
					contextMenu = createFileContextMenu(node, label);
				}

				// Hiển thị ContextMenu tại vị trí nhấn chuột
				contextMenu.show((Node) event.getSource(), event.getScreenX(), event.getScreenY());
			}
		});

		// Thêm VBox vào GridPane
		contentGrid.add(box, col, row);
	}

	// Tạo GridPane với cấu hình sẵn
	private GridPane createGridPane() {
		GridPane gridPane = new GridPane();
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		gridPane.setPadding(new Insets(10));
		gridPane.getColumnConstraints().addAll(createColumnConstraints());
		return gridPane;
	}

	// Tạo danh sách các cột cố định
	private List<ColumnConstraints> createColumnConstraints() {
		List<ColumnConstraints> constraints = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			ColumnConstraints column = new ColumnConstraints();
			column.setPrefWidth(121.25);
			constraints.add(column);
		}
		return constraints;
	}

	public void loadFolderFromServer(String folderId, FileManager fileManager) {
		currentFolderId = folderId;

		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu tải thư mục con
			dos.writeUTF("GetAllByFolderID");
			dos.writeUTF(folderId);

			// Nhận phản hồi từ server
			String folderResponse = dis.readUTF();
			String fileResponse = dis.readUTF();

			// Lưu dữ liệu vào FileManager
			storeData(fileManager, folderId, folderResponse, fileResponse);

			// Đánh dấu folderId là đã tải
			fileManager.markAsLoaded(folderId);

			// Hiển thị dữ liệu
			displayFiles(fileManager, folderId);

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void storeData(FileManager fileManager, String folderId, String folderResponse, String fileResponse) {
		// Lưu dữ liệu thư mục
		String[] folderEntries = folderResponse.split(";");
		for (String entry : folderEntries) {
			if (!entry.trim().isEmpty()) {
				String[] parts = entry.split(",");
				String id = parts[0].trim();
				String name = parts[1].trim();
				FileNode folder = new FileNode(id, name, true);

				if (!fileManager.isFolderExists(id)) { // Kiểm tra thư mục tồn tại
					fileManager.addFolder(folderId, folder);
					fileManager.markAsLoaded(id); // Đánh dấu là đã tải
				}
			}
		}

		// Lưu dữ liệu file
		String[] fileEntries = fileResponse.split(";");
		for (String entry : fileEntries) {
			if (!entry.trim().isEmpty()) {
				String[] parts = entry.split(",");
				String id = parts[0].trim();
				String name = parts[1].trim();
				FileNode file = new FileNode(id, name, false);

				if (!fileManager.isFileExists(id)) { // Kiểm tra tệp tồn tại
					fileManager.addFile(folderId, file);
					fileManager.markAsLoaded(id); // Đánh dấu là đã tải
				}
			}
		}
	}

	private void displayFiles(FileManager fileManager, String folderId) {
		GridPane contentGrid = createGridPane();
		List<FileNode> folders = fileManager.getFolders(folderId);
		List<FileNode> files = fileManager.getFiles(folderId);

		int row = 0, col = 0;

		for (FileNode folder : folders) {
			displayFileNode(folder, contentGrid, col, row, fileManager);
			col++;
			if (col >= 8) { // Chuyển sang hàng mới khi cột đủ
				col = 0;
				row++;
			}
		}

		for (FileNode file : files) {
			displayFileNode(file, contentGrid, col, row, fileManager);
			col++;
			if (col >= 8) { // Chuyển sang hàng mới khi cột đủ
				col = 0;
				row++;
			}
		}

		// Đưa GridPane vào VBox để hiển thị và cuộn
		VBox contentBox = new VBox(20);
		contentBox.getChildren().add(contentGrid);

		// Tạo ScrollPane để có thể cuộn khi cần
		ScrollPane scrollPane = new ScrollPane(contentBox);
		scrollPane.setFitToWidth(true); // Đảm bảo ScrollPane không bị tràn ra ngoài

		// Hiển thị nội dung lên Centerpane
		Centerpane.getChildren().clear();
		Centerpane.getChildren().add(scrollPane);
	}

	public void handleFolderClick(String folderId) {
		FileNode currentFolder = findFolderById(folderId);
		if (currentFolder != null) {
			currentFolderId = folderId; // Cập nhật thư mục hiện tại
			historyStack.push(currentFolder); // Lưu vào lịch sử

			// Kiểm tra nếu thư mục chưa được tải
			if (fileManager.isClicked(folderId)) {
//				System.out.println("đã được click.");
				displayFiles(fileManager, folderId);
			} else {
//				System.out.println("chưa được click.");
				fileManager.markAsClicked(folderId); // Đánh dấu thư mục đã click
				loadFolderFromServer(folderId, fileManager);
			}

			// Cập nhật breadcrumb hoặc UI nếu cần
			updateBreadcrumb();
		}
	}

	// Tìm FileNode trong danh sách hiện tại
	private FileNode findFolderById(String folderId) {
		List<FileNode> folders = fileManager.getFolders(currentFolderId);
		for (FileNode folder : folders) {
			if (folder.getId().equals(folderId)) {
				return folder;
			}
		}
		return null;
	}

	private void updateBreadcrumb() {
		// Xóa các phần tử hiện có
		labelPath.getChildren().clear();

		// Duyệt qua lịch sử để tạo đường dẫn
		List<FileNode> breadcrumb = new ArrayList<>(historyStack);

		for (int i = 0; i < breadcrumb.size(); i++) {
			FileNode folder = breadcrumb.get(i);

			// Tạo Hyperlink cho mỗi thư mục
			Hyperlink link = new Hyperlink(folder.getName());
			link.setOnAction(event -> handleBreadcrumbClick(folder.getId()));

			// Tùy chỉnh style: font size lớn hơn, không gạch chân, và đổi màu chữ
			link.setStyle("-fx-font-size: 14px; -fx-text-fill: black; -fx-underline: false;");

			// Thêm Hyperlink vào labelPath
			labelPath.getChildren().add(link);

			// Thêm ">" nếu không phải thư mục cuối
			if (i < breadcrumb.size() - 1) {
				Label separator = new Label(">");
				separator.setStyle("-fx-font-size: 16px;"); // Đồng bộ kích thước với Hyperlink
				labelPath.getChildren().add(separator);
			}
		}
	}

	private void handleBreadcrumbClick(String folderId) {
		// Xóa lịch sử phía sau thư mục được nhấn
		while (!historyStack.isEmpty() && !historyStack.peek().getId().equals(folderId)) {
			historyStack.pop();
		}

		// Cập nhật thư mục hiện tại và hiển thị
		currentFolderId = folderId;
		displayFiles(fileManager, currentFolderId);
		updateBreadcrumb();
	}

	// Hàm để xác định đường dẫn icon dựa trên đuôi file
	private String getIconPath(String fileName) {
		if (fileName.endsWith(".pdf")) {
			return "/image/icon/icon/pdf128.png";
		} else if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".txt")) {
			return "/image/icon/icon/google-docs128.png";
		} else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx") || fileName.endsWith(".csv")) {
			return "/image/icon/icon/sheets128.png";
		} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")
				|| fileName.endsWith(".gif")) {
			return "/image/icon/icon/photo128.png";
		} else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
			return "/image/icon/icon/powerpoint128.png";
		} else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
			return "/image/icon/icon/zip128.png";
		} else {
			return "/image/File.png";
		}
	}

	public void handleShowUploadOptions() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/UploadOptions.fxml"));
			VBox uploadOptionsLayout = loader.load();

			UploadOptionsController controller = loader.getController();
			controller.setMainController(this);
			controller.setFileManager(this.fileManager); // Truyền đối tượng fileManager

			Popup popup = new Popup();
			popup.getContent().add(uploadOptionsLayout);

			// Keep reference to the popup
			currentPopup = popup;

			Point2D buttonLocation = btnUp.localToScreen(btnUp.getLayoutBounds().getMinX(),
					btnUp.getLayoutBounds().getMinY());

			popup.setX(buttonLocation.getX() + 20);
			popup.setY(buttonLocation.getY() - uploadOptionsLayout.getPrefHeight() + 60);

			popup.show(btnUp.getScene().getWindow());

			// Close the popup when clicking outside
			btnUp.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				if (!popup.getContent().contains(event.getTarget())) {
					hidePopup(); // Hide popup if clicked outside
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Method to hide the popup
	public void hidePopup() {
		if (currentPopup != null) {
			currentPopup.hide(); // Hide the popup
		}
	}

//	private ContextMenu createFileContextMenu(FileNode fileNode, Label fileLabel) {
//		ContextMenu contextMenu = new ContextMenu();
//
//		// Tùy chọn Download
//		MenuItem downloadItem = new MenuItem("Download");
//		downloadItem.setOnAction(event -> downloadFile(fileNode.getId(), (Stage) fileLabel.getScene().getWindow()));
//		contextMenu.getItems().add(downloadItem);
//
//		// Tùy chọn Share
//		MenuItem shareItem = new MenuItem("Share");
//		shareItem.setOnAction(event -> {
//			TextInputDialog dialog = new TextInputDialog();
//			dialog.setTitle("Nhập MSSV");
//			dialog.setHeaderText("Chia sẻ file với người khác");
//			dialog.setContentText("Nhập MSSV của người nhận:");
//			Optional<String> result = dialog.showAndWait();
//			result.ifPresent(mssvgest -> shareFile(fileNode.getId(), mssvgest));
//		});
//		contextMenu.getItems().add(shareItem);
//
//		// Tùy chọn List Shared
//		MenuItem listSharedItem = new MenuItem("List Shared");
//		listSharedItem.setOnAction(event -> listSharedUsers(fileNode.getId()));
//		contextMenu.getItems().add(listSharedItem);
//
//		// Tùy chọn Remove
//		MenuItem removeItem = new MenuItem("Remove");
//		removeItem.setOnAction(event -> {
//			removeFile(fileNode.getId()); // Gửi yêu cầu xóa file lên server
//		});
//		contextMenu.getItems().add(removeItem);
//
//		return contextMenu;
//	}
	private ContextMenu createFileContextMenu(FileNode fileNode, Label fileLabel) {
		ContextMenu contextMenu = new ContextMenu();
		contextMenu.setStyle("-fx-font-size: 14px;");

		// Kiểm tra nếu currentFunction là "GetGuestFileName"
		if ("GetGuestFileName".equals(currentFunction)) {
			// Chỉ thêm tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(event -> downloadFile(fileNode.getId(), (Stage) fileLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);
		} else {
			// Tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(event -> downloadFile(fileNode.getId(), (Stage) fileLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);

			// Tùy chọn Share
			MenuItem shareItem = new MenuItem("Share");
			shareItem.setOnAction(event -> {
				TextInputDialog dialog = new TextInputDialog();
				dialog.setTitle("Nhập MSSV");
				dialog.setHeaderText("Chia sẻ file với người khác");
				dialog.setContentText("Nhập MSSV của người nhận:");
				Optional<String> result = dialog.showAndWait();
				result.ifPresent(mssvgest -> shareFile(fileNode.getId(), mssvgest));
			});
			contextMenu.getItems().add(shareItem);

			// Tùy chọn List Shared
			MenuItem listSharedItem = new MenuItem("List Shared");
			listSharedItem.setOnAction(event -> listSharedUsers(fileNode.getId()));
			contextMenu.getItems().add(listSharedItem);

			// Tùy chọn List Shared
			MenuItem renameItem = new MenuItem("Rename");
			renameItem.setOnAction(event -> renamefile(fileNode, fileLabel));
			contextMenu.getItems().add(renameItem);

			// Tùy chọn Remove
			MenuItem removeItem = new MenuItem("Remove");
			removeItem.setOnAction(event -> removeFile(fileNode.getId())); // Gửi yêu cầu xóa file lên server
			contextMenu.getItems().add(removeItem);
		}

		return contextMenu;
	}

	private ContextMenu createFolderContextMenu(FileNode folderNode, Label folderLabel) {
		ContextMenu contextMenu = new ContextMenu();
		contextMenu.setStyle("-fx-font-size: 14px;");

		// Kiểm tra nếu currentFunction là "GetGuestFileName"
		if ("GetGuestFileName".equals(currentFunction)) {
			// Chỉ thêm tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(
					event -> downloadFolder(folderNode.getId(), (Stage) folderLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);
		} else {
			// Tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(
					event -> downloadFolder(folderNode.getId(), (Stage) folderLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);

			// Tùy chọn Share
			MenuItem shareItem = new MenuItem("Share");
			shareItem.setOnAction(event -> {
				TextInputDialog dialog = new TextInputDialog();
				dialog.setTitle("Nhập MSSV");
				dialog.setHeaderText("Chia sẻ thư mục với người khác");
				dialog.setContentText("Nhập MSSV của người nhận:");
				Optional<String> result = dialog.showAndWait();
				result.ifPresent(mssvgest -> shareFolder(folderNode.getId(), mssvgest));
			});
			contextMenu.getItems().add(shareItem);

			// Tùy chọn List Shared
			MenuItem listSharedItem = new MenuItem("List Shared");
			listSharedItem.setOnAction(event -> listSharedUsersfolder(folderNode.getId()));
			contextMenu.getItems().add(listSharedItem);

			// Tùy chọn List Shared
			MenuItem renameItem = new MenuItem("Rename");
			renameItem.setOnAction(event -> renameFolder(folderNode, folderLabel));
			contextMenu.getItems().add(renameItem);

			// Tùy chọn Remove
			MenuItem removeItem = new MenuItem("Remove");
			removeItem.setOnAction(event -> removeFolder(folderNode.getId())); // Gửi yêu cầu xóa thư mục lên server
			contextMenu.getItems().add(removeItem);
		}

		return contextMenu;
	}

	private void showAlert(Alert.AlertType alertType, String title, String contentText) {
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(title);
		alert.setContentText(contentText);
		alert.showAndWait();
	}

	private void renamefile(FileNode fileNode, Label fileLabel) {
		// Tạo một TextField để người dùng nhập tên mới
		TextField renameField = new TextField(fileLabel.getText()); // Gán giá trị ban đầu là tên hiện tại
		renameField.setStyle("-fx-font-size: 14px;");

		// Gán sự kiện khi người dùng nhấn Enter sau khi nhập tên mới
		renameField.setOnAction(event -> {
			String newFileName = renameField.getText();

			// Cập nhật tên tệp ngay trên UI
			fileLabel.setText(newFileName);
			fileNode.setName(newFileName); // Cập nhật đối tượng FileNode

			// Gửi yêu cầu đổi tên lên server
			sendRenameRequest(fileNode.getId(), newFileName);

			// Thay thế TextField bằng Label trở lại
			GridPane gridPane = (GridPane) fileLabel.getParent(); // Lấy GridPane làm cha của label
			gridPane.getChildren().set(gridPane.getChildren().indexOf(renameField), fileLabel); // Thay TextField bằng
																								// Label
		});

		// Thêm TextField vào GridPane (thay thế Label tạm thời)
		GridPane gridPane = (GridPane) fileLabel.getParent(); // Lấy GridPane làm cha của label
		int colIndex = GridPane.getColumnIndex(fileLabel);
		int rowIndex = GridPane.getRowIndex(fileLabel);
		gridPane.add(renameField, colIndex, rowIndex); // Thêm TextField vào đúng vị trí
		gridPane.getChildren().remove(fileLabel); // Loại bỏ Label khỏi GridPane
	}

	private void renameFolder(FileNode fileNode, Label fileLabel) {
		// Tạo một TextField để người dùng nhập tên mới
		TextField renameField = new TextField(fileLabel.getText()); // Gán giá trị ban đầu là tên hiện tại
		renameField.setStyle("-fx-font-size: 14px;");

		// Gán sự kiện khi người dùng nhấn Enter sau khi nhập tên mới
		renameField.setOnAction(event -> {
			String newFileName = renameField.getText();

			// Cập nhật tên tệp ngay trên UI
			fileLabel.setText(newFileName);
			fileNode.setName(newFileName); // Cập nhật đối tượng FileNode

			// Gửi yêu cầu đổi tên lên server
			renameFolderOnServer(fileNode.getId(), newFileName);

			// Thay thế TextField bằng Label trở lại
			Parent parent = fileLabel.getParent();
			if (parent instanceof GridPane) {
				GridPane gridPane = (GridPane) parent;
				gridPane.getChildren().set(gridPane.getChildren().indexOf(renameField), fileLabel); // Thay TextField
																									// bằng Label
			} else if (parent instanceof HBox) {
				// Xử lý HBox (nếu cần)
			}
		});

		// Thêm TextField vào GridPane (thay thế Label tạm thời)
		Parent parent = fileLabel.getParent();
		if (parent instanceof GridPane) {
			GridPane gridPane = (GridPane) parent;
			int colIndex = GridPane.getColumnIndex(fileLabel); // Sử dụng phương thức static
			int rowIndex = GridPane.getRowIndex(fileLabel); // Sử dụng phương thức static

			// Đảm bảo thêm TextField vào đúng vị trí trong GridPane
			gridPane.add(renameField, colIndex, rowIndex); // Thêm TextField vào đúng vị trí
			gridPane.getChildren().remove(fileLabel); // Loại bỏ Label khỏi GridPane
		} else if (parent instanceof HBox) {
			// Nếu bố cục HBox, thêm logic cần thiết vào đây
			// Ví dụ: Thêm TextField vào HBox, thay thế Label
		}

		// Đảm bảo layout được cập nhật lại nếu cần
		if (parent != null) {
			parent.requestLayout();
		}
	}

	private void renameFolderOnServer(String id, String newName) {
		try {
			// Tạo kết nối tới server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF("ChangeFolderName");
			dos.writeUTF(mssv); // Gửi MSSV
			dos.writeUTF(id); // Gửi ID (FileID hoặc FolderID)
			dos.writeUTF(newName); // Gửi tên mới

			// Đợi phản hồi từ server
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			String response = dis.readUTF();
			System.out.println("Server response: " + response);

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendRenameRequest(String id, String newName) {
		try {
			// Tạo kết nối tới server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF("RenameFile");
			dos.writeUTF(mssv); // Gửi MSSV
			dos.writeUTF(id); // Gửi ID (FileID hoặc FolderID)
			dos.writeUTF(newName); // Gửi tên mới

			// Đợi phản hồi từ server
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			String response = dis.readUTF();
			System.out.println("Server response: " + response);

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void downloadFile(String fileId, Stage stage) {
		// Hiển thị hộp thoại xác nhận tải xuống
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Download File");
		alert.setHeaderText("Download File");
		alert.setContentText("Do you want to download this file?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			// Hiển thị DirectoryChooser để người dùng chọn thư mục lưu file
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Chọn thư mục để lưu file");
			File selectedDirectory = directoryChooser.showDialog(stage);

			// Kiểm tra nếu người dùng không chọn thư mục nào
			if (selectedDirectory == null) {
				System.out.println("Không có thư mục nào được chọn. Hủy tải file.");
				return;
			}

			// Gửi yêu cầu tải xuống đến server và lưu vào thư mục đã chọn
			try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
					DataInputStream dis = new DataInputStream(socket.getInputStream())) {

				// Gửi yêu cầu tải file và ID file đến server
				dos.writeUTF("DownloadFile"); // Gửi yêu cầu tải file
				dos.writeUTF(fileId); // Gửi ID file

				// Nhận và lưu file từ server vào thư mục đã chọn
				receiveFile(dis, selectedDirectory);

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Lỗi khi tải file: " + e.getMessage());
			}
		}
	}

	private void receiveFile(DataInputStream dis, File selectedDirectory) throws IOException {
		String fileName = dis.readUTF(); // Lấy tên file ZIP từ server
		File zipFile = new File(selectedDirectory, "temp_" + fileName + ".zip"); // Đường dẫn tạm trong thư mục đã chọn

		try (FileOutputStream fos = new FileOutputStream(zipFile);
				BufferedOutputStream bos = new BufferedOutputStream(fos)) {

			byte[] buffer = new byte[4096];
			int bytesRead;

			// Đọc và ghi dữ liệu vào file ZIP tạm
			while ((bytesRead = dis.read(buffer)) > 0) {
				bos.write(buffer, 0, bytesRead);
			}
			bos.flush();
			System.out.println("File ZIP downloaded successfully to: " + zipFile.getAbsolutePath());

			// Kiểm tra nếu file tải về có kích thước 0 (lỗi)
			if (zipFile.length() == 0) {
				showAlert(Alert.AlertType.ERROR, "Lỗi tải file", "Tải File thất bại!");
				return;
			}

			// Giải nén file ZIP vào thư mục đã chọn
			extractZipFile(zipFile, selectedDirectory.getAbsolutePath() + File.separator);

			// Hiển thị thông báo tải thành công
			showAlert(Alert.AlertType.INFORMATION, "Tải file thành công", "File đã được tải xuống thành công!");

		} finally {
			// Xóa file ZIP tạm sau khi giải nén
			if (zipFile.exists()) {
				zipFile.delete();
			}
		}
	}

	private void extractZipFile(File zipFile, String destDir) {
		if (!zipFile.exists()) {
			System.out.println("Lỗi: File ZIP không tồn tại: " + zipFile.getAbsolutePath());
			return;
		}

		File destDirectory = new File(destDir);
		if (!destDirectory.exists()) {
			destDirectory.mkdirs();
		}

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			byte[] buffer = new byte[4096];

			while ((entry = zis.getNextEntry()) != null) {
				File newFile = new File(destDir, entry.getName());
				// Kiểm tra nếu file đã tồn tại, thay đổi tên file
				if (newFile.exists()) {
					String fileNameWithoutExtension = getFileNameWithoutExtension(newFile);
					String extension = getFileExtension(newFile);
					int counter = 1;
					while (newFile.exists()) {
						newFile = new File(destDir, fileNameWithoutExtension + "_" + counter + extension);
						counter++;
					}
				}

				System.out.println("Extracting: " + newFile.getAbsolutePath());

				if (entry.isDirectory()) {
					newFile.mkdirs();
				} else {
					if (!newFile.getParentFile().exists()) {
						newFile.getParentFile().mkdirs();
					}

					try (FileOutputStream fos = new FileOutputStream(newFile);
							BufferedOutputStream bos = new BufferedOutputStream(fos)) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							bos.write(buffer, 0, len);
						}
						bos.flush();
					}
				}
				zis.closeEntry();
			}

			System.out.println("File ZIP đã được giải nén thành công tại: " + destDir);

		} catch (IOException e) {
			System.out.println("Lỗi khi giải nén file ZIP: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Phương thức để lấy tên file mà không có phần mở rộng
	private String getFileNameWithoutExtension(File file) {
		String fileName = file.getName();
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex > 0) {
			return fileName.substring(0, dotIndex);
		} else {
			return fileName;
		}
	}

	// Phương thức để lấy phần mở rộng của file
	private String getFileExtension(File file) {
		String fileName = file.getName();
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex > 0) {
			return fileName.substring(dotIndex);
		} else {
			return "";
		}
	}

	private void downloadFolder(String folderId, Stage stage) {
		// Hiển thị hộp thoại xác nhận tải xuống
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Download Folder");
		alert.setHeaderText("Download Folder");
		alert.setContentText("Do you want to download this folder?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			// Hiển thị DirectoryChooser để người dùng chọn thư mục lưu folder
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Chọn thư mục để lưu folder");
			File selectedDirectory = directoryChooser.showDialog(stage);

			if (selectedDirectory == null) {
				System.out.println("Không có thư mục nào được chọn. Hủy tải folder.");
				return;
			}

			try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
					DataInputStream dis = new DataInputStream(socket.getInputStream())) {

				// Gửi yêu cầu tải folder và ID folder đến server
				dos.writeUTF("DownloadFolder");
				dos.writeUTF(folderId);

				// Nhận và lưu file ZIP từ server
				receiveFolder(dis, selectedDirectory);

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Lỗi khi tải thư mục: " + e.getMessage());
			}
		}
	}

	public void receiveFolder(DataInputStream dis, File selectedDirectory) throws IOException {
		try {
			// Nhận tên file và kích thước file từ server
			String fileName = dis.readUTF(); // Nhận tên file từ server
			String fileSizeStr = dis.readUTF(); // Nhận kích thước file dưới dạng String

			long fileSize = Long.parseLong(fileSizeStr);
			// Tạo file tại thư mục đích
			File file = new File(selectedDirectory, fileName); // Đảm bảo file có đuôi .zip

			try (FileOutputStream fos = new FileOutputStream(file);
					BufferedOutputStream bos = new BufferedOutputStream(fos)) {

				byte[] buffer = new byte[8192]; // Dùng bộ đệm để đọc file
				long totalBytesRead = 0;
				int bytesRead;

				// Đọc dữ liệu từ server và ghi vào file
				while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
					bos.write(buffer, 0, bytesRead);
					totalBytesRead += bytesRead;
				}

				bos.flush();
				System.out.println("File đã được tải xuống thành công: " + file.getAbsolutePath());
				showAlert(Alert.AlertType.INFORMATION, "Tải folder thành công", "Folder đã được tải xuống thành công!");

			} catch (IOException e) {
				System.out.println("Lỗi khi lưu file: " + e.getMessage());
				throw e;
			}
		} catch (IOException e) {
			System.out.println("Lỗi khi nhận file: " + e.getMessage());
			throw e;
		}
	}

	private void shareFile(String fileId, String mssvgest) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu chia sẻ đến server
			dos.writeUTF("AddGuest"); // Gửi lệnh SHARE
			dos.writeUTF(mssv);
			dos.writeUTF(mssvgest); // Gửi MSSV của người muốn chia sẻ
			dos.writeUTF(fileId); // Gửi ID file

			// Nhận phản hồi từ server
			String response = dis.readUTF();
			System.out.println("Server response: " + response);

			dis.close();
			dos.close();
			socket.close();

			// Hiển thị thông báo cho người dùng
			if (response.equals("Share file successfully")) {
				showAlert(Alert.AlertType.INFORMATION, "Chia sẻ thành công", "File đã được chia sẻ với " + mssv);
			} else {
				showAlert(Alert.AlertType.ERROR, "Chia sẻ thất bại", "Không thể chia sẻ file.");
			}
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Lỗi chia sẻ", "Lỗi khi kết nối đến server: " + e.getMessage());
		}
	}

	private void shareFolder(String folderId, String mssvgest) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu chia sẻ đến server
			dos.writeUTF("ShareFolder"); // Gửi lệnh chia sẻ thư mục
			dos.writeUTF(mssv); // Gửi MSSV của người chia sẻ
			dos.writeUTF(folderId); // Gửi ID thư mục
			dos.writeUTF(mssvgest); // Gửi MSSV của người nhận

			// Nhận phản hồi từ server
			String response = dis.readUTF();
			System.out.println("Server response: " + response);

			dis.close();
			dos.close();
			socket.close();

			// Hiển thị thông báo cho người dùng
			if (response.equals("Share folder successfully")) {
				showAlert(Alert.AlertType.INFORMATION, "Chia sẻ thành công", "Thư mục đã được chia sẻ với " + mssvgest);
			} else {
				showAlert(Alert.AlertType.ERROR, "Chia sẻ thất bại", "Không thể chia sẻ thư mục.");
			}
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Lỗi chia sẻ", "Lỗi khi kết nối đến server: " + e.getMessage());
		}
	}

//Xóa
	private void removeFile(String fileId) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu xóa file tới server
			dos.writeUTF("DeleteFile");
			dos.writeUTF(mssv);
			dos.writeUTF(fileId);

			// Nhận phản hồi từ server
			String response = dis.readUTF();
			System.out.println(response);

			if (response.equals("Delete file successfully")) {
				showAlert(Alert.AlertType.INFORMATION, "Xóa file thành công", "File đã được xóa thành công.");
			} else {
				showAlert(Alert.AlertType.ERROR, "Xóa file thất bại", "Không thể xóa file.");
			}

			if (response.equals("Delete file successfully")) {
				if (!currentFolderId.equals(root)) {
					fileManager.removeFile(currentFolderId, fileId);
					loadFolderFromServer(currentFolderId, fileManager);
				} else {
					fileManager.removeFile(currentFolderId, fileId);
					loadFiles(currentFunction);
				}
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error while removing file: " + e.getMessage());
		}
	}

//	private void removeFile(String fileId) {
//		System.out.println("Attempting to delete file. File ID: " + fileId);
//		System.out.println("Sending MSSV: " + mssv); // Print MSSV being sent
//
//		// Ensure the operation is done asynchronously
//		new Thread(() -> {
//			try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
//					DataInputStream dis = new DataInputStream(socket.getInputStream());
//					DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
//
//				// Send the request to delete the file
//				dos.writeUTF("DeleteFile");
//				dos.writeUTF(mssv); // Send MSSV of the user
//				dos.writeUTF(fileId); // Send the file ID
//
//				System.out.println("Sent MSSV: " + mssv); // Confirm that MSSV is sent
//				System.out.println("Sent File ID: " + fileId); // Confirm that File ID is sent
//
//				// Get the response from the server
//				String response = dis.readUTF();
//				System.out.println("Server response: " + response);
//
//				// Handle the response
//				if (response.equals("Delete file successfully")) {
//					// File deletion was successful
//					Platform.runLater(() -> {
//						showAlert(Alert.AlertType.INFORMATION, "Xóa file thành công", "File đã được xóa thành công.");
//						// Remove the file from the FileManager
//						if (!currentFolderId.equals(root)) {
//							fileManager.removeFile(currentFolderId, fileId);
//							loadFolderFromServer(currentFolderId, fileManager);
//						} else {
//							loadFiles(currentFunction);
//						}
//					});
//				} else {
//					// File deletion failed
//					Platform.runLater(() -> {
//						showAlert(Alert.AlertType.ERROR, "Xóa file thất bại", "Không thể xóa file.");
//					});
//				}
//
//			} catch (IOException e) {
//				// Handle the exception
//				e.printStackTrace();
//				Platform.runLater(() -> {
//					showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Đã xảy ra lỗi khi kết nối với server.");
//				});
//			}
//		}).start(); // Run the file deletion operation in a separate thread
//	}

	private void removeFolder(String folderId) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("DeleteFolder");
			dos.writeUTF(mssv);
			dos.writeUTF(folderId);

			String response = dis.readUTF();
			System.out.println(response);

			if (response.equals("Delete Folder successfully")) {
				showAlert(Alert.AlertType.INFORMATION, "Xóa thư mục thành công", "Thư mục đã được xóa thành công.");
			} else {
				showAlert(Alert.AlertType.ERROR, "Xóa thư mục thất bại", "Không thể xóa thư mục.");
			}

			if (response.equals("Delete Folder successfully")) {
				if (!currentFolderId.equals(root)) {
					fileManager.removeFolder(currentFolderId, folderId);
					loadFolderFromServer(currentFolderId, fileManager);
				} else {
					fileManager.removeFolder(currentFolderId, folderId);
					loadFiles(currentFunction);
				}
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error while removing folder: " + e.getMessage());
		}
	}
//	private void removeFolder(String folderId) {
//
//		new Thread(() -> {
//			try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
//					DataInputStream dis = new DataInputStream(socket.getInputStream());
//					DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
//
//				dos.writeUTF("DeleteFolder");
//				dos.writeUTF(mssv);
//				dos.writeUTF(folderId);
//
//				String response = dis.readUTF();
//				System.out.println("Server response: " + response);
//
//				if (response.equals("Delete Folder successfully.")) {
//					Platform.runLater(() -> {
//						showAlert(Alert.AlertType.INFORMATION, "Xóa folder  thành công", "Folder đã được xóa thành công.");
//						if (!currentFolderId.equals(root)) {
//							fileManager.removeFolder(currentFolderId, folderId);
//							loadFolderFromServer(currentFolderId, fileManager);
//						} else {
//							loadFiles(currentFunction);
//						}
//					});
//				} else {
//					Platform.runLater(() -> {
//						showAlert(Alert.AlertType.ERROR, "Xóa folder thất bại", "Không thể xóa folder.");
//					});
//				}
//
//			} catch (IOException e) {
//				e.printStackTrace();
//				Platform.runLater(() -> {
//					showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Đã xảy ra lỗi khi kết nối với server.");
//				});
//			}
//		}).start(); // Run the folder deletion operation in a separate thread
//	}

	private void listSharedUsers(String fileId) {
		try {
			// Tạo kết nối đến server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu lấy danh sách người dùng đã chia sẻ file
			dos.writeUTF("GetAllFileGuest");
			dos.writeUTF(mssv);
			dos.writeUTF(fileId);

			// Nhận danh sách người dùng đã chia sẻ file
			String response = dis.readUTF();

			if (response.equals("ERR")) {
				System.out.println("Lỗi khi tìm người xem file.");
			} else {
				// Tạo ListView hiển thị danh sách người dùng đã chia sẻ
				ListView<String> sharedUsersList = new ListView<>();

				// Tách danh sách người dùng và thêm vào ListView
				String[] users = response.split(",");
				sharedUsersList.getItems().addAll(users);

				// Tạo Dialog để hiển thị ListView
				Dialog<String> dialog = new Dialog<>();
				dialog.setTitle("Danh sách được chia sẽ");
				dialog.getDialogPane().setContent(sharedUsersList);
				dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
				dialog.showAndWait();
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Lỗi khi giao tiếp với server: " + e.getMessage());
		}
	}

	private void listSharedUsersfolder(String folderid) {
		try {
			// Tạo kết nối đến server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu lấy danh sách người dùng đã chia sẻ file
			dos.writeUTF("GetAllFolderGuest");
			dos.writeUTF(mssv);
			dos.writeUTF(folderid);

			// Nhận danh sách người dùng đã chia sẻ file
			String response = dis.readUTF();

			if (response.equals("ERR")) {
				System.out.println("Lỗi khi tìm người xem file.");
			} else {
				// Tạo ListView hiển thị danh sách người dùng đã chia sẻ
				ListView<String> sharedUsersList = new ListView<>();

				// Tách danh sách người dùng và thêm vào ListView
				String[] users = response.split(",");
				sharedUsersList.getItems().addAll(users);

				// Tạo Dialog để hiển thị ListView
				Dialog<String> dialog = new Dialog<>();
				dialog.setTitle("Danh sách được chia sẽ");
				dialog.getDialogPane().setContent(sharedUsersList);
				dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
				dialog.showAndWait();
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Lỗi khi giao tiếp với server: " + e.getMessage());
		}
	}

//	@FXML
//	public void handleShowUploadOptions() {
//		try {
//			// Tải file FXML
//			FXMLLoader loader = new FXMLLoader(getClass().getResource("UploadOptions.fxml"));
//			VBox uploadOptionsLayout = loader.load();
//
//			// Lấy controller của FXML và thiết lập tham chiếu tới MainController
//			UploadOptionsController controller = loader.getController();
//			controller.setMainController(this);
//
//			// Tạo và hiển thị dialog
//			Stage dialogStage = new Stage();
//			dialogStage.initModality(Modality.APPLICATION_MODAL);
//			dialogStage.setScene(new Scene(uploadOptionsLayout));
//			dialogStage.showAndWait();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

//	@FXML
//	public void handleUploadFile() {
//		FileChooser fileChooser = new FileChooser();
//		fileChooser.setTitle("Chọn file để tải lên");
//		File selectedFile = fileChooser.showOpenDialog(null);
//
//		if (selectedFile != null) {
//			// Compress the file before sending it
//			String zipFilePath = zipFile(selectedFile.getAbsolutePath()); // Pass the selected file for zipping
//			if (zipFilePath != null) {
//				sendFile(zipFilePath, selectedFile.getName()); // Send the compressed file with its name
//			}
//		}
//	}

//	private String zipFile(String filePath) {
//		// Lấy tên file mà không có đường dẫn
//		String fileName = new File(filePath).getName();
//
//		// Kiểm tra nếu file đã có đuôi .zip thì không thêm nữa
//		String zipFilePath = filePath.endsWith(".zip") ? filePath : filePath + ".zip";
//
//		try (FileOutputStream fos = new FileOutputStream(zipFilePath);
//				ZipOutputStream zos = new ZipOutputStream(fos);
//				FileInputStream fis = new FileInputStream(filePath)) {
//
//			ZipEntry zipEntry = new ZipEntry(fileName); // Lấy tên file mà không có đường dẫn
//			zos.putNextEntry(zipEntry);
//
//			byte[] buffer = new byte[1024];
//			int len;
//			while ((len = fis.read(buffer)) > 0) {
//				zos.write(buffer, 0, len);
//			}
//
//			zos.closeEntry();
//			System.out.println("File đã được nén thành công: " + zipFilePath);
//
//		} catch (IOException e) {
//			System.out.println("Lỗi khi nén file: " + e.getMessage());
//			return null; // Return null if there's an error in zipping
//		}
//		return zipFilePath;
//	}

//	private void sendFile(String zipFilePath, String fileName) {
//		try {
//			File file = new File(zipFilePath);
//			try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
//					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//					FileInputStream fis = new FileInputStream(file);
//					BufferedOutputStream bos = new BufferedOutputStream(dos)) {
//
//				dos.writeUTF("UploadFile"); // Gửi loại yêu cầu
//				dos.writeUTF(mssv); // Gửi MSSV
//				dos.writeUTF(fileName); // Gửi tên file
//
//				// Gửi kích thước file tính bằng KB
//				double fileSizeInKB = (double) file.length() / 1024;
//				double roundedFileSizeInKB = Math.round(fileSizeInKB); // Làm tròn đến hàng đơn vị
//				dos.writeUTF(String.valueOf(roundedFileSizeInKB));
//
//				// Gửi dữ liệu file ZIP
//				byte[] buffer = new byte[1024];
//				int bytesRead;
//				while ((bytesRead = fis.read(buffer)) != -1) {
//					bos.write(buffer, 0, bytesRead);
//				}
//
//				bos.flush();
//				System.out.println("File đã được gửi tới server!");
//				
//
//			}
//		} catch (IOException e) {
//			System.out.println("Lỗi khi gửi file: " + e.getMessage());
//		}
//	}
//	private void sendFile(String zipFilePath, String fileName) {
//		try {
//			File file = new File(zipFilePath);
//			try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
//					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//					FileInputStream fis = new FileInputStream(file);
//					BufferedOutputStream bos = new BufferedOutputStream(dos);
//					DataInputStream dis = new DataInputStream(socket.getInputStream())) { // DataInputStream để nhận
//																							// phản hồi từ server
//
//				dos.writeUTF("UploadFile"); // Gửi loại yêu cầu
//				dos.writeUTF(mssv); // Gửi MSSV
//				dos.writeUTF(fileName); // Gửi tên file
//				dos.writeUTF("2"); // Gửi tên file
//
//
//				// Gửi kích thước file tính bằng KB
//				double fileSizeInKB = (double) file.length() / 1024;
//				double roundedFileSizeInKB = Math.round(fileSizeInKB); // Làm tròn đến hàng đơn vị
//				dos.writeUTF(String.valueOf(roundedFileSizeInKB));
//
//				// Gửi dữ liệu file ZIP
//				byte[] buffer = new byte[1024];
//				int bytesRead;
//				while ((bytesRead = fis.read(buffer)) != -1) {
//					bos.write(buffer, 0, bytesRead);
//				}
//
//				bos.flush();
//				System.out.println("File đã được gửi tới server!");
//
//	            // Nhận phản hồi từ server
//	            String serverResponse = dis.readUTF();
//	            System.out.println("Server Response: " + serverResponse);
//			}
//		} catch (IOException e) {
//			System.out.println("Lỗi khi gửi file: " + e.getMessage());
//		}
//	}

}
