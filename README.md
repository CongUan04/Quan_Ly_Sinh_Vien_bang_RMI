<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   QUẢN LÝ SINH VIÊN BẰNG RMI
</h2>
<div align="center">
    <p align="center">
        <img src="Images/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="Images/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="Images/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

## 1. 📖 Giới thiệu hệ thống
Ứng dụng **Quản lý Sinh viên** được xây dựng dựa trên công nghệ **Java RMI** cho phép **Client** (Java Swing) và **Server** (RMI Service) trao đổi dữ liệu qua mạng.  
- Quản lý sinh viên (thêm, sửa, xóa, xem chi tiết, lọc theo lớp)
- Quản lý học phần (thêm, sửa, xóa học phần)
- Nhập/Xuất điểm theo học phần (Chuyên cần – Kiểm tra 1 – Thi)
- Tự động tính điểm môn theo công thức trọng số tín chỉ và tính GPA tích lũy
- Báo cáo GPA theo lớp hoặc từng sinh viên
- Dữ liệu được lưu trữ bền vững trên **MongoDB** (collection `students` và `modules` trong database `studentdb`)

✨ Ứng dụng phù hợp cho việc học tập, nghiên cứu lập trình mạng và phân tán trong Java. Giao diện được chia thành hai tab: Quản lý Sinh viên và Quản lý Điểm, với chức năng tìm kiếm và menu hành động (Xem chi tiết, Sửa, Xóa) cho từng sinh viên.

---

## 2. 💻 Công nghệ sử dụng
- **Ngôn ngữ:** Java 17+
- **Giao diện:** Java Swing (Nimbus Look & Feel)
- **Truyền thông mạng:** Java RMI
- **Cơ sở dữ liệu:** MongoDB (driver `mongo-java-driver`)
- **IDE đề xuất:** IntelliJ IDEA / Eclipse / NetBeans

<p align="center">
  <a href="https://www.java.com/"><img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"></a>
  <a href="https://docs.oracle.com/javase/tutorial/uiswing/"><img src="https://img.shields.io/badge/Java%20Swing-007396?style=for-the-badge&logo=java&logoColor=white"></a>
  <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/"><img src="https://img.shields.io/badge/Java%20RMI-5382a1?style=for-the-badge&logo=java&logoColor=white"></a>
  <img src="https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white"/>
</p>

<p align="center">
  <a href="https://www.eclipse.org/"><img src="https://img.shields.io/badge/Eclipse-2C2255?style=for-the-badge&logo=eclipse&logoColor=white"></a>
  <a href="https://netbeans.apache.org/"><img src="https://img.shields.io/badge/NetBeans-1B6AC6?style=for-the-badge&logo=apache-netbeans-ide&logoColor=white"></a>
</p>

---

## 3. 📸 Hình ảnh các chức năng

### 🔹 🖼️ Giao diện chính (Java Swing)
- Giao diện với hai tab: **Quản lý Sinh viên** (danh sách sinh viên với các nút chức năng Xem chi tiết, Sửa, Xóa) và **Quản lý Học Phần** (chọn học phần và cập nhật điểm số).
- Hỗ trợ tìm kiếm theo tên hoặc ID, thêm sinh viên mới qua dialog.
<p align="center">
<img src="Images/GiaoDienChinh.png" alt="Giao diện chính" width="800"/>
</p>

### 🔹 Tab Quản lý Học Phần
- Chọn học phần từ dropdown (ví dụ: Lập Trình Mạng, Kỹ Năng Mềm), hiển thị bảng điểm số (Chuyên cần, Kiểm tra 1, Điểm thi) và nút Cập nhật cho từng sinh viên.
<p align="center">
<img src="Images/QuanLyHocPhan.png" alt="Tab Quản lý Học Phần" width="800"/>
</p>

### 🔹 Thêm sinh viên mới
- Dialog form nhập thông tin cơ bản (Mã SV, Họ tên, Năm sinh, Email, Lớp), với điểm mặc định 0 cho các học phần.
<p align="center">
<img src="Images/ThemSinhVien.png" alt="Thêm sinh viên mới" width="800"/>
</p>

### 🔹 Xem chi tiết sinh viên
- Dialog read-only hiển thị đầy đủ thông tin sinh viên, bao gồm điểm số chi tiết cho từng học phần.
<p align="center">
<img src="Images/XemChiTietSinhVien.png" alt="Xem chi tiết sinh viên" width="800"/>
</p>

### 🔹 Cập nhật sinh viên
- Dialog form chỉnh sửa thông tin cơ bản (Mã SV, Họ tên, Năm sinh, Email, Lớp).
<p align="center">
<img src="Images/CapNhatSinhVien.png" alt="Cập nhật sinh viên" width="800"/>
</p>

### 🔹 Cập nhật điểm số học phần
- Dialog form cập nhật điểm cụ thể cho một học phần (Chuyên cần, Kiểm tra 1, Điểm thi).
<p align="center">
<img src="Images/CapNhatDiemSo.png" alt="Cập nhật điểm số" width="800"/>
</p>

### 🔹 Báo cáo GPA theo lớp
<p align="center">
<img src="Images/BaoCao.png" alt="Cập nhật điểm số" width="800"/>
</p>

---

## 4. 🚀 Các bước cài đặt

### 🔹 1. Cài đặt môi trường
- Cài **Java JDK 8+** (tải từ [Oracle](https://www.oracle.com/java/technologies/downloads/) hoặc OpenJDK).  
- Không cần cơ sở dữ liệu bên ngoài vì sử dụng file CSV.  

### 🔹 2. Clone repository
```sh
git clone <repository_url>
cd <repository_folder>
```

### 🔹 3. Biên dịch project
Mở terminal tại thư mục dự án:
```sh
javac -d . src/rmi/student/*.java
```
(Lưu ý: Thay `src` bằng đường dẫn thư mục chứa code nếu cần.)

Hoặc mở project trong IDE (IntelliJ, Eclipse) và build.

---

## 5. Liên hệ
- Email: nguyenconguan04@gmail.com

---

<div align="center">
    <p>Developed by [Nguyen Cong Uan] - Faculty of Information Technology, DaiNam University</p>
</div>
