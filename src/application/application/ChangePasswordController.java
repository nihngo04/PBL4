package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;


public class ChangePasswordController {

    @FXML
    private PasswordField txtOldPw;
    
    @FXML
    private PasswordField txtNewPassword;
    
    @FXML
    private PasswordField txtConfirmPassword;
    
    @FXML
    void handleChangePassword(ActionEvent event) {
    	
        String oldPassword = txtOldPw.getText();
        String newPassword = txtNewPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String MSSV = UserSession.getInstance().getMSSV(); // Lấy MSSV từ UserSession
        
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showAlert(AlertType.WARNING, "Cảnh báo", "Mật khẩu xác nhận không trùng khớp.");
            return;
        }

        try {
            // Kết nối đến server
            Socket socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            // Gửi yêu cầu cập nhật mật khẩu
            dos.writeUTF("UpdatePassword");
            dos.writeUTF(MSSV);
            dos.writeUTF(oldPassword);
            dos.writeUTF(newPassword);

            // Nhận kết quả từ server
            String response = input.readUTF();
            if ("Update password successfully".equals(response)) {
                showAlert(AlertType.INFORMATION, "Thành công", "Cập nhật mật khẩu thành công.");
            } else {
                showAlert(AlertType.ERROR, "Lỗi", "Cập nhật mật khẩu không thành công: " + response);
            }

            // Đóng kết nối
            input.close();
            dos.close();	
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Lỗi", "Lỗi khi cập nhật mật khẩu: " + e.getMessage());
        }
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    void handleCancel(ActionEvent event) {
    	Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
	    stage.close();
    }
}

