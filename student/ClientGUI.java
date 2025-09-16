package rmi.student;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.Naming;
import java.util.*;
import java.util.List;

public class ClientGUI extends JFrame {
    private StudentManager manager;
    private JTable studentTable;
    private DefaultTableModel studentTableModel;
    private JTable scoreTable;
    private DefaultTableModel scoreTableModel;
    private JTextField searchField;
    private JComboBox<String> moduleComboBox;  // Mới: Chọn học phần

    // Màu sắc thân thiện
    static final Color PRIMARY_COLOR = new Color(52, 152, 219); // Xanh dương
    static final Color SECONDARY_COLOR = new Color(46, 204, 113); // Xanh lá
    static final Color ACCENT_COLOR = new Color(231, 76, 60); // Đỏ
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250); // Xám nhạt
    private static final Color PANEL_COLOR = new Color(255, 255, 255); // Trắng
    private static final Color TEXT_COLOR = new Color(33, 37, 41); // Đen đậm

    public ClientGUI() {
        // Áp dụng Look and Feel hiện đại
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("nimbusBase", PRIMARY_COLOR);
            UIManager.put("nimbusBlueGrey", BACKGROUND_COLOR);
            UIManager.put("control", PANEL_COLOR);
        } catch (Exception e) {
            // Fallback nếu không hỗ trợ Nimbus
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Ignore
            }
        }

        try {
            manager = (StudentManager) Naming.lookup("rmi://localhost:1099/StudentManager");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến Server: " + e.getMessage(), "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setTitle("Quản lý Sinh viên - RMI Client");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Áp dụng font chung dễ đọc
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(PANEL_COLOR);
        tabbedPane.setForeground(TEXT_COLOR);

        // Tab 1: Quản lý Sinh viên
        JPanel studentPanel = createStudentPanel();
        tabbedPane.addTab("Quản lý Sinh viên", createIconTab("👤", studentPanel));

        // Tab 2: Quản lý Học Phần
        JPanel scorePanel = createScorePanel();
        tabbedPane.addTab("Quản lý Học Phần", createIconTab("📚", scorePanel));

        add(tabbedPane);

        loadStudents(); // Load dữ liệu cho tab sinh viên
        loadModules();  // Load học phần và điểm mặc định
    }

    // Helper tạo tab với icon (emoji cho thân thiện)
    private JPanel createIconTab(String icon, JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PANEL_COLOR);
        wrapper.add(panel);
        return wrapper;
    }

    // Tạo panel cho Quản lý Sinh viên
    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tiêu đề
        JLabel titleLabel = new JLabel("Danh sách sinh viên", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_COLOR);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Bảng sinh viên
        studentTableModel = new DefaultTableModel(new Object[]{"Mã SV", "Họ và tên", "Lớp", "Năm sinh", "Email", "Xem chi tiết", "Sửa", "Xóa"}, 0);
        studentTable = new JTable(studentTableModel) {
            public boolean isCellEditable(int row, int column) {
                return column >= 5; // Cho phép chỉnh sửa ở cột 5,6,7 (Xem, Sửa, Xóa)
            }
        };
        studentTable.setRowHeight(30);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        studentTable.setGridColor(new Color(220, 220, 220));
        studentTable.setSelectionBackground(SECONDARY_COLOR);

        // Render và Editor cho từng cột nút
        setupButtonColumn(studentTable, 5, "Xem chi tiết", new ViewButtonEditor(new JCheckBox(), this), PRIMARY_COLOR);
        setupButtonColumn(studentTable, 6, "Sửa", new EditButtonEditor(new JCheckBox(), this), SECONDARY_COLOR);
        setupButtonColumn(studentTable, 7, "Xóa", new DeleteButtonEditor(new JCheckBox(), this), ACCENT_COLOR);

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Các nút chức năng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(PANEL_COLOR);
        JButton addButton = createStyledButton("Thêm", SECONDARY_COLOR, Color.WHITE);
        searchField = new JTextField(15);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        JButton searchButton = createStyledButton("Tìm kiếm", PRIMARY_COLOR, Color.WHITE);
        JButton refreshButton = createStyledButton("Làm mới", Color.GRAY, Color.WHITE);

        buttonPanel.add(addButton);
        buttonPanel.add(new JLabel("Tìm kiếm:"));
        buttonPanel.add(searchField);
        buttonPanel.add(searchButton);
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Sự kiện
        addButton.addActionListener(e -> showAddDialog());
        searchButton.addActionListener(e -> searchStudents());
        refreshButton.addActionListener(e -> {
            loadStudents();
            loadModules(); // Đồng bộ với tab học phần
        });

        return panel;
    }

    // Helper để setup cột nút với màu
    private void setupButtonColumn(JTable table, int columnIndex, String buttonText, DefaultCellEditor editor, Color bgColor) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setCellRenderer(new StyledButtonRenderer(buttonText, bgColor));
        column.setCellEditor(editor);
        column.setPreferredWidth(100);
    }

    // Tạo nút styled
    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // Tạo panel cho Quản lý Học Phần
    private JPanel createScorePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tiêu đề
        JLabel titleLabel = new JLabel("Danh sách học phần sinh viên", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_COLOR);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Panel chọn học phần
        JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        selectPanel.setBackground(PANEL_COLOR);
        selectPanel.add(new JLabel("Chọn học phần:"));
        moduleComboBox = new JComboBox<>();
        moduleComboBox.setBackground(Color.WHITE);
        moduleComboBox.setForeground(TEXT_COLOR);
        moduleComboBox.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1));
        moduleComboBox.addActionListener(e -> {
            String selectedModule = (String) moduleComboBox.getSelectedItem();
            if (selectedModule != null) {
                loadModuleScores(selectedModule);
            }
        });
        selectPanel.add(moduleComboBox);
        panel.add(selectPanel, BorderLayout.NORTH);

        // Bảng học phần
        scoreTableModel = new DefaultTableModel(new Object[]{"Mã SV", "Họ và tên", "Chuyên cần", "Kiểm tra 1", "Điểm thi", "Cập nhật"}, 0);
        scoreTable = new JTable(scoreTableModel) {
            public boolean isCellEditable(int row, int column) {
                return column == 5; // chỉ cho cột Cập nhật bấm được
            }
        };
        scoreTable.setRowHeight(30);
        scoreTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        scoreTable.setGridColor(new Color(220, 220, 220));
        scoreTable.setSelectionBackground(SECONDARY_COLOR);

        // Render nút trong cột "Cập nhật"
        scoreTable.getColumn("Cập nhật").setCellRenderer(new StyledButtonRenderer("Cập nhật", SECONDARY_COLOR));
        scoreTable.getColumn("Cập nhật").setCellEditor(new ModuleButtonEditor(new JCheckBox(), this));

        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Nút làm mới
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(PANEL_COLOR);
        JButton refreshButton = createStyledButton("Làm mới", Color.GRAY, Color.WHITE);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> {
            loadModules();
            loadStudents(); // Đồng bộ với tab sinh viên
        });

        return panel;
    }

    // Load danh sách học phần vào ComboBox
    private void loadModules() {
        try {
            List<String> modules = manager.getAllModules();
            moduleComboBox.removeAllItems();
            for (String module : modules) {
                moduleComboBox.addItem(module);
            }
            if (!modules.isEmpty()) {
                moduleComboBox.setSelectedIndex(0);
                loadModuleScores(modules.get(0));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải học phần: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load điểm cho học phần được chọn
    private void loadModuleScores(String moduleName) {
        try {
            scoreTableModel.setRowCount(0); // clear
            List<Student> students = manager.getStudentsWithScoresForModule(moduleName);
            for (Student s : students) {
                Student.SubjectScores scores = s.getScoresForModule(moduleName);
                scoreTableModel.addRow(new Object[]{
                    s.getId(), s.getName(), 
                    scores.getAttendance(), scores.getTest1(), scores.getExam(), 
                    "Cập nhật"
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải danh sách điểm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load dữ liệu cho bảng sinh viên
    private void loadStudents() {
        try {
            studentTableModel.setRowCount(0); // clear
            List<Student> students = manager.getAllStudents();
            for (Student s : students) {
                studentTableModel.addRow(new Object[]{s.getId(), s.getName(), s.getClassName(), s.getYear(), s.getEmail(), "Xem chi tiết", "Sửa", "Xóa"});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải danh sách: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Tìm kiếm sinh viên theo ID hoặc Name
    private void searchStudents() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadStudents();
            return;
        }

        try {
            studentTableModel.setRowCount(0); // clear
            List<Student> students = manager.getAllStudents();
            for (Student s : students) {
                if (s.getId().toLowerCase().contains(query) || s.getName().toLowerCase().contains(query)) {
                    studentTableModel.addRow(new Object[]{s.getId(), s.getName(), s.getClassName(), s.getYear(), s.getEmail(), "Xem chi tiết", "Sửa", "Xóa"});
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Form thêm sinh viên
    private void showAddDialog() {
        JDialog dialog = createStyledDialog("Thêm sinh viên", 350, 300);
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField idField = createStyledTextField();
        JTextField nameField = createStyledTextField();
        JTextField yearField = createStyledTextField();
        JTextField emailField = createStyledTextField();
        String[] classes = {"CNTT 16-01", "CNTT 16-02", "CNTT 16-03"};
        JComboBox<String> classComboBox = new JComboBox<>(classes);
        classComboBox.setBackground(Color.WHITE);
        classComboBox.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1));

        formPanel.add(createStyledLabel("Mã SV:"));
        formPanel.add(idField);
        formPanel.add(createStyledLabel("Họ và tên:"));
        formPanel.add(nameField);
        formPanel.add(createStyledLabel("Lớp:"));
        formPanel.add(classComboBox);
        formPanel.add(createStyledLabel("Năm sinh:"));
        formPanel.add(yearField);
        formPanel.add(createStyledLabel("Email:"));
        formPanel.add(emailField);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton okButton = createStyledButton("Lưu", SECONDARY_COLOR, Color.WHITE);
        JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            try {
                Student s = new Student(
                        idField.getText(),
                        nameField.getText(),
                        Integer.parseInt(yearField.getText()),
                        emailField.getText(),
                        (String) classComboBox.getSelectedItem()
                );
                if (manager.addStudent(s)) {
                    JOptionPane.showMessageDialog(dialog, "Thêm sinh viên thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    loadStudents();
                    loadModules();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Mã sinh viên đã tồn tại!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    // Hoàn thiện form chỉnh sửa thông tin sinh viên (tương tự add, nhưng load dữ liệu)
    public void showEditStudentDialog(String id) {
        try {
            Student s = manager.getStudentById(id);
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JDialog dialog = createStyledDialog("Sửa sinh viên", 350, 300);
            JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JTextField idField = createStyledTextField(s.getId());
            idField.setEditable(false);
            JTextField nameField = createStyledTextField(s.getName());
            JTextField yearField = createStyledTextField(String.valueOf(s.getYear()));
            JTextField emailField = createStyledTextField(s.getEmail());
            String[] classes = {"CNTT 16-01", "CNTT 16-02", "CNTT 16-03"};
            JComboBox<String> classComboBox = new JComboBox<>(classes);
            classComboBox.setSelectedItem(s.getClassName());
            classComboBox.setBackground(Color.WHITE);
            classComboBox.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1));

            formPanel.add(createStyledLabel("Mã SV:"));
            formPanel.add(idField);
            formPanel.add(createStyledLabel("Họ và tên:"));
            formPanel.add(nameField);
            formPanel.add(createStyledLabel("Lớp:"));
            formPanel.add(classComboBox);
            formPanel.add(createStyledLabel("Năm sinh:"));
            formPanel.add(yearField);
            formPanel.add(createStyledLabel("Email:"));
            formPanel.add(emailField);

            dialog.add(formPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            JButton okButton = createStyledButton("Lưu", SECONDARY_COLOR, Color.WHITE);
            JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            okButton.addActionListener(e -> {
                try {
                    s.setName(nameField.getText());
                    s.setYear(Integer.parseInt(yearField.getText()));
                    s.setEmail(emailField.getText());
                    s.setClassName((String) classComboBox.getSelectedItem());
                    if (manager.updateStudent(s)) {
                        JOptionPane.showMessageDialog(dialog, "Cập nhật thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadStudents();
                        loadModules();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Không thể cập nhật!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelButton.addActionListener(e -> dialog.dispose());
            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Form xem chi tiết sinh viên
    public void showDetailDialog(String id) {
        try {
            Student s = manager.getStudentById(id);
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JDialog dialog = createStyledDialog("Chi tiết sinh viên", 450, 400);
            JTextArea detailArea = new JTextArea();
            detailArea.setEditable(false);
            detailArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            detailArea.setBackground(PANEL_COLOR);
            detailArea.setForeground(TEXT_COLOR);
            detailArea.append("Mã SV: " + s.getId() + "\n");
            detailArea.append("Họ và tên: " + s.getName() + "\n");
            detailArea.append("Lớp: " + s.getClassName() + "\n");
            detailArea.append("Năm sinh: " + s.getYear() + "\n");
            detailArea.append("Email: " + s.getEmail() + "\n\n");
            detailArea.append("Học phần và điểm:\n");
            for (Map.Entry<String, Student.SubjectScores> entry : s.getSubjectScores().entrySet()) {
                Student.SubjectScores sc = entry.getValue();
                detailArea.append("- " + entry.getKey() + ": Chuyên cần=" + sc.getAttendance() + 
                                  ", Kiểm tra 1=" + sc.getTest1() + ", Điểm thi=" + sc.getExam() + "\n");
            }

            JScrollPane scrollPane = new JScrollPane(detailArea);
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            dialog.add(scrollPane, BorderLayout.CENTER);

            JButton closeButton = createStyledButton("Đóng", Color.GRAY, Color.WHITE);
            closeButton.addActionListener(e -> dialog.dispose());
            dialog.add(closeButton, BorderLayout.SOUTH);

            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Xóa sinh viên
    public void deleteStudent(String id, String name) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa sinh viên " + name + " không?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (manager.deleteStudent(id)) {
                    JOptionPane.showMessageDialog(this, "Xóa thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    loadStudents();
                    loadModules();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên để xóa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Form cập nhật điểm học phần
    public void showEditScoreDialog(String id) {
        String moduleName = (String) moduleComboBox.getSelectedItem();
        if (moduleName == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn học phần!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        showEditModuleScoreDialog(id, moduleName);
    }

    // Internal: Form cập nhật điểm cụ thể cho học phần
    private void showEditModuleScoreDialog(String id, String moduleName) {
        try {
            Student s = manager.getStudentById(id);
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Student.SubjectScores currentScores = s.getScoresForModule(moduleName);

            JDialog dialog = createStyledDialog("Cập nhật điểm - " + moduleName, 400, 300);
            JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JTextField idField = createStyledTextField(s.getId());
            idField.setEditable(false);
            JTextField nameField = createStyledTextField(s.getName());
            nameField.setEditable(false);
            JTextField attendanceField = createStyledTextField(String.valueOf(currentScores.getAttendance()));
            JTextField test1Field = createStyledTextField(String.valueOf(currentScores.getTest1()));
            JTextField examField = createStyledTextField(String.valueOf(currentScores.getExam()));

            formPanel.add(createStyledLabel("Mã SV:"));
            formPanel.add(idField);
            formPanel.add(createStyledLabel("Họ và tên:"));
            formPanel.add(nameField);
            formPanel.add(createStyledLabel("Chuyên cần:"));
            formPanel.add(attendanceField);
            formPanel.add(createStyledLabel("Kiểm tra 1:"));
            formPanel.add(test1Field);
            formPanel.add(createStyledLabel("Điểm thi:"));
            formPanel.add(examField);

            dialog.add(formPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            JButton saveButton = createStyledButton("Lưu", SECONDARY_COLOR, Color.WHITE);
            JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            saveButton.addActionListener(e -> {
                try {
                    Student.SubjectScores newScores = new Student.SubjectScores(
                        Double.parseDouble(attendanceField.getText()),
                        Double.parseDouble(test1Field.getText()),
                        Double.parseDouble(examField.getText())
                    );
                    Map<String, Student.SubjectScores> updates = new HashMap<>();
                    updates.put(id, newScores);
                    if (manager.updateScoresForModule(moduleName, updates)) {
                        JOptionPane.showMessageDialog(dialog, "Cập nhật điểm thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadModules();
                        loadStudents();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Không thể cập nhật điểm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelButton.addActionListener(e -> dialog.dispose());

            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper tạo dialog styled
    private JDialog createStyledDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(PANEL_COLOR);
        dialog.setLayout(new BorderLayout(10, 10));
        return dialog;
    }

    // Helper tạo label styled
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_COLOR);
        return label;
    }

    // Helper tạo text field styled
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return field;
    }

    private JTextField createStyledTextField(String text) {
        JTextField field = createStyledTextField();
        field.setText(text);
        return field;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}

// Renderer cho nút styled
class StyledButtonRenderer extends JButton implements TableCellRenderer {
    public StyledButtonRenderer(String text, Color bgColor) {
        setText(text);
        setBackground(bgColor);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setFocusPainted(false);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        setText(value != null ? value.toString() : "Cập nhật");
        return this;
    }
}

// Các editor giữ nguyên, chỉ cập nhật button trong constructor
class ViewButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String id;
    private boolean clicked;
    private ClientGUI parent;

    public ViewButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox);
        this.parent = parent;
        button = new JButton("Xem chi tiết");
        button.setBackground(parent.PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        clicked = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (clicked) {
            parent.showDetailDialog(id);
        }
        clicked = false;
        return "Xem chi tiết";
    }
}

class EditButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String id;
    private boolean clicked;
    private ClientGUI parent;

    public EditButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox);
        this.parent = parent;
        button = new JButton("Sửa");
        button.setBackground(parent.SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        clicked = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (clicked) {
            parent.showEditStudentDialog(id);
        }
        clicked = false;
        return "Sửa";
    }
}

class DeleteButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String id;
    private String name;
    private boolean clicked;
    private ClientGUI parent;

    public DeleteButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox);
        this.parent = parent;
        button = new JButton("Xóa");
        button.setBackground(parent.ACCENT_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        name = (String) table.getValueAt(row, 1);
        clicked = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (clicked) {
            parent.deleteStudent(id, name);
        }
        clicked = false;
        return "Xóa";
    }
}

class ModuleButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String id;
    private boolean clicked;
    private ClientGUI parent;

    public ModuleButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox);
        this.parent = parent;
        button = new JButton("Cập nhật");
        button.setBackground(parent.SECONDARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        clicked = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (clicked) {
            parent.showEditScoreDialog(id);
        }
        clicked = false;
        return "Cập nhật";
    }
}