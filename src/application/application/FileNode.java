package application;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class FileNode {
//    private String id;
//    private String name;
//    private boolean isFolder; // true nếu là thư mục, false nếu là file
//    private List<FileNode> children; // Danh sách con nếu là thư mục
//
//    public FileNode(String id, String name, boolean isFolder) {
//        this.id = id;
//        this.name = name;
//        this.isFolder = isFolder;
//        this.children = new ArrayList<>();
//    }
//
//    // Getter và Setter
//    public String getId() {
//        return id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public boolean isFolder() {
//        return isFolder;
//    }
//
//    public List<FileNode> getChildren() {
//        return children;
//    }
//
//    public void addChild(FileNode child) {
//        this.children.add(child);
//    }
//}
//
public class FileNode {
    private String id;
    private String name;
    private boolean isFolder; // true: thư mục, false: file

    public FileNode(String id, String name, boolean isFolder) {
        this.id = id;
        this.name = name;
        this.isFolder = isFolder;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
 // Setter cho name
    public void setName(String name) {
        this.name = name;
    }

    public boolean isFolder() {
        return isFolder;
    }
}

