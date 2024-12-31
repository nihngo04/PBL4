package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AddFormController {
	@FXML
    private TextField txtStudentID;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtClass;

    @FXML
    private TextField txtPass;

    @FXML
    private Label lblResponse;

    @FXML
    private void Register(ActionEvent event) {
        String studentID = txtStudentID.getText();
        String name = txtName.getText();
        String className = txtClass.getText();
        String password = txtPass.getText();

        if (studentID.isEmpty() || name.isEmpty() || className.isEmpty() || password.isEmpty()) {
            lblResponse.setText("Tất cả các trường phải được điền");
            return; 
        }

        try {
            Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("Register");
            dos.writeUTF(studentID);
            dos.writeUTF(name);
            dos.writeUTF(className);
            dos.writeUTF(password);

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String response = dis.readUTF();
            lblResponse.setText(response);

            dos.close();
            dis.close();
            socket.close();

        } catch (IOException e) {
            lblResponse.setText("Error: " + e.getMessage());
        }
    }
    @FXML
    void Cancel(ActionEvent event) {
    	txtStudentID.clear();
    	txtName.clear();
    	txtClass.clear();
        txtPass.clear();
        lblResponse.setText(""); 
    }
}
