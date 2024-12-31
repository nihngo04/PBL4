package application;

public class User {
    private String mssv;
    private String name;

    public User(String mssv, String name) {
        this.mssv = mssv;
        this.name = name;
    }

    public String getMssv() {
        return mssv;
    }

    public void setMssv(String mssv) {
        this.mssv = mssv;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

