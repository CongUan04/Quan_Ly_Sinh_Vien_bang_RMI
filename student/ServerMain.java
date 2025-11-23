package rmi.student;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    public static void main(String[] args) {
        try {
            // Cổng RMI registry
            int port = 1099;

            // Tạo registry trong JVM (nếu đã chạy sẵn thì sẽ báo lỗi, ta sẽ bắt lỗi này)
            try {
                LocateRegistry.createRegistry(port);
                System.out.println("Đã tạo RMI registry tại cổng " + port);
            } catch (Exception e) {
                System.out.println("RMI registry có thể đã chạy sẵn tại cổng " + port);
            }

            // Khởi tạo đối tượng quản lý sinh viên
            StudentManagerImpl impl = new StudentManagerImpl();

            // Đăng ký đối tượng với registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("StudentManager", impl);

            System.out.println("Đối tượng StudentManager đã được đăng ký. Server sẵn sàng hoạt động.");
        } catch (Exception e) {
            System.err.println("Lỗi máy chủ: " + e.getMessage());
            e.printStackTrace();
        }
    }
}