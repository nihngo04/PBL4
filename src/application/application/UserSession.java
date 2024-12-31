package application;

public class UserSession {
	private static UserSession instance;
	private String MSSV; // Mã sinh viên
	private String ten; // Tên sinh viên
	private String lop; // Lớp sinh viên
	private String data; // Dữ liệu bổ sung
    private String root;  // Add root field to store the root 
    private String node;



	// Constructor riêng tư
	private UserSession() {
	}

	// Phương thức để lấy thể hiện singleton
	public static UserSession getInstance() {
		if (instance == null) {
			instance = new UserSession();
		}
		return instance;
	}

	// Getter và setter cho mã sinh viên
	public String getMSSV() {
		return MSSV;
	}

	public void setMSSV(String MSSV) {
		this.MSSV = MSSV;
	}

	// Getter và setter cho tên sinh viên
	public String getTen() {
		return ten;
	}

	public void setTen(String ten) {
		this.ten = ten;
	}

	// Getter và setter cho lớp sinh viên
	public String getLop() {
		return lop;
	}

	public void setLop(String lop) {
		this.lop = lop;
	}

	// Getter và setter cho dữ liệu bổ sung (Data)
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	
	public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
    
    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }
	
	public void clearSession() {
		MSSV = null;
        ten = null;
        lop = null;
        data = null;
    }
}
