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
import javafx.util.Pair;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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
import javafx.scene.input.KeyCode;
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
	private FileManager fileManager = new FileManager();

	String mssv = UserSession.getInstance().getMSSV();
	String root = UserSession.getInstance().getRoot();
	private Popup currentPopup;

	@FXML
	public GridPane Centerpane;
	@FXML
	private Label lbMssv;
	@FXML
	public Label lbTen;

	@FXML
	private Button btnUp;

	@FXML
	private HBox labelPath;

	@FXML
	private TextField txtSearch;

	private Stack<FileNode> historyStack = new Stack<>();

	private String currentFunction;

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
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Information.fxml"));
			Node newContent = loader.load();

			InformationController controller = loader.getController();
			controller.setMainController(this);

			controller.loadInformation();

			Centerpane.getChildren().clear();
			Centerpane.add(newContent, 0, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void handleDangXuat(ActionEvent event) {
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

			dos.writeUTF(requestType);
			dos.writeUTF(mssv);

			String folderResponse = dis.readUTF();
			System.out.println("Folders: " + folderResponse);

			String fileResponse = dis.readUTF();
			System.out.println("Files: " + fileResponse);

			storeData(fileManager, folderResponse, fileResponse);

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
				if (parts.length == 2) {
					String id = parts[0].trim();
					String name = parts[1].trim();
					FileNode folder = new FileNode(id, name, true);
					String parentId = root;

					// Kiểm tra xem thư mục đã tồn tại chưa trước khi thêm
					if (!fileManager.isFolderExists(id)) {
						fileManager.addFolder(parentId, folder);
					}
				}
			}
		}

		// Phân tách và lưu trữ file
		String[] fileEntries = fileResponse.split(";");
		for (String entry : fileEntries) {
			if (!entry.trim().isEmpty()) {
				String[] parts = entry.split(",");
				if (parts.length == 2) {
					String id = parts[0].trim();
					String name = parts[1].trim();
					FileNode file = new FileNode(id, name, false);
					String parentId = root;

					// Kiểm tra xem file đã tồn tại chưa trước khi thêm
					if (!fileManager.isFileExists(id)) {
						fileManager.addFile(parentId, file);
					}
				}
			}
		}

		fileManager.markAsLoaded(root);

	}

	private void displayFiles(FileManager fileManager) {
		// Tạo GridPane để hiển thị thư mục và file
		GridPane contentGrid = createGridPane();
		List<FileNode> folders = fileManager.getFolders(root);
		List<FileNode> files = fileManager.getFiles(root);

		int row = 0, col = 0;

		// Hiển thị các thư mục
		for (FileNode folder : folders) {
			displayFileNode(folder, contentGrid, col, row, fileManager);
			col++;
			if (col >= 8) {
				col = 0;
				row++;
			}
		}

		// Hiển thị các file
		for (FileNode file : files) {
			displayFileNode(file, contentGrid, col, row, fileManager);
			col++;
			if (col >= 8) {
				col = 0;
				row++;
			}
		}

		VBox contentBox = new VBox(20);
		contentBox.getChildren().add(contentGrid);

		ScrollPane scrollPane = new ScrollPane(contentBox);
		scrollPane.setFitToWidth(true);

		Centerpane.getChildren().clear();
		Centerpane.getChildren().add(scrollPane);
	}

	private void displayFileNode(FileNode node, GridPane contentGrid, int col, int row, FileManager fileManager) {
		String iconPath = node.isFolder() ? "/image/File.png" : getIconPath(node.getName());
		ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
		icon.setFitWidth(90);
		icon.setFitHeight(90);

		Label label = new Label(node.getName());
		label.setId(node.getId());

		Label menuIcon = new Label("⋮");
		menuIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: gray; -fx-padding: 0 5px;");

		VBox box = new VBox(6, icon, label);
		box.setAlignment(Pos.CENTER);

		HBox labelBox = new HBox(5, label, menuIcon);
		labelBox.setAlignment(Pos.CENTER);
		labelBox.setSpacing(5);

		menuIcon.setMaxWidth(20);

		box.getChildren().add(labelBox);

		// Gán sự kiện chuột trái cho icon
		icon.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				if (node.isFolder()) {
					handleFolderClick(node.getId());
				}
			}
		});

		// Gán sự kiện cho dấu ⋮
		menuIcon.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {

				ContextMenu contextMenu;
				if (node.isFolder()) {
					contextMenu = createFolderContextMenu(node, label);
				} else {
					contextMenu = createFileContextMenu(node, label);
				}

				contextMenu.show((Node) event.getSource(), event.getScreenX(), event.getScreenY());
			}
		});

		contentGrid.add(box, col, row);
	}

	private GridPane createGridPane() {
		GridPane gridPane = new GridPane();
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		gridPane.setPadding(new Insets(10));
		gridPane.getColumnConstraints().addAll(createColumnConstraints());
		return gridPane;
	}

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

			dos.writeUTF("GetAllByFolderID");
			dos.writeUTF(folderId);

			String folderResponse = dis.readUTF();
			String fileResponse = dis.readUTF();

			storeData(fileManager, folderId, folderResponse, fileResponse);

			fileManager.markAsLoaded(folderId);

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

				if (!fileManager.isFolderExists(id)) {
					fileManager.addFolder(folderId, folder);
					fileManager.markAsLoaded(id);
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

				if (!fileManager.isFileExists(id)) {
					fileManager.addFile(folderId, file);
					fileManager.markAsLoaded(id);
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
			if (col >= 8) {
				col = 0;
				row++;
			}
		}

		for (FileNode file : files) {
			displayFileNode(file, contentGrid, col, row, fileManager);
			col++;
			if (col >= 8) {
				col = 0;
				row++;
			}
		}

		VBox contentBox = new VBox(20);
		contentBox.getChildren().add(contentGrid);

		ScrollPane scrollPane = new ScrollPane(contentBox);
		scrollPane.setFitToWidth(true);

		Centerpane.getChildren().clear();
		Centerpane.getChildren().add(scrollPane);
	}

	public void handleFolderClick(String folderId) {
		FileNode currentFolder = findFolderById(folderId);
		if (currentFolder != null) {
			currentFolderId = folderId;
			historyStack.push(currentFolder);

			if (fileManager.isClicked(folderId)) {
				displayFiles(fileManager, folderId);
			} else {
				fileManager.markAsClicked(folderId);
				loadFolderFromServer(folderId, fileManager);
			}

			updateBreadcrumb();
		}
	}

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
		labelPath.getChildren().clear();

		List<FileNode> breadcrumb = new ArrayList<>(historyStack);

		for (int i = 0; i < breadcrumb.size(); i++) {
			FileNode folder = breadcrumb.get(i);

			Hyperlink link = new Hyperlink(folder.getName());
			link.setOnAction(event -> handleBreadcrumbClick(folder.getId()));

			link.setStyle("-fx-font-size: 14px; -fx-text-fill: black; -fx-underline: false;");

			labelPath.getChildren().add(link);

			if (i < breadcrumb.size() - 1) {
				Label separator = new Label(">");
				separator.setStyle("-fx-font-size: 16px;"); // Đồng bộ kích thước với Hyperlink
				labelPath.getChildren().add(separator);
			}
		}
	}

	private void handleBreadcrumbClick(String folderId) {
		while (!historyStack.isEmpty() && !historyStack.peek().getId().equals(folderId)) {
			historyStack.pop();
		}

		currentFolderId = folderId;
		displayFiles(fileManager, currentFolderId);
		updateBreadcrumb();
	}

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
			controller.setFileManager(this.fileManager);

			Popup popup = new Popup();
			popup.getContent().add(uploadOptionsLayout);

			currentPopup = popup;

			Point2D buttonLocation = btnUp.localToScreen(btnUp.getLayoutBounds().getMinX(),
					btnUp.getLayoutBounds().getMinY());

			popup.setX(buttonLocation.getX() + 20);
			popup.setY(buttonLocation.getY() - uploadOptionsLayout.getPrefHeight() + 60);

			popup.show(btnUp.getScene().getWindow());

			btnUp.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				if (!popup.getContent().contains(event.getTarget())) {
					hidePopup();
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void hidePopup() {
		if (currentPopup != null) {
			currentPopup.hide();
		}
	}

//Menu
	private ContextMenu createFileContextMenu(FileNode fileNode, Label fileLabel) {
		ContextMenu contextMenu = new ContextMenu();
		contextMenu.setStyle("-fx-font-size: 14px;");

		if ("GetGuestFileName".equals(currentFunction)) {
			// tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(event -> downloadFile(fileNode.getId(), (Stage) fileLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);
			// Tùy chọn List Shared
			MenuItem renameItem = new MenuItem("Rename");
			renameItem.setOnAction(event -> renamefile(fileNode, fileLabel));
			contextMenu.getItems().add(renameItem);

			// Tùy chọn Remove
			MenuItem removeItem = new MenuItem("Remove");
			removeItem.setOnAction(event -> removeFile(fileNode.getId()));
			contextMenu.getItems().add(removeItem);
		} else {
			// Tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(event -> downloadFile(fileNode.getId(), (Stage) fileLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);

			// Tùy chọn Share
			MenuItem shareItem = new MenuItem("Share");
			shareItem.setOnAction(event -> {
				showCustomShareDialog(fileNode, false);
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
			removeItem.setOnAction(event -> removeFile(fileNode.getId()));
			contextMenu.getItems().add(removeItem);
		}

		return contextMenu;
	}

	private ContextMenu createFolderContextMenu(FileNode folderNode, Label folderLabel) {
		ContextMenu contextMenu = new ContextMenu();
		contextMenu.setStyle("-fx-font-size: 14px;");

		if ("GetGuestFileName".equals(currentFunction)) {
			// tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(
					event -> downloadFolder(folderNode.getId(), (Stage) folderLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);
			// Tùy chọn List Shared
			MenuItem renameItem = new MenuItem("Rename");
			renameItem.setOnAction(event -> renameFolder(folderNode, folderLabel));
			contextMenu.getItems().add(renameItem);

			// Tùy chọn Remove
			MenuItem removeItem = new MenuItem("Remove");
			removeItem.setOnAction(event -> removeFolder(folderNode.getId()));
			contextMenu.getItems().add(removeItem);
		} else {
			// Tùy chọn Download
			MenuItem downloadItem = new MenuItem("Download");
			downloadItem.setOnAction(
					event -> downloadFolder(folderNode.getId(), (Stage) folderLabel.getScene().getWindow()));
			contextMenu.getItems().add(downloadItem);

			// Tùy chọn Share
			MenuItem shareItem = new MenuItem("Share");
			shareItem.setOnAction(event -> {
				showCustomShareDialog(folderNode, true);
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
			removeItem.setOnAction(event -> removeFolder(folderNode.getId()));
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

//Doi ten


	private void renamefile(FileNode fileNode, Label fileLabel) {
	    // Tạo TextField để nhập tên mới
	    TextField renameField = new TextField(fileNode.getName());
	    renameField.setStyle("-fx-font-size: 14px;");

	    // Tạo Popup để chứa TextField
	    Popup popup = new Popup();
	    popup.getContent().add(renameField);
	    popup.setAutoHide(true); // Ẩn popup khi click ra ngoài

	    // Lấy vị trí của Label để hiển thị Popup
	    Point2D labelPos = fileLabel.localToScreen(0, 0);

	    // Hiển thị Popup
	    popup.show(fileLabel.getScene().getWindow(), labelPos.getX(), labelPos.getY() + fileLabel.getHeight());

	    // Focus vào TextField
	    renameField.requestFocus();

	    // Xử lý sự kiện khi người dùng nhấn Enter
	    renameField.setOnAction(event -> {
	        String newName = renameField.getText();

	        // Kiểm tra tên mới hợp lệ
	        if (newName == null || newName.trim().isEmpty()) {
	            showAlert(Alert.AlertType.WARNING, "Lỗi", "Tên file không được để trống.");
	            return;
	        }

	        // Ẩn Popup
	        popup.hide();

	        // Gửi yêu cầu đổi tên lên server
	        renameFileRequest(fileNode.getId(), newName, fileNode);
	    });

	    // Xử lý sự kiện khi người dùng nhấn ESC
	    renameField.setOnKeyPressed(event -> {
	        if (event.getCode() == KeyCode.ESCAPE) {
	            popup.hide();
	        }
	    });
	}

	private void renameFolder(FileNode folderNode, Label folderLabel) {
	    // Tạo TextField để nhập tên mới
	    TextField renameField = new TextField(folderNode.getName());
	    renameField.setStyle("-fx-font-size: 14px;");

	    // Tạo Popup để chứa TextField
	    Popup popup = new Popup();
	    popup.getContent().add(renameField);
	    popup.setAutoHide(true); // Ẩn popup khi click ra ngoài

	    // Lấy vị trí của Label để hiển thị Popup
	    Point2D labelPos = folderLabel.localToScreen(0, 0);

	    // Hiển thị Popup
	    popup.show(folderLabel.getScene().getWindow(), labelPos.getX(), labelPos.getY() + folderLabel.getHeight());

	    // Focus vào TextField
	    renameField.requestFocus();

	    // Xử lý sự kiện khi người dùng nhấn Enter
	    renameField.setOnAction(event -> {
	        String newName = renameField.getText();

	        // Kiểm tra tên mới hợp lệ
	        if (newName == null || newName.trim().isEmpty()) {
	            showAlert(Alert.AlertType.WARNING, "Lỗi", "Tên thư mục không được để trống.");
	            return;
	        }

	        // Ẩn Popup
	        popup.hide();

	        // Gửi yêu cầu đổi tên lên server
	        renameFolderRequest(folderNode.getId(), newName, folderNode);
	    });

	    // Xử lý sự kiện khi người dùng nhấn ESC
	    renameField.setOnKeyPressed(event -> {
	        if (event.getCode() == KeyCode.ESCAPE) {
	            popup.hide();
	        }
	    });
	}

//	private void renameFolderRequest(String id, String newName) {
//		try {
//			// Tạo kết nối tới server
//			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
//			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//			dos.writeUTF("ChangeFolderName");
//			dos.writeUTF(mssv); // Gửi MSSV
//			dos.writeUTF(id); // Gửi ID (FileID hoặc FolderID)
//			dos.writeUTF(newName); // Gửi tên mới
//
//			DataInputStream dis = new DataInputStream(socket.getInputStream());
//			String response = dis.readUTF();
//
//			if (response.equals("Folder renamed successfully.")) {
//				showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi tên thư mục thành công!");
//			} else {
//				showAlert(Alert.AlertType.ERROR, "Lỗi", "Đổi tên thư mục thất bại!");
//			}
//
//			dis.close();
//			dos.close();
//			socket.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void renameFileRequest(String id, String newName) {
//		try {
//			// Tạo kết nối tới server
//			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
//			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//			dos.writeUTF("RenameFile");
//			dos.writeUTF(mssv); // Gửi MSSV
//			dos.writeUTF(id); // Gửi ID (FileID hoặc FolderID)
//			dos.writeUTF(newName); // Gửi tên mới
//
//			DataInputStream dis = new DataInputStream(socket.getInputStream());
//			String response = dis.readUTF();
//
//			if (response.equals("Rename successfully")) {
//				showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi tên file thành công!");
//			} else {
//				showAlert(Alert.AlertType.ERROR, "Lỗi", "Đổi tên file thất bại!");
//			}
//
//			dis.close();
//			dos.close();
//			socket.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	private void renameFolderRequest(String id, String newName, FileNode folderNode) {
	    try {
	        // Tạo kết nối tới server
	        Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
	        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
	        dos.writeUTF("ChangeFolderName");
	        dos.writeUTF(mssv); // Gửi MSSV
	        dos.writeUTF(id); // Gửi ID
	        dos.writeUTF(newName); // Gửi tên mới

	        DataInputStream dis = new DataInputStream(socket.getInputStream());
	        String response = dis.readUTF();

	        // Xử lý response từ server
	        Platform.runLater(() -> {
	            if (response.equals("Folder renamed successfully.")) {
	                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi tên thư mục thành công!");
	                folderNode.setName(newName); // Cập nhật tên trong FileNode
	            } else {
	                showAlert(Alert.AlertType.ERROR, "Lỗi", "Đổi tên thư mục thất bại!");
	            }

	            // Cập nhật lại giao diện sau khi nhận phản hồi từ server
	            if (!currentFolderId.equals(root)) {
	                loadFolderFromServer(currentFolderId, fileManager);
	            } else {
	                loadFiles(currentFunction);
	            }
	        });

	        dis.close();
	        dos.close();
	        socket.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi kết nối đến server!"));
	    }
	}

	private void renameFileRequest(String id, String newName, FileNode fileNode) {
	    try {
	        // Tạo kết nối tới server
	        Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
	        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
	        dos.writeUTF("RenameFile");
	        dos.writeUTF(mssv); // Gửi MSSV
	        dos.writeUTF(id); // Gửi ID
	        dos.writeUTF(newName); // Gửi tên mới

	        DataInputStream dis = new DataInputStream(socket.getInputStream());
	        String response = dis.readUTF();

	        // Xử lý response từ server
	        Platform.runLater(() -> {
	            if (response.equals("Rename successfully")) {
	                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi tên file thành công!");
	                fileNode.setName(newName); // Cập nhật tên trong FileNode
	            } else {
	                showAlert(Alert.AlertType.ERROR, "Lỗi", "Đổi tên file thất bại!");
	            }

	            // Cập nhật lại giao diện sau khi nhận phản hồi từ server
	            if (!currentFolderId.equals(root)) {
	                loadFolderFromServer(currentFolderId, fileManager);
	            } else {
	                loadFiles(currentFunction);
	            }
	        });

	        dis.close();
	        dos.close();
	        socket.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi kết nối đến server!"));
	    }
	}

//Download
	private void downloadFile(String fileId, Stage stage) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Download File");
		alert.setHeaderText("Download File");
		alert.setContentText("Do you want to download this file?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Chọn thư mục để lưu file");
			File selectedDirectory = directoryChooser.showDialog(stage);

			if (selectedDirectory == null) {
				System.out.println("Không có thư mục nào được chọn. Hủy tải file.");
				return;
			}

			try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
					DataInputStream dis = new DataInputStream(socket.getInputStream())) {

				dos.writeUTF("DownloadFile");
				dos.writeUTF(fileId);

				receiveFile(dis, selectedDirectory);

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Lỗi khi tải file: " + e.getMessage());
			}
		}
	}

	private void receiveFile(DataInputStream dis, File selectedDirectory) throws IOException {
		String fileName = dis.readUTF();
		File zipFile = new File(selectedDirectory, "temp_" + fileName + ".zip");

		try (FileOutputStream fos = new FileOutputStream(zipFile);
				BufferedOutputStream bos = new BufferedOutputStream(fos)) {

			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = dis.read(buffer)) > 0) {
				bos.write(buffer, 0, bytesRead);
			}
			bos.flush();
			System.out.println("File ZIP downloaded successfully to: " + zipFile.getAbsolutePath());

			if (zipFile.length() == 0) {
				showAlert(Alert.AlertType.ERROR, "Lỗi tải file", "Tải File thất bại!");
				return;
			}

			extractZipFile(zipFile, selectedDirectory.getAbsolutePath() + File.separator);

			showAlert(Alert.AlertType.INFORMATION, "Tải file thành công", "File đã được tải xuống thành công!");

		} finally {
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
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Download Folder");
		alert.setHeaderText("Download Folder");
		alert.setContentText("Do you want to download this folder?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
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

				dos.writeUTF("DownloadFolder");
				dos.writeUTF(folderId);

				receiveFolder(dis, selectedDirectory);

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Lỗi khi tải thư mục: " + e.getMessage());
			}
		}
	}

	public void receiveFolder(DataInputStream dis, File selectedDirectory) throws IOException {
		try {
			String fileName = dis.readUTF();
			String fileSizeStr = dis.readUTF();

			long fileSize = Long.parseLong(fileSizeStr);
			File file = new File(selectedDirectory, fileName);

			try (FileOutputStream fos = new FileOutputStream(file);
					BufferedOutputStream bos = new BufferedOutputStream(fos)) {

				byte[] buffer = new byte[8192];
				long totalBytesRead = 0;
				int bytesRead;

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

//Share	
	private List<String> getRolesFromServer() {
		List<String> roles = new ArrayList<>();
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("GetAllFileRole");

			String response = dis.readUTF();
			// Xử lý response: 0,Admin;1,Read_Write;2,Read_only
			String[] roleParts = response.split(";");
			for (String part : roleParts) {
				String[] roleInfo = part.split(",");
				if (roleInfo.length == 2) {
					// Lọc bỏ role "Admin" (roleId = 0)
					if (!roleInfo[0].equals("0")) {
						roles.add(roleInfo[1]); // Thêm roleName vào danh sách
					}
				}
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return roles;
	}

	private void showCustomShareDialog(FileNode node, boolean isFolder) {
		Dialog<Pair<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Chia sẻ");
		dialog.setHeaderText((isFolder ? "Chia sẻ thư mục" : "Chia sẻ file") + " với người khác");

		// Thêm các nút
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		// Tạo GridPane để chứa các thành phần
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		// Tạo TextField để nhập MSSV
		TextField mssvField = new TextField();
		mssvField.setPromptText("Nhập MSSV");

		// Lấy danh sách quyền từ server
		List<String> roles = getRolesFromServer();

		// Tạo ComboBox để chọn quyền
		ComboBox<String> roleComboBox = new ComboBox<>();
		roleComboBox.getItems().addAll(roles);
		roleComboBox.setValue(roles.isEmpty() ? "" : roles.get(0)); // Chọn quyền mặc định nếu có

		// Thêm các thành phần vào GridPane
		grid.add(new Label("MSSV:"), 0, 0);
		grid.add(mssvField, 1, 0);
		grid.add(new Label("Chọn quyền:"), 0, 1);
		grid.add(roleComboBox, 1, 1);

		// Thêm GridPane vào Dialog
		dialog.getDialogPane().setContent(grid);

		// Chuyển đổi kết quả khi nhấn OK
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.OK) {
				return new Pair<>(mssvField.getText(), roleComboBox.getValue());
			}
			return null;
		});

		Optional<Pair<String, String>> result = dialog.showAndWait();

		result.ifPresent(mssvRole -> {
			String mssv = mssvRole.getKey();
			String roleName = mssvRole.getValue();
			String roleId = getRoleIdFromName(roleName);

			if (isFolder) {
				shareFolder(node.getId(), mssv, roleId);
				listSharedUsersfolder(node.getId());
			} else {
				shareFile(node.getId(), mssv, roleId);
				listSharedUsers(node.getId());
			}
		});
	}

	private void shareFile(String fileId, String mssvgest, String roleId) {
		try {

			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu chia sẻ đến server
			dos.writeUTF("AddGuest");
			dos.writeUTF(mssv);
			dos.writeUTF(mssvgest);
			dos.writeUTF(fileId);
			dos.writeUTF(roleId); // Gửi thêm roleId

			// Nhận phản hồi từ server
			String response = dis.readUTF();
			System.out.println("Server response: " + response);

			dis.close();
			dos.close();
			socket.close();

			// Hiển thị thông báo cho người dùng
			if (response.equals("Share file successfully")) {
				showAlert(Alert.AlertType.INFORMATION, "Chia sẻ thành công", "File đã được chia sẻ với " + mssvgest);
			} else {
				showAlert(Alert.AlertType.ERROR, "Chia sẻ thất bại", "Không thể chia sẻ file.");
			}
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Lỗi chia sẻ", "Lỗi khi kết nối đến server: " + e.getMessage());
		}
	}

	private void shareFolder(String folderId, String mssvgest, String roleId) {
		try {

			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu chia sẻ đến server
			dos.writeUTF("ShareFolder");
			dos.writeUTF(mssv);
			dos.writeUTF(folderId);
			dos.writeUTF(mssvgest);
			dos.writeUTF(roleId); // Gửi thêm roleId

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

	private String getRoleIdFromName(String roleName) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("GetAllFileRole");

			String response = dis.readUTF();
			String[] roleParts = response.split(";");
			for (String part : roleParts) {
				String[] roleInfo = part.split(",");
				if (roleInfo.length == 2 && roleInfo[1].equals(roleName)) {
					return roleInfo[0];
				}
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null; // Default roleId
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

//Danh sach chia se
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
			System.out.println("Received from server: " + response);

			if (response.equals("ERR")) {
				System.out.println("Lỗi khi tìm người xem file.");
			} else {
				// Tạo ListView hiển thị danh sách người dùng đã chia sẻ
				ListView<HBox> sharedUsersList = new ListView<>();
				sharedUsersList.setStyle("-fx-font-size: 14px;");

				// Tách danh sách người dùng và thêm vào ListView
				String[] users = response.split(";");
				for (String user : users) {
					String[] userInfo = user.split(",");
					if (userInfo.length == 3) {
						String userId = userInfo[0];
						String roleId = userInfo[1];
						String roleName = userInfo[2];

						Label userLabel = new Label(userId);
						Label roleLabel = new Label(" - " + roleName);

						Button editButton = new Button("Chỉnh sửa");
						editButton.setOnAction(e -> {
							handleEditRole(fileId, userId, roleId, roleLabel);
						});

						HBox userBox = new HBox(10);
						userBox.getChildren().addAll(userLabel, roleLabel, editButton);
						userBox.setAlignment(Pos.CENTER_LEFT); // Căn giữa theo chiều dọc

						sharedUsersList.getItems().add(userBox);
					}
				}

				// Tạo Dialog để hiển thị ListView
				Dialog<String> dialog = new Dialog<>();
				dialog.setTitle("Danh sách được chia sẻ");
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

	private void listSharedUsersfolder(String folderId) {
		try {
			// Tạo kết nối đến server
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu lấy danh sách người dùng đã chia sẻ thư mục
			dos.writeUTF("GetAllFolderGuest");
			dos.writeUTF(mssv);
			dos.writeUTF(folderId);

			// Nhận danh sách người dùng đã chia sẻ thư mục
			String response = dis.readUTF();
			System.out.println("Received from server: " + response);

			if (response.equals("ERR")) {
				System.out.println("Lỗi khi tìm người xem thư mục.");
			} else {
				// Tạo ListView hiển thị danh sách người dùng đã chia sẻ
				ListView<HBox> sharedUsersList = new ListView<>();
				sharedUsersList.setStyle("-fx-font-size: 14px;");

				// Tách danh sách người dùng và thêm vào ListView
				String[] users = response.split(";");
				for (String user : users) {
					String[] userInfo = user.split(",");
					if (userInfo.length == 3) {
						String userId = userInfo[0];
						String roleId = userInfo[1];
						String roleName = userInfo[2];

						Label userLabel = new Label(userId);
						Label roleLabel = new Label(" - " + roleName);

						Button editButton = new Button("Chỉnh sửa");
						editButton.setOnAction(e -> {
							handleEditRoleFolder(folderId, userId, roleId, roleLabel);
						});

						HBox userBox = new HBox(10);
						userBox.getChildren().addAll(userLabel, roleLabel, editButton);
						userBox.setAlignment(Pos.CENTER_LEFT); // Căn giữa theo chiều dọc

						sharedUsersList.getItems().add(userBox);
					}
				}

				// Tạo Dialog để hiển thị ListView
				Dialog<String> dialog = new Dialog<>();
				dialog.setTitle("Danh sách được chia sẻ");
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

	private void handleEditRole(String fileId, String userId, String currentRoleId, Label roleLabel) {
		// ... (phần code tạo dialog và chọn quyền giữ nguyên) ...
		List<String> roles = getRolesFromServer();
		roles.remove("Admin"); // Xóa "Admin" khỏi danh sách

		// Tạo ComboBox để chọn quyền
		ComboBox<String> roleComboBox = new ComboBox<>();
		roleComboBox.getItems().addAll(roles);
		roleComboBox.setValue(getRoleNameFromId(currentRoleId)); // Chọn quyền hiện tại
		roleComboBox.setStyle("-fx-font-size: 14px;");

		// Tạo Dialog để hiển thị ComboBox
		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle("Chỉnh sửa quyền");
		dialog.setHeaderText("Chỉnh sửa quyền cho " + userId);
		dialog.getDialogPane().setContent(roleComboBox);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		// Xử lý khi nhấn OK
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.OK) {
				String newRoleName = roleComboBox.getValue();
				String newRoleId = getRoleIdFromName(newRoleName);

				// Cập nhật roleLabel ngay lập tức
				roleLabel.setText(" - " + newRoleName);

				// Gửi yêu cầu cập nhật quyền đến server (sử dụng updateFileRole)
				updateFileRole(fileId, userId, newRoleId);
				return newRoleId;
			}
			return null;
		});

		dialog.showAndWait();
	}

	private void handleEditRoleFolder(String folderId, String userId, String currentRoleId, Label roleLabel) {
		// ... (phần code tạo dialog và chọn quyền giữ nguyên) ...
		List<String> roles = getRolesFromServer();
		roles.remove("Admin"); // Xóa "Admin" khỏi danh sách

		// Tạo ComboBox để chọn quyền
		ComboBox<String> roleComboBox = new ComboBox<>();
		roleComboBox.getItems().addAll(roles);
		roleComboBox.setValue(getRoleNameFromId(currentRoleId));
		roleComboBox.setStyle("-fx-font-size: 14px;");

		// Tạo Dialog để hiển thị ComboBox
		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle("Chỉnh sửa quyền");
		dialog.setHeaderText("Chỉnh sửa quyền cho " + userId);
		dialog.getDialogPane().setContent(roleComboBox);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		// Xử lý khi nhấn OK
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.OK) {
				String newRoleName = roleComboBox.getValue();
				String newRoleId = getRoleIdFromName(newRoleName);

				// Cập nhật roleLabel ngay lập tức
				roleLabel.setText(" - " + newRoleName);

				// Gửi yêu cầu cập nhật quyền đến server (sử dụng updateFolderRole)
				updateFolderRole(folderId, userId, newRoleId);
				return newRoleId;
			}
			return null;
		});

		dialog.showAndWait();
	}

	private void updateFileRole(String fileId, String userId, String roleId) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu cập nhật quyền cho file
			dos.writeUTF("UpdateFileRole");
			dos.writeUTF(mssv);
			dos.writeUTF(fileId);
			dos.writeUTF(userId);
			dos.writeUTF(roleId);

			// Nhận phản hồi từ server
			String response = dis.readUTF();
			System.out.println("Server response: " + response);

			// Hiển thị thông báo
			if (response.equals("Update successfully")) {
				showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật quyền thành công");
			} else {
				showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật quyền thất bại");
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi kết nối đến server: " + e.getMessage());
		}
	}

	private String getRoleNameFromId(String roleId) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("GetAllFileRole");

			String response = dis.readUTF();
			String[] roleParts = response.split(";");
			for (String part : roleParts) {
				String[] roleInfo = part.split(",");
				if (roleInfo.length == 2 && roleInfo[0].equals(roleId)) {
					return roleInfo[1];
				}
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ""; // Default
	}

	private void updateFolderRole(String folderId, String userId, String roleId) {
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			// Gửi yêu cầu cập nhật quyền cho thư mục
			dos.writeUTF("UpdateFolderRole");
			dos.writeUTF(mssv);
			dos.writeUTF(folderId);
			dos.writeUTF(userId);
			dos.writeUTF(roleId);

			// Nhận phản hồi từ server
			String response = dis.readUTF();
			System.out.println("Server response: " + response);

			// Hiển thị thông báo
			if (response.equals("Update successfully")) {
				showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật quyền thành công");
			} else {
				showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật quyền thất bại");
			}

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi kết nối đến server: " + e.getMessage());
		}
	}

//Tim kiem
	@FXML
	public void handleSearch(ActionEvent event) {
		String keyword = txtSearch.getText();
		if (keyword == null || keyword.trim().isEmpty()) {
			// Nếu ô tìm kiếm trống, hiển thị lại tất cả file/folder
			if (currentFunction.equals("GetMyFileName"))
				handleCuaToi();
			else if (currentFunction.equals("GetGuestFileName")) {
				handleDuocChiaSe();
			} else {
				handleTatCa();
			}
			return;
		}

		// Xóa dữ liệu cũ
		fileManager.reset();

		// Gửi yêu cầu tìm kiếm đến server
		try {
			Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF("Search");
			dos.writeUTF(mssv);
			dos.writeUTF(keyword);

			// Nhận phản hồi từ server
			String folderResponse = dis.readUTF();
			String fileResponse = dis.readUTF();

			// Lưu trữ dữ liệu mới, giả sử bạn đã có hàm storeSearchData tương tự storeData
			storeSearchData(fileManager, folderResponse, fileResponse);

			// Hiển thị kết quả tìm kiếm
			Platform.runLater(() -> {
				if (fileManager.isEmpty()) {
					// Nếu không tìm thấy kết quả
					showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Không tìm thấy kết quả phù hợp.");
				} else {
					// Hiển thị kết quả tìm kiếm
					displayFiles(fileManager);
				}
			});

			dis.close();
			dos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi kết nối đến server!");
		}
	}

	private void storeSearchData(FileManager fileManager, String folderResponse, String fileResponse) {
		// Xử lý folderResponse
		if (!folderResponse.trim().isEmpty()) { // Kiểm tra chuỗi rỗng
			String[] folderEntries = folderResponse.split(";");
			for (String entry : folderEntries) {
				String[] parts = entry.split(",");
				if (parts.length == 2) {
					String id = parts[0].trim();
					String name = parts[1].trim();
					FileNode folder = new FileNode(id, name, true);
					// Thêm folder vào root (vì không có parentId)
					if (!fileManager.isFolderExists(id)) {
						fileManager.addFolder(root, folder); // Thay vì parentId, ta thêm vào root
					}
				}
			}
		}

		// Xử lý fileResponse
		if (!fileResponse.trim().isEmpty()) { // Kiểm tra chuỗi rỗng
			String[] fileEntries = fileResponse.split(";");
			for (String entry : fileEntries) {
				String[] parts = entry.split(",");
				if (parts.length == 2) {
					String id = parts[0].trim();
					String name = parts[1].trim();
					FileNode file = new FileNode(id, name, false);
					// Thêm file vào root (vì không có parentId)
					if (!fileManager.isFileExists(id)) {
						fileManager.addFile(root, file); // Thay vì parentId, ta thêm vào root
					}
				}
			}
		}
	}

}
