package application;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class UploadOptionsController {

	String mssv = UserSession.getInstance().getMSSV();
	String root = UserSession.getInstance().getRoot();
	private String node;
	private MainController mainController;
	private FileManager fileManager; // Biến lưu trữ đối tượng fileManager

	public void setMainController(MainController mainController) {
		this.mainController = mainController;

	}

	public void setFileManager(FileManager fileManager) {
		this.fileManager = fileManager;
	}

	@FXML
	public void handleNewFolder() {
		mainController.hidePopup();

		node = mainController.getCurrentFolderId();

		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("New Folder");
		dialog.setHeaderText("Tạo thư mục mới");
		dialog.setContentText("Nhập tên thư mục:");
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(folderName -> {
			createFolder(folderName); // Tạo thư mục
		});

	}

	@FXML
	public void handleFileUpload() {
		mainController.hidePopup();

		node = mainController.getCurrentFolderId();

		// Mở hộp thoại chọn file
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Chọn file để tải lên");
		File selectedFile = fileChooser.showOpenDialog(null);

		if (selectedFile != null) {
			// Lấy kích thước file gốc
			long originalFileSize = selectedFile.length();

			// Nén file trước khi gửi
			String zipFilePath = zipFile(selectedFile.getAbsolutePath());
			if (zipFilePath != null) {
				// Gửi file đã nén tới server cùng với kích thước gốc
				sendFile(zipFilePath, selectedFile.getName(), originalFileSize);
			}
		}
	}

//	@FXML
//	public void handleFolderUpload() {
//		mainController.hidePopup(); // Ẩn popup hiện tại
//
//		// Mở hộp thoại chọn thư mục
//		DirectoryChooser directoryChooser = new DirectoryChooser();
//		directoryChooser.setTitle("Chọn thư mục để tải lên");
//		File selectedDirectory = directoryChooser.showDialog(new Stage());
//
//		if (selectedDirectory != null) {
//			// Đặt tên thư mục và nén thư mục
//			String zipFolderPath = zipFolder(selectedDirectory.getAbsolutePath());
//			if (zipFolderPath != null) {
//				// Gửi thư mục lên server
//				sendFolder(zipFolderPath, selectedDirectory.getName());
//			}
//		}
//	}

	private void createFolder(String folderName) {
		try (Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				DataInputStream dis = new DataInputStream(socket.getInputStream())) {

			dos.writeUTF("CreateFolder");
			dos.writeUTF(mssv);
			dos.writeUTF(folderName);

			String parentFolderId = (!node.equals(root)) ? node : root;
			dos.writeUTF(parentFolderId);
			System.out.println("root=" + root);
			System.out.println("node=" + parentFolderId);

			System.out.println("Yêu cầu tạo thư mục '" + folderName + "' đã được gửi.");
			System.out.println("Comparing parentFolderId='" + parentFolderId + "' with root='" + root + "'");
			String response = dis.readUTF();

			if (parentFolderId.equals(root)) {
				// Load files from the root directory (GetMyFileName)
				mainController.loadFiles("GetMyFileName");
			} else {
				// Load files from the specific folder (the newly created folder)
				mainController.loadFolderFromServer(parentFolderId, fileManager);
			}

		} catch (IOException e) {
			System.err.println("Error creating folder: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private String zipFile(String filePath) {

		String fileName = new File(filePath).getName();

		String zipFilePath = filePath.endsWith(".zip") ? filePath : filePath + ".zip";

		try (FileOutputStream fos = new FileOutputStream(zipFilePath);
				ZipOutputStream zos = new ZipOutputStream(fos);
				FileInputStream fis = new FileInputStream(filePath)) {

			ZipEntry zipEntry = new ZipEntry(fileName); // Lấy tên file mà không có đường dẫn
			zos.putNextEntry(zipEntry);

			byte[] buffer = new byte[1024];
			int len;
			while ((len = fis.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}

			zos.closeEntry();
			System.out.println("File đã được nén thành công: " + zipFilePath);

		} catch (IOException e) {
			System.out.println("Lỗi khi nén file: " + e.getMessage());
			return null; // Return null if there's an error in zipping
		}
		return zipFilePath;
	}

	private void sendFile(String zipFilePath, String fileName, long originalFileSize) {
		String currentFolderId = mainController.getCurrentFolderId();

		Socket socket = null;
		DataOutputStream dos = null;
		DataInputStream dis = null;
		FileInputStream fis = null;
		BufferedOutputStream bos = null;

		try {
			File file = new File(zipFilePath);
			if (!file.exists()) {
				System.out.println("File không tồn tại: " + file.getAbsolutePath());
				return;
			}

			// Create connection to the server
			socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			dos = new DataOutputStream(socket.getOutputStream());
			fis = new FileInputStream(file);
			bos = new BufferedOutputStream(dos);
			dis = new DataInputStream(socket.getInputStream());

			dos.writeUTF("UploadFile"); // Request type
			dos.writeUTF(mssv); // MSSV
			dos.writeUTF(fileName); // File name
			dos.writeUTF(currentFolderId); // Folder ID
			dos.writeUTF(String.valueOf(file.length())); // File size (kích thước file nén)
			dos.writeUTF(String.valueOf(originalFileSize)); // Kích thước file gốc
			String response = dis.readUTF();
			if ("Upload".equals(response)) {

				// Send file data
				byte[] buffer = new byte[8192];
				int bytesRead;
				long totalBytesSent = 0;

				while ((bytesRead = fis.read(buffer)) != -1) {
					bos.write(buffer, 0, bytesRead);
					totalBytesSent += bytesRead;
					System.out.println("Đã gửi: " + totalBytesSent + " / " + file.length() + " bytes");
				}

				bos.flush();
				System.out.println("File đã được gửi tới server!");

				String serverResponse = dis.readUTF();
				System.out.println("Phản hồi từ server: " + serverResponse);

				if ("Upload successful".equals(serverResponse)) {
					showAlert(Alert.AlertType.INFORMATION, "Upload file thành công", "File đã được Upload thành công!");

					if (file.delete()) {
						System.out.println("Đã xóa file nén tạm: " + file.getAbsolutePath());
					} else {
						System.out.println("Không thể xóa file nén tạm: " + file.getAbsolutePath());
					}

					if (currentFolderId.equals(root)) {
						mainController.loadFiles("GetMyFileName");
					} else {
						mainController.loadFolderFromServer(currentFolderId, fileManager);
					}
				} else {
					showAlert(Alert.AlertType.INFORMATION, "Upload file thất bại", "File đã được Upload thất bại!");
				}
			} else {
				showAlert(Alert.AlertType.ERROR, "Upload folder thất bại", response);
			}
		} catch (IOException e) {
			System.out.println("Lỗi khi gửi file: " + e.getMessage());
		} finally {
			try {
				if (dis != null)
					dis.close();
				if (dos != null)
					dos.close();
				if (bos != null)
					bos.close();
				if (fis != null)
					fis.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				System.out.println("Lỗi khi đóng kết nối: " + e.getMessage());
			}
		}
	}

	@FXML
	private void handleFolderUpload() {
		mainController.hidePopup(); // Ẩn popup hiện tại

		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Chọn thư mục để upload");

		// Mở cửa sổ chọn thư mục
		Stage stage = new Stage(); // Thay thế bằng Stage hiện tại nếu cần
		File selectedFolder = directoryChooser.showDialog(stage);

		if (selectedFolder != null) {
			String folderPath = selectedFolder.getAbsolutePath();
			System.out.println("Thư mục được chọn: " + folderPath);

			// Tính toán kích thước thư mục
			long folderSize = calculateFolderSize(selectedFolder);

			// Tạo file .zip từ thư mục
			File zipFile = new File(folderPath + ".zip");
			try {
				zipDirectory(selectedFolder, zipFile);
				System.out.println("Đã nén thư mục thành: " + zipFile.getAbsolutePath());

				sendFile(zipFile, folderSize); // Truyền thêm kích thước folder

			} catch (IOException e) {
				System.out.println("Lỗi khi nén thư mục: " + e.getMessage());
				showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể nén thư mục.");
			}
		} else {
			System.out.println("Không có thư mục nào được chọn.");
		}
	}

	public long calculateFolderSize(File folder) {
		long size = 0;
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				size += calculateFolderSize(file);
			} else {
				size += file.length();
			}
		}
		return size;
	}

	private boolean sendFile(File file, long folderSize) { // Thêm tham số folderSize
		String currentFolderId = mainController.getCurrentFolderId();
		Socket socket = null;
		DataOutputStream dos = null;
		DataInputStream dis = null;
		System.out.println("kích thước folder: " + folderSize);

		try {
			if (!file.exists()) {
				System.out.println("File không tồn tại: " + file.getAbsolutePath());
				return false;
			}

			System.out.println("Bắt đầu gửi file: " + file.getName());
			System.out.println("Kích thước file: " + file.length() + " bytes");
			System.out.println("Kích thước thư mục ban đầu: " + folderSize + " bytes"); // In ra kích thước folder

			// Kết nối tới server
			socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

			// Loại bỏ đuôi .zip khỏi tên file
			String fileName = file.getName();
			if (fileName.endsWith(".zip")) {
				fileName = fileName.substring(0, fileName.lastIndexOf(".zip"));
			}

			// Gửi thông tin file
			dos.writeUTF("UploadFolder"); // Loại yêu cầu
			dos.writeUTF(mssv); // MSSV
			dos.writeUTF(fileName); // Tên file không có đuôi .zip
			dos.writeUTF(currentFolderId); // ID thư mục hiện tại
			dos.writeUTF(String.valueOf(file.length())); // Kích thước file ZIP
			dos.writeUTF(String.valueOf(folderSize)); // Kích thước folder ban đầu
			String response = dis.readUTF();
			if ("Upload".equals(response)) {
				// Gửi dữ liệu file
				try (FileInputStream fis = new FileInputStream(file)) {
					byte[] buffer = new byte[8192];
					int bytesRead;
					long totalBytesSent = 0;

					while ((bytesRead = fis.read(buffer)) != -1) {
						dos.write(buffer, 0, bytesRead);
						totalBytesSent += bytesRead;
						System.out.println("Đã gửi: " + totalBytesSent + " / " + file.length() + " bytes");
					}
				}

				// Nhận phản hồi từ server
				String responseresult = dis.readUTF();
				System.out.println("Phản hồi từ server: " + responseresult);
				if ("Upload successfully".equals(responseresult)) {
					showAlert(Alert.AlertType.INFORMATION, "Upload file thành công", "File đã được Upload thành công!");
					// Xóa file ZIP tạm nếu gửi thành công (tùy chọn)
					 file.delete();

					if (currentFolderId.equals(root)) {
						mainController.loadFiles("GetMyFileName");
					} else {
						mainController.loadFolderFromServer(currentFolderId, fileManager);
					}
				} else {
					showAlert(Alert.AlertType.INFORMATION, "Upload file thất bại", "File đã được Upload thất bại!");
				}

				return true;
			}
			else {
				showAlert(Alert.AlertType.ERROR, "Upload folder thất bại", response);
				return false;
			}
		} catch (IOException e) {
			System.out.println("Lỗi khi gửi file: " + e.getMessage());
			return false;
		} finally {
			// Đảm bảo đóng socket và luồng
			try {
				if (dis != null)
					dis.close();
				if (dos != null)
					dos.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				System.out.println("Lỗi khi đóng kết nối: " + e.getMessage());
			}
		}
	}

	// ... (các phương thức khác giữ nguyên) ...

	private void zipDirectory(File inputDir, File outputZipFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(outputZipFile);
				ZipOutputStream zipOs = new ZipOutputStream(fos)) {

			// Thêm thư mục gốc vào file ZIP
			String rootEntryName = inputDir.getName() + "/";
			zipOs.putNextEntry(new ZipEntry(rootEntryName));
			zipOs.closeEntry(); // Đóng entry thư mục gốc

			// Lấy danh sách các file con và thêm chúng vào trong thư mục gốc
			List<File> allFiles = listChildFiles(inputDir);
			for (File file : allFiles) {
				// Tạo tên entry với cấu trúc thư mục gốc
				String entryName = inputDir.getName() + "/"
						+ file.getAbsolutePath().substring(inputDir.getAbsolutePath().length() + 1);

				// Nếu là thư mục, thêm dấu '/' vào cuối tên
				if (file.isDirectory()) {
					entryName += "/";
				}

				zipOs.putNextEntry(new ZipEntry(entryName));

				// Nếu là file, ghi dữ liệu vào ZIP
				if (file.isFile()) {
					try (FileInputStream fileIs = new FileInputStream(file)) {
						byte[] buffer = new byte[8192];
						int len;
						while ((len = fileIs.read(buffer)) > 0) {
							zipOs.write(buffer, 0, len);
						}
					}
				}

				zipOs.closeEntry();
			}
		}
	}

	/**
	 * Lấy danh sách các file trong thư mục: bao gồm tất cả các file con, cháu,..
	 * của thư mục đầu vào.
	 */
	private static List<File> listChildFiles(File dir) throws IOException {
		List<File> allFiles = new ArrayList<>();

		File[] childFiles = dir.listFiles();
		if (childFiles != null) {
			for (File file : childFiles) {
				if (file.isFile()) {
					allFiles.add(file);
				} else {
					allFiles.add(file); // Thêm thư mục vào danh sách
					List<File> files = listChildFiles(file);
					allFiles.addAll(files);
				}
			}
		}
		return allFiles;
	}
//
//	/**
//	 * Thêm các thư mục rỗng vào file zip.
//	 */
//	private void addEmptyDirectories(File inputDir, ZipOutputStream zipOs) throws IOException {
//		File[] childFiles = inputDir.listFiles();
//		if (childFiles != null) {
//			for (File file : childFiles) {
//				if (file.isDirectory()) {
//					String entryName = file.getAbsolutePath().substring(inputDir.getAbsolutePath().length() + 1) + "/";
//					zipOs.putNextEntry(new ZipEntry(entryName));
//					zipOs.closeEntry(); // Đảm bảo thư mục rỗng được thêm
//					addEmptyDirectories(file, zipOs); // Đệ quy để thêm thư mục con
//				}
//			}
//		}
//	}

	private void showAlert(Alert.AlertType alertType, String title, String contentText) {
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(title);
		alert.setContentText(contentText);
		alert.showAndWait();
	}

}
