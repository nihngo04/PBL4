package application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileManager {
	private Map<String, List<FileNode>> folders; // Lưu thư mục con theo id của thư mục cha
	private Map<String, List<FileNode>> files; // Lưu file con theo id của thư mục cha
	private Set<String> loadedIds; // Lưu danh sách id đã tải
	private Set<String> clickedIds; // Lưu danh sách id đã được click

	public FileManager() {
		folders = new HashMap<>();
		files = new HashMap<>();
		loadedIds = new HashSet<>();
		clickedIds = new HashSet<>();
	}

	// Đánh dấu thư mục đã tải
	public void markAsLoaded(String id) {
		loadedIds.add(id);
	}

	// Kiểm tra thư mục đã tải chưa
	public boolean isLoaded(String id) {
		return loadedIds.contains(id);
	}

	// Đánh dấu thư mục đã click
	public void markAsClicked(String id) {
		clickedIds.add(id);
	}

	// Kiểm tra thư mục đã click chưa
	public boolean isClicked(String id) {
		return clickedIds.contains(id);
	}

	// Các phương thức thêm thư mục và file như trước
	public void addFolder(String parentId, FileNode folder) {
		folders.computeIfAbsent(parentId, k -> new ArrayList<>()).add(folder);
	}

	public void addFile(String parentId, FileNode file) {
		files.computeIfAbsent(parentId, k -> new ArrayList<>()).add(file);
	}

	public List<FileNode> getFolders(String parentId) {
		return folders.getOrDefault(parentId, new ArrayList<>());
	}

	public List<FileNode> getFiles(String parentId) {
		return files.getOrDefault(parentId, new ArrayList<>());
	}

	public boolean isFolderExists(String folderId) {
		for (List<FileNode> folderList : folders.values()) {
			for (FileNode folder : folderList) {
				if (folder.getId().equals(folderId)) {
					return true; // Thư mục đã tồn tại
				}
			}
		}
		return false;
	}

	public boolean isFileExists(String fileId) {
		for (List<FileNode> fileList : files.values()) {
			for (FileNode file : fileList) {
				if (file.getId().equals(fileId)) {
					return true; // Tệp đã tồn tại
				}
			}
		}
		return false;
	}

	public void reset() {
		folders.clear();
		files.clear();
		loadedIds.clear();
		clickedIds.clear();
	}

	// Kiểm tra file có thuộc thư mục con nào không
	public boolean isFileChildOf(String parentId, String fileId) {
		List<FileNode> fileList = files.get(parentId);
		if (fileList != null) {
			for (FileNode file : fileList) {
				if (file.getId().equals(fileId)) {
					return true; // File này là con của thư mục
				}
			}
		}
		return false; // File này không phải là con của thư mục
	}

	// Xóa thư mục và tất cả thư mục con, file con khỏi node
	public void removeFolder(String parentId, String folderId) {
		List<FileNode> folderList = folders.get(parentId);
		if (folderList != null) {
			// Tìm thư mục cần xóa
			FileNode folderToRemove = null;
			for (FileNode folder : folderList) {
				if (folder.getId().equals(folderId)) {
					folderToRemove = folder;
					break;
				}
			}

			if (folderToRemove != null) {
				// Xóa tất cả các thư mục con của thư mục này (đệ quy)
				List<FileNode> subFolders = getFolders(folderToRemove.getId());
				for (FileNode subFolder : subFolders) {
					removeFolder(folderToRemove.getId(), subFolder.getId()); // Đệ quy xóa thư mục con
				}

				// Xóa tất cả các file con của thư mục này
				List<FileNode> subFiles = getFiles(folderToRemove.getId());
				for (FileNode file : subFiles) {
					removeFile(folderToRemove.getId(), file.getId()); // Xóa file con
				}

				// Cuối cùng, xóa thư mục khỏi danh sách của parentId
				folderList.remove(folderToRemove);
			}
		}
	}

	// Xóa file khỏi node
	public void removeFile(String parentId, String fileId) {
		List<FileNode> fileList = files.get(parentId);
		if (fileList != null) {
			fileList.removeIf(file -> file.getId().equals(fileId)); // Xóa file có id khớp
		}
	}

	public boolean isEmpty() {
		return folders.isEmpty() && files.isEmpty();
	}

}
