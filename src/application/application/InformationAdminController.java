package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class InformationAdminController {

    @FXML
    private TextField txtMSSV;

    @FXML
    private TextField txtTen;

    @FXML
    private TextField txtLop;

    @FXML
    private TextField txtData;

    @FXML
    private TextField txtDataMax;

    @FXML
    private Label lbRoleName;

    @FXML
    private TextField txtRoleName;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void loadInformation(String mssv) {
        // Lấy thông tin từ server dựa trên mssv
        getInformationFromServer(mssv);
    }

    private void getInformationFromServer(String mssv) {
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
                    txtMSSV.setText(mssvValue);
                } else if (line.startsWith("Name:")) {
                    String tenValue = line.split(": ")[1].trim();
                    txtTen.setText(tenValue);
                } else if (line.startsWith("Class:")) {
                    String lopValue = line.split(": ")[1].trim();
                    txtLop.setText(lopValue);
                } else if (line.startsWith("Data:")) {
                    String dataValue = line.split(": ")[1].trim();
                    txtData.setText(dataValue);
                } else if (line.startsWith("DataMax:")) {
                    String dataMaxValue = line.split(": ")[1].trim();
                    txtDataMax.setText(dataMaxValue);
                } else if (line.startsWith("RoleName:")) {
                    String roleName = line.split(": ")[1].trim();
                    txtRoleName.setText(roleName);
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
}