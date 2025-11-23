package rmi.student;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.Naming;
import java.util.*;
import java.util.List;

public class ClientGUI extends JFrame {
    private StudentManager manager;
    private JTable studentTable, scoreTable, moduleTable, gpaTable;
    private DefaultTableModel studentTableModel, scoreTableModel, moduleTableModel, gpaTableModel;
    private JTextField searchField;
    private JComboBox<String> scoreClassComboBox, gpaClassComboBox, moduleComboBox;
    JPanel studentCardPanel;
    private JLabel titleLabelStudent;
    private String currentClass;

    static final Color PRIMARY_COLOR = new Color(173, 216, 230); // Light Blue Pastel
    static final Color SECONDARY_COLOR = new Color(152, 251, 152); // Light Green Pastel
    static final Color ACCENT_COLOR = new Color(255, 182, 193); // Light Pink Pastel
    private static final Color BACKGROUND_COLOR = new Color(240, 248, 255); // Alice Blue Pastel
    private static final Color PANEL_COLOR = new Color(255, 255, 255);
    private static final Color TEXT_COLOR = new Color(70, 130, 180); // Steel Blue

    private JPanel contentPanel;
    private CardLayout contentLayout;

    public ClientGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("nimbusBase", PRIMARY_COLOR);
            UIManager.put("nimbusBlueGrey", BACKGROUND_COLOR);
            UIManager.put("control", PANEL_COLOR);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
            }
        }

        try {
            manager = (StudentManager) Naming.lookup("rmi://localhost:1099/StudentManager");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến Server: " + e.getMessage(), "Lỗi Kết Nối",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setTitle("Quản lý Sinh viên - RMI Client");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);

        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 14); // Larger font for better usability
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);

        // Create sidebar
        JPanel sidebar = createSidebar();

        // Create content panel with CardLayout
        contentPanel = new JPanel();
        contentLayout = new CardLayout();
        contentPanel.setLayout(contentLayout);
        contentPanel.setBackground(PANEL_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Add panels to content
        contentPanel.add(createStudentPanel(), "students");
        contentPanel.add(createModulePanel(), "modules");
        contentPanel.add(createScorePanel(), "scores");
        contentPanel.add(createGPAPanel(), "gpa");

        // Main layout
        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Initial view
        contentLayout.show(contentPanel, "students");

        loadClassesWithCounts();
        loadModules();
        loadClasses();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(230, 240, 255)); // Soft pastel blue
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 10, 20, 10)));

        JLabel sidebarTitle = new JLabel("Menu", SwingConstants.CENTER);
        sidebarTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sidebarTitle.setForeground(TEXT_COLOR);
        sidebar.add(sidebarTitle);
        sidebar.add(Box.createVerticalStrut(20));

        JButton studentBtn = createSidebarButton("Quản lý Sinh viên",
                e -> contentLayout.show(contentPanel, "students"));
        JButton moduleBtn = createSidebarButton("Quản lý Học Phần",
                e -> contentLayout.show(contentPanel, "modules"));
        JButton scoreBtn = createSidebarButton("Quản lý Điểm", e -> contentLayout.show(contentPanel, "scores"));
        JButton gpaBtn = createSidebarButton("Báo cáo GPA", e -> contentLayout.show(contentPanel, "gpa"));

        sidebar.add(studentBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(moduleBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(scoreBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(gpaBtn);

        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton createSidebarButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setBackground(PANEL_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(listener);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(SECONDARY_COLOR);
                button.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(PANEL_COLOR);
                button.setForeground(TEXT_COLOR);
            }
        });
        return button;
    }

    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        studentCardPanel = new JPanel(new CardLayout());

        // Class list panel
        JPanel classListPanel = new JPanel(new BorderLayout(10, 10));
        classListPanel.setBackground(PANEL_COLOR);

        JLabel titleClass = new JLabel("Danh sách lớp", SwingConstants.CENTER);
        titleClass.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleClass.setForeground(PRIMARY_COLOR);
        classListPanel.add(titleClass, BorderLayout.NORTH);

        DefaultTableModel classTableModel = new DefaultTableModel(
                new Object[] { "Lớp", "Số lượng sinh viên", "Xem danh sách" }, 0);
        JTable classTable = new JTable(classTableModel) {
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
        classTable.setRowHeight(40); // Larger rows for easier interaction
        classTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        classTable.setGridColor(new Color(220, 220, 220));
        classTable.setSelectionBackground(SECONDARY_COLOR);
        setupButtonColumn(classTable, 2, "Xem danh sách", new ViewClassButtonEditor(new JCheckBox(), this),
                PRIMARY_COLOR);

        JScrollPane scrollClass = new JScrollPane(classTable);
        scrollClass.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        classListPanel.add(scrollClass, BorderLayout.CENTER);

        JPanel buttonPanelClass = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanelClass.setBackground(PANEL_COLOR);
        JButton addClassButton = createStyledButton("Thêm lớp", SECONDARY_COLOR, Color.WHITE);
        JButton addButtonClass = createStyledButton("Thêm sinh viên", SECONDARY_COLOR, Color.WHITE);
        JButton refreshClass = createStyledButton("Làm mới", Color.GRAY, Color.WHITE);
        buttonPanelClass.add(addClassButton);
        buttonPanelClass.add(addButtonClass);
        buttonPanelClass.add(refreshClass);
        classListPanel.add(buttonPanelClass, BorderLayout.SOUTH);

        addClassButton.addActionListener(e -> showAddClassDialog());
        addButtonClass.addActionListener(e -> showAddDialog(null));
        refreshClass.addActionListener(e -> loadClassesWithCounts());

        // Student list panel
        JPanel studentListPanel = new JPanel(new BorderLayout(10, 10));
        studentListPanel.setBackground(PANEL_COLOR);

        titleLabelStudent = new JLabel("Danh sách sinh viên", SwingConstants.CENTER);
        titleLabelStudent.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabelStudent.setForeground(PRIMARY_COLOR);
        studentListPanel.add(titleLabelStudent, BorderLayout.NORTH);

        studentTableModel = new DefaultTableModel(
                new Object[] { "Mã SV", "Họ và tên", "Lớp", "Năm sinh", "Email", "Xem chi tiết", "Sửa", "Xóa" }, 0);
        studentTable = new JTable(studentTableModel) {
            public boolean isCellEditable(int row, int column) {
                return column >= 5;
            }
        };
        studentTable.setRowHeight(40); // Larger rows
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        studentTable.setGridColor(new Color(220, 220, 220));
        studentTable.setSelectionBackground(SECONDARY_COLOR);
        setupButtonColumn(studentTable, 5, "Xem chi tiết", new ViewButtonEditor(new JCheckBox(), this), PRIMARY_COLOR);
        setupButtonColumn(studentTable, 6, "Sửa", new EditButtonEditor(new JCheckBox(), this), SECONDARY_COLOR);
        setupButtonColumn(studentTable, 7, "Xóa", new DeleteButtonEditor(new JCheckBox(), this), ACCENT_COLOR);

        JScrollPane scrollStudent = new JScrollPane(studentTable);
        scrollStudent.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        studentListPanel.add(scrollStudent, BorderLayout.CENTER);

        JPanel buttonPanelStudent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanelStudent.setBackground(PANEL_COLOR);
        JButton backButton = createStyledButton("Quay lại", Color.GRAY, Color.WHITE);
        JButton addButtonStudent = createStyledButton("Thêm", SECONDARY_COLOR, Color.WHITE);
        searchField = new JTextField(15);
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JButton searchButton = createStyledButton("Tìm kiếm", PRIMARY_COLOR, Color.WHITE);
        JButton refreshStudent = createStyledButton("Làm mới", Color.GRAY, Color.WHITE);
        buttonPanelStudent.add(backButton);
        buttonPanelStudent.add(addButtonStudent);
        buttonPanelStudent.add(new JLabel("Tìm kiếm:"));
        buttonPanelStudent.add(searchField);
        buttonPanelStudent.add(searchButton);
        buttonPanelStudent.add(refreshStudent);
        studentListPanel.add(buttonPanelStudent, BorderLayout.SOUTH);

        backButton.addActionListener(e -> {
            ((CardLayout) studentCardPanel.getLayout()).show(studentCardPanel, "classes");
            loadClassesWithCounts();
        });
        addButtonStudent.addActionListener(e -> showAddDialog(currentClass));
        searchButton.addActionListener(e -> searchStudents());
        refreshStudent.addActionListener(e -> loadStudentsForClass(currentClass));

        studentCardPanel.add(classListPanel, "classes");
        studentCardPanel.add(studentListPanel, "students");
        ((CardLayout) studentCardPanel.getLayout()).show(studentCardPanel, "classes");

        panel.add(studentCardPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createModulePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Quản lý Học Phần", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_COLOR);
        panel.add(title, BorderLayout.NORTH);

        moduleTableModel = new DefaultTableModel(new Object[] { "Mã HP", "Tên HP", "Số tín chỉ", "Sửa", "Xóa" }, 0);
        moduleTable = new JTable(moduleTableModel) {
            public boolean isCellEditable(int row, int column) {
                return column >= 3;
            }
        };
        moduleTable.setRowHeight(30);
        moduleTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        moduleTable.setGridColor(new Color(220, 220, 220));
        moduleTable.setSelectionBackground(SECONDARY_COLOR);
        setupButtonColumn(moduleTable, 3, "Sửa", new EditModuleButtonEditor(new JCheckBox(), this), SECONDARY_COLOR);
        setupButtonColumn(moduleTable, 4, "Xóa", new DeleteModuleButtonEditor(new JCheckBox(), this), ACCENT_COLOR);

        JScrollPane scrollModule = new JScrollPane(moduleTable);
        scrollModule.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollModule, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(PANEL_COLOR);
        JButton addButton = createStyledButton("Thêm học phần", SECONDARY_COLOR, Color.WHITE);
        JButton refreshButton = createStyledButton("Làm mới", Color.GRAY, Color.WHITE);
        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> showAddModuleDialog());
        refreshButton.addActionListener(e -> loadModules());

        return panel;
    }

    private JPanel createScorePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Quản lý Điểm", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_COLOR);
        panel.add(title, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(PANEL_COLOR);
        JLabel classLabel = new JLabel("Chọn lớp:");
        scoreClassComboBox = new JComboBox<>();
        scoreClassComboBox.setPreferredSize(new Dimension(150, 30));
        JLabel moduleLabel = new JLabel("Chọn học phần:");
        moduleComboBox = new JComboBox<>();
        moduleComboBox.setPreferredSize(new Dimension(150, 30));
        filterPanel.add(classLabel);
        filterPanel.add(scoreClassComboBox);
        filterPanel.add(moduleLabel);
        filterPanel.add(moduleComboBox);
        panel.add(filterPanel, BorderLayout.NORTH);

        scoreTableModel = new DefaultTableModel(
                new Object[] { "Mã SV", "Họ và tên", "Lớp", "Chuyên cần", "Kiểm tra 1", "Thi", "Cập nhật" }, 0);
        scoreTable = new JTable(scoreTableModel) {
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        scoreTable.setRowHeight(30);
        scoreTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        scoreTable.setGridColor(new Color(220, 220, 220));
        scoreTable.setSelectionBackground(SECONDARY_COLOR);
        setupButtonColumn(scoreTable, 6, "Cập nhật", new ModuleButtonEditor(new JCheckBox(), this), SECONDARY_COLOR);

        JScrollPane scrollScore = new JScrollPane(scoreTable);
        scrollScore.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollScore, BorderLayout.CENTER);

        ItemListener loadScoresListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                loadScores();
        };
        scoreClassComboBox.addItemListener(loadScoresListener);
        moduleComboBox.addItemListener(loadScoresListener);

        return panel;
    }

    private JPanel createGPAPanel() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBackground(PANEL_COLOR);
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JLabel title = new JLabel("Báo cáo GPA", SwingConstants.CENTER);
    title.setFont(new Font("Segoe UI", Font.BOLD, 18));
    title.setForeground(PRIMARY_COLOR);
    panel.add(title, BorderLayout.NORTH);

    JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
    filterPanel.setBackground(PANEL_COLOR);

    JLabel classLabel = new JLabel("Chọn lớp:");
    classLabel.setForeground(TEXT_COLOR);

    gpaClassComboBox = new JComboBox<>();
    gpaClassComboBox.setPreferredSize(new Dimension(150, 30));

    JButton loadGPA = createStyledButton("Tải GPA", SECONDARY_COLOR, Color.WHITE);
    JButton refreshGPA = createStyledButton("Làm mới", Color.GRAY, Color.WHITE);

    filterPanel.add(classLabel);
    filterPanel.add(gpaClassComboBox);
    filterPanel.add(loadGPA);
    filterPanel.add(refreshGPA);
    panel.add(filterPanel, BorderLayout.NORTH);  // Đặt ở NORTH sau title, nhưng có thể điều chỉnh nếu cần

    // Bảng GPA
    gpaTableModel = new DefaultTableModel(new Object[]{"MSSV", "Họ tên", "GPA"}, 0);  // Cột mặc định cho từng sinh viên
    gpaTable = new JTable(gpaTableModel);
    gpaTable.setRowHeight(40);
    gpaTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    gpaTable.setGridColor(new Color(220, 220, 220));
    gpaTable.setSelectionBackground(SECONDARY_COLOR);

    JScrollPane scrollGPA = new JScrollPane(gpaTable);
    scrollGPA.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
    panel.add(scrollGPA, BorderLayout.CENTER);

    // Thêm panel hiển thị GPA trung bình ở dưới
    JPanel averagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    averagePanel.setBackground(PANEL_COLOR);
    averagePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    JLabel averageLabel = new JLabel("GPA trung bình: ");
    averageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    averageLabel.setForeground(TEXT_COLOR);
    averagePanel.add(averageLabel);
    panel.add(averagePanel, BorderLayout.SOUTH);

    // Action listeners
    loadGPA.addActionListener(e -> loadGPAsForClass(averageLabel));
    refreshGPA.addActionListener(e -> {
        loadClasses();
        loadGPAsForClass(averageLabel);  // Làm mới dữ liệu sau khi load classes
    });

    return panel;
}
private void loadGPAsForClass(JLabel averageLabel) {
    String selectedClass = (String) gpaClassComboBox.getSelectedItem();
    if (selectedClass == null) return;

    gpaTableModel.setRowCount(0);  // Xóa dữ liệu cũ
    averageLabel.setText("GPA trung bình: ");  // Reset label

    try {
        if ("Tất cả".equals(selectedClass)) {
            // Chế độ hiển thị GPA trung bình theo lớp
            gpaTableModel.setColumnIdentifiers(new Object[]{"Lớp", "GPA Trung Bình"});  // Thay đổi cột

            // Lấy tất cả lớp duy nhất
            List<Student> allStudents = manager.getAllStudents();
            Set<String> classes = new TreeSet<>();
            for (Student s : allStudents) {
                classes.add(s.getClassName());
            }

            double totalSum = 0;
            int totalStudents = 0;

            // Tính GPA trung bình cho từng lớp và hiển thị
            for (String cls : classes) {
                Map<String, Double> gpas = manager.getGPAForClass(cls);
                if (!gpas.isEmpty()) {
                    double sum = 0;
                    for (Double gpa : gpas.values()) {
                        sum += gpa;
                    }
                    double average = sum / gpas.size();
                    gpaTableModel.addRow(new Object[]{cls, String.format("%.2f", average)});

                    // Tích lũy cho GPA trung bình tổng thể
                    totalSum += sum;
                    totalStudents += gpas.size();
                } else {
                    gpaTableModel.addRow(new Object[]{cls, "0.00 (Không có sinh viên)"});
                }
            }

            // Hiển thị GPA trung bình tổng thể ở dưới
            if (totalStudents > 0) {
                double overallAverage = totalSum / totalStudents;
                averageLabel.setText("GPA trung bình tổng thể (tất cả lớp): " + String.format("%.2f", overallAverage));
            } else {
                averageLabel.setText("GPA trung bình tổng thể: 0.00 (Không có sinh viên)");
            }

        } else {
            // Chế độ hiển thị GPA từng sinh viên theo lớp cụ thể
            gpaTableModel.setColumnIdentifiers(new Object[]{"MSSV", "Họ tên", "GPA"});  // Quay về cột mặc định

            Map<String, Double> gpas = manager.getGPAForClass(selectedClass);
            List<Student> studentsInClass = manager.getStudentsByClass(selectedClass);

            double sum = 0;
            for (Student s : studentsInClass) {
                double gpa = gpas.getOrDefault(s.getId(), 0.0);
                gpaTableModel.addRow(new Object[]{s.getId(), s.getName(), String.format("%.2f", gpa)});
                sum += gpa;
            }

            // Hiển thị GPA trung bình của lớp ở dưới
            if (!studentsInClass.isEmpty()) {
                double average = sum / studentsInClass.size();
                averageLabel.setText("GPA trung bình của lớp " + selectedClass + ": " + String.format("%.2f", average));
            } else {
                averageLabel.setText("GPA trung bình của lớp " + selectedClass + ": 0.00 (Không có sinh viên)");
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Lỗi khi tải GPA: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}

    private void loadClassesWithCounts() {
        try {
            List<Student> allStudents = manager.getAllStudents();
            Map<String, Integer> classCounts = new HashMap<>();
            for (Student s : allStudents) {
                String cls = s.getClassName();
                classCounts.put(cls, classCounts.getOrDefault(cls, 0) + 1);
            }
            DefaultTableModel model = (DefaultTableModel) ((JTable) ((JScrollPane) ((JPanel) studentCardPanel
                    .getComponent(0)).getComponent(1)).getViewport().getView()).getModel();
            model.setRowCount(0);
            for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
                model.addRow(new Object[] { entry.getKey(), entry.getValue(), "Xem danh sách" });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải danh sách lớp: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadStudentsForClass(String cls) {
        currentClass = cls;
        titleLabelStudent.setText("Danh sách sinh viên lớp " + cls);
        loadStudents(cls);
    }

    private void loadStudents(String cls) {
        try {
            List<Student> studentList = manager.getStudentsByClass(cls);
            studentTableModel.setRowCount(0);
            for (Student s : studentList) {
                studentTableModel.addRow(new Object[] { s.getId(), s.getName(), s.getClassName(), s.getYear(),
                        s.getEmail(), "Xem chi tiết", "Sửa", "Xóa" });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải danh sách sinh viên: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchStudents() {
        String keyword = searchField.getText().toLowerCase();
        try {
            List<Student> studentList = manager.getStudentsByClass(currentClass);
            studentTableModel.setRowCount(0);
            for (Student s : studentList) {
                if (s.getId().toLowerCase().contains(keyword) || s.getName().toLowerCase().contains(keyword)) {
                    studentTableModel.addRow(new Object[] { s.getId(), s.getName(), s.getClassName(), s.getYear(),
                            s.getEmail(), "Xem chi tiết", "Sửa", "Xóa" });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tìm kiếm: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadModules() {
        try {
            List<Module> moduleList = manager.getAllModules();
            moduleTableModel.setRowCount(0);
            moduleComboBox.removeAllItems();
            for (Module m : moduleList) {
                moduleTableModel.addRow(new Object[] { m.getCode(), m.getName(), m.getCredits(), "Sửa", "Xóa" });
                moduleComboBox.addItem(m.getCode() + " - " + m.getName());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải học phần: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadClasses() {
        try {
            List<Student> allStudents = manager.getAllStudents();
            Set<String> classes = new TreeSet<>();
            for (Student s : allStudents) {
                classes.add(s.getClassName());
            }
            scoreClassComboBox.removeAllItems();
            gpaClassComboBox.removeAllItems();
            scoreClassComboBox.addItem("Tất cả");
            gpaClassComboBox.addItem("Tất cả");
            for (String cls : classes) {
                scoreClassComboBox.addItem(cls);
                gpaClassComboBox.addItem(cls);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải danh sách lớp: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadScores() {
        String selectedClass = (String) scoreClassComboBox.getSelectedItem();
        String selectedModule = (String) moduleComboBox.getSelectedItem();
        if (selectedClass == null || selectedModule == null)
            return;
        String moduleCode = selectedModule.split(" - ")[0];
        try {
            List<Student> studentList = "Tất cả".equals(selectedClass)
                    ? manager.getStudentsWithScoresForModule(moduleCode)
                    : manager.getStudentsWithScoresForModuleByClass(moduleCode, selectedClass);
            scoreTableModel.setRowCount(0);
            for (Student s : studentList) {
                Student.SubjectScores scores = s.getScoresForModule(moduleCode);
                scoreTableModel.addRow(new Object[] { s.getId(), s.getName(), s.getClassName(), scores.getAttendance(),
                        scores.getTest1(), scores.getExam(), "Cập nhật" });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải điểm: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGPAs() {
        String selectedClass = (String) gpaClassComboBox.getSelectedItem();
        if (selectedClass == null)
            return;
        try {
            Map<String, Double> gpas;
            if ("Tất cả".equals(selectedClass)) {
                gpas = new HashMap<>();
                List<Student> allStudents = manager.getAllStudents();
                for (Student s : allStudents) {
                    gpas.put(s.getId(), manager.getGPAForStudent(s.getId()));
                }
            } else {
                gpas = manager.getGPAForClass(selectedClass);
            }
            gpaTableModel.setRowCount(0);
            for (Map.Entry<String, Double> entry : gpas.entrySet()) {
                Student s = getStudentById(entry.getKey());
                if (s != null) {
                    gpaTableModel.addRow(new Object[] { s.getId(), s.getName(), s.getClassName(),
                            String.format("%.2f", entry.getValue()) });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải GPA: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Student getStudentById(String id) {
        try {
            return manager.getStudentById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private void showAddClassDialog() {
        JDialog dialog = new JDialog(this, "Thêm Lớp", true);
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Sửa: Cho phép field mở rộng
        gbc.weightx = 1.0; // Sửa: Trọng số để field chiếm hết không gian

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0; // Label không cần weightx
        formPanel.add(new JLabel("Tên lớp:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // Field cần weightx
        JTextField classField = createStyledTextField();
        formPanel.add(classField, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = createStyledButton("Lưu và Thêm Sinh viên", SECONDARY_COLOR, Color.WHITE);
        JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            String cls = classField.getText().trim();
            if (cls.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên lớp không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                if (!manager.getStudentsByClass(cls).isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Lớp đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
            }
            dialog.dispose();
            showAddDialog(cls);
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showAddDialog(String prefillClass) {
        JDialog dialog = new JDialog(this, "Thêm Sinh viên", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Sửa: Cho phép field mở rộng
        gbc.weightx = 1.0; // Sửa: Trọng số để field chiếm hết không gian

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0; // Label không cần weightx
        formPanel.add(new JLabel("Mã SV:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // Field cần weightx
        JTextField idField = createStyledTextField();
        formPanel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Họ và tên:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField nameField = createStyledTextField();
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Năm sinh:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField yearField = createStyledTextField();
        formPanel.add(yearField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField emailField = createStyledTextField();
        formPanel.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Lớp:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<String> classCombo = new JComboBox<>();
        classCombo.setEditable(true);
        classCombo.setPreferredSize(new Dimension(200, 25));
        try {
            List<Student> allStudents = manager.getAllStudents();
            Set<String> classes = new TreeSet<>();
            for (Student s : allStudents)
                classes.add(s.getClassName());
            for (String cls : classes)
                classCombo.addItem(cls);
        } catch (Exception e) {
        }
        if (prefillClass != null)
            classCombo.setSelectedItem(prefillClass);
        formPanel.add(classCombo, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = createStyledButton("Lưu", SECONDARY_COLOR, Color.WHITE);
        JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            try {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                int year = Integer.parseInt(yearField.getText().trim());
                String email = emailField.getText().trim();
                String cls = ((String) classCombo.getSelectedItem()).trim();
                if (cls.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Lớp không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Student s = new Student(id, name, year, email, cls);
                if (manager.addStudent(s)) {
                    JOptionPane.showMessageDialog(this, "Thêm thành công!");
                    loadClassesWithCounts();
                    loadClasses();
                    if (currentClass != null && currentClass.equals(cls))
                        loadStudents(currentClass);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Mã SV đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Năm sinh phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi thêm: " + ex.getMessage(), "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    public void showEditStudentDialog(String id) {
        try {
            Student s = manager.getStudentById(id);
            if (s == null)
                return;

            JDialog dialog = new JDialog(this, "Sửa Sinh viên", true);
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL; // Sửa: Cho phép field mở rộng
            gbc.weightx = 1.0; // Sửa: Trọng số để field chiếm hết không gian

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Mã SV:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JTextField idField = createStyledTextField();
            idField.setText(s.getId());
            idField.setEditable(false);
            formPanel.add(idField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Họ và tên:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JTextField nameField = createStyledTextField();
            nameField.setText(s.getName());
            formPanel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Năm sinh:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JTextField yearField = createStyledTextField();
            yearField.setText(String.valueOf(s.getYear()));
            formPanel.add(yearField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JTextField emailField = createStyledTextField();
            emailField.setText(s.getEmail());
            formPanel.add(emailField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Lớp:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JComboBox<String> classCombo = new JComboBox<>();
            classCombo.setEditable(true);
            classCombo.setPreferredSize(new Dimension(200, 25));
            try {
                List<Student> allStudents = manager.getAllStudents();
                Set<String> classes = new TreeSet<>();
                for (Student st : allStudents)
                    classes.add(st.getClassName());
                for (String cls : classes)
                    classCombo.addItem(cls);
            } catch (Exception e) {
            }
            classCombo.setSelectedItem(s.getClassName());
            formPanel.add(classCombo, gbc);

            dialog.add(formPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveButton = createStyledButton("Lưu", SECONDARY_COLOR, Color.WHITE);
            JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            saveButton.addActionListener(e -> {
                try {
                    String newName = nameField.getText().trim();
                    int newYear = Integer.parseInt(yearField.getText().trim());
                    String newEmail = emailField.getText().trim();
                    String newCls = ((String) classCombo.getSelectedItem()).trim();
                    if (newCls.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Lớp không được để trống!", "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    s.setName(newName);
                    s.setYear(newYear);
                    s.setEmail(newEmail);
                    s.setClassName(newCls);
                    if (manager.updateStudent(s)) {
                        JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
                        loadClassesWithCounts();
                        loadClasses();
                        loadStudents(currentClass);
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Năm sinh phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật: " + ex.getMessage(), "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            cancelButton.addActionListener(e -> dialog.dispose());
            dialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteStudent(String id, String name) {
        int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận xóa sinh viên " + name + "?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (manager.deleteStudent(id)) {
                    JOptionPane.showMessageDialog(this, "Xóa thành công!");
                    loadClassesWithCounts();
                    loadClasses();
                    loadStudents(currentClass);
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void showDetailDialog(String id) {
        try {
            Student s = manager.getStudentById(id);
            if (s == null)
                return;

            JDialog dialog = new JDialog(this, "Chi tiết Sinh viên", true);
            dialog.setSize(500, 400);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            JPanel infoPanel = new JPanel(new GridBagLayout());
            infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin cơ bản"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            infoPanel.add(new JLabel("Mã SV:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(s.getId()), gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            infoPanel.add(new JLabel("Họ và tên:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(s.getName()), gbc);
            gbc.gridx = 0;
            gbc.gridy = 2;
            infoPanel.add(new JLabel("Năm sinh:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(String.valueOf(s.getYear())), gbc);
            gbc.gridx = 0;
            gbc.gridy = 3;
            infoPanel.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(s.getEmail()), gbc);
            gbc.gridx = 0;
            gbc.gridy = 4;
            infoPanel.add(new JLabel("Lớp:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(s.getClassName()), gbc);

            dialog.add(infoPanel, BorderLayout.NORTH);

            DefaultTableModel detailScoreModel = new DefaultTableModel(
                    new Object[] { "Học phần", "Chuyên cần", "Kiểm tra 1", "Thi", "Điểm môn" }, 0);
            JTable detailScoreTable = new JTable(detailScoreModel);
            detailScoreTable.setRowHeight(25);

            List<Module> modules = manager.getAllModules();
            for (Module m : modules) {
                Student.SubjectScores scores = s.getScoresForModule(m.getCode());
                double moduleScore = s.getModuleScore(m.getCode(), m.getCredits());
                detailScoreModel.addRow(new Object[] { m.getName(), scores.getAttendance(), scores.getTest1(),
                        scores.getExam(), String.format("%.2f", moduleScore) });
            }

            JScrollPane scrollDetail = new JScrollPane(detailScoreTable);
            scrollDetail.setBorder(BorderFactory.createTitledBorder("Điểm các học phần"));
            dialog.add(scrollDetail, BorderLayout.CENTER);

            JPanel gpaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JLabel gpaLabel = new JLabel("GPA: " + String.format("%.2f", manager.getGPAForStudent(id)));
            gpaLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            gpaPanel.add(gpaLabel);
            dialog.add(gpaPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải chi tiết: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddModuleDialog() {
        JDialog dialog = new JDialog(this, "Thêm Học Phần Mới", true);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(PANEL_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Nhập thông tin học phần");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(title, gbc);

        JLabel codeLabel = new JLabel("Mã học phần:");
        codeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        dialog.add(codeLabel, gbc);
        JTextField codeField = new JTextField(20);
        codeField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1;
        dialog.add(codeField, gbc);

        JLabel nameLabel = new JLabel("Tên học phần:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 2;
        dialog.add(nameLabel, gbc);
        JTextField nameField = new JTextField(20);
        nameField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1;
        dialog.add(nameField, gbc);

        JLabel creditsLabel = new JLabel("Số tín chỉ:");
        creditsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 3;
        dialog.add(creditsLabel, gbc);
        JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        creditsSpinner.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gbc.gridx = 1;
        dialog.add(creditsSpinner, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(PANEL_COLOR);
        JButton addButton = createStyledButton("Thêm", SECONDARY_COLOR, Color.WHITE);
        JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        addButton.addActionListener(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            int credits = (Integer) creditsSpinner.getValue();
            if (code.isEmpty() || name.isEmpty() || credits < 1) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng điền đầy đủ và hợp lệ thông tin.", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            Module m = new Module(code, name, credits);
            try {
                if (manager.addModule(m)) {
                    JOptionPane.showMessageDialog(dialog, "Thêm học phần thành công!", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadModules();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Mã học phần đã tồn tại hoặc lỗi.", "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    public void showEditModuleDialog(String code) {
        try {
            Module m = manager.getModuleByCode(code);
            if (m == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy học phần.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JDialog dialog = new JDialog(this, "Sửa Thông Tin Học Phần", true);
            dialog.setLayout(new GridBagLayout());
            dialog.getContentPane().setBackground(PANEL_COLOR);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 15, 10, 15);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("Chỉnh sửa học phần: " + m.getCode());
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            title.setForeground(PRIMARY_COLOR);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            dialog.add(title, gbc);

            JLabel codeLabel = new JLabel("Mã học phần:");
            codeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            dialog.add(codeLabel, gbc);
            JTextField codeField = new JTextField(m.getCode(), 20);
            codeField.setEditable(false);
            codeField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            gbc.gridx = 1;
            dialog.add(codeField, gbc);

            JLabel nameLabel = new JLabel("Tên học phần:");
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            gbc.gridx = 0;
            gbc.gridy = 2;
            dialog.add(nameLabel, gbc);
            JTextField nameField = new JTextField(m.getName(), 20);
            nameField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            gbc.gridx = 1;
            dialog.add(nameField, gbc);

            JLabel creditsLabel = new JLabel("Số tín chỉ:");
            creditsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            gbc.gridx = 0;
            gbc.gridy = 3;
            dialog.add(creditsLabel, gbc);
            JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(m.getCredits(), 1, 10, 1));
            creditsSpinner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_COLOR, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            gbc.gridx = 1;
            dialog.add(creditsSpinner, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            buttonPanel.setBackground(PANEL_COLOR);
            JButton saveButton = createStyledButton("Lưu", SECONDARY_COLOR, Color.WHITE);
            JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            dialog.add(buttonPanel, gbc);

            saveButton.addActionListener(e -> {
                String newName = nameField.getText().trim();
                int newCredits = (Integer) creditsSpinner.getValue();
                if (newName.isEmpty() || newCredits < 1) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng điền đầy đủ và hợp lệ thông tin.", "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                m.setName(newName);
                m.setCredits(newCredits);
                try {
                    if (manager.updateModule(m)) {
                        JOptionPane.showMessageDialog(dialog, "Cập nhật học phần thành công!", "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadModules();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Lỗi cập nhật học phần.", "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
            cancelButton.addActionListener(e -> dialog.dispose());

            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);
            dialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteModule(String code, String name) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận xóa học phần " + name + "?\nĐiều này sẽ xóa điểm liên quan của tất cả sinh viên.", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (manager.deleteModule(code)) {
                    JOptionPane.showMessageDialog(this, "Xóa thành công!");
                    loadModules();
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void showEditScoreDialog(String id) {
        try {
            Student s = manager.getStudentById(id);
            if (s == null)
                return;
            String selectedModule = (String) moduleComboBox.getSelectedItem();
            if (selectedModule == null)
                return;
            String moduleCode = selectedModule.split(" - ")[0];
            Student.SubjectScores scores = s.getScoresForModule(moduleCode);

            JDialog dialog = new JDialog(this, "Cập nhật Điểm - " + s.getName(), true);
            dialog.setSize(400, 250);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL; // Sửa: Cho phép field mở rộng
            gbc.weightx = 1.0; // Sửa: Trọng số để field chiếm hết không gian

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Chuyên cần:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JTextField attField = createStyledTextField();
            attField.setText(String.valueOf(scores.getAttendance()));
            formPanel.add(attField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Kiểm tra 1:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JTextField test1Field = createStyledTextField();
            test1Field.setText(String.valueOf(scores.getTest1()));
            formPanel.add(test1Field, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Thi:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            JTextField examField = createStyledTextField();
            examField.setText(String.valueOf(scores.getExam()));
            formPanel.add(examField, gbc);

            dialog.add(formPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveButton = createStyledButton("Lưu", SECONDARY_COLOR, Color.WHITE);
            JButton cancelButton = createStyledButton("Hủy", Color.GRAY, Color.WHITE);
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            saveButton.addActionListener(e -> {
                try {
                    double att = Double.parseDouble(attField.getText().trim());
                    double test1 = Double.parseDouble(test1Field.getText().trim());
                    double exam = Double.parseDouble(examField.getText().trim());
                    Student.SubjectScores newScores = new Student.SubjectScores(att, test1, exam);
                    Map<String, Student.SubjectScores> updates = new HashMap<>();
                    updates.put(id, newScores);
                    if (manager.updateScoresForModule(moduleCode, updates)) {
                        JOptionPane.showMessageDialog(this, "Cập nhật điểm thành công!");
                        loadScores();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Điểm phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật: " + ex.getMessage(), "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            cancelButton.addActionListener(e -> dialog.dispose());
            dialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField(20);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return field;
    }

    private void setupButtonColumn(JTable table, int columnIndex, String buttonText, DefaultCellEditor editor,
            Color bgColor) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setCellRenderer(new StyledButtonRenderer(buttonText, bgColor));
        column.setCellEditor(editor);
        column.setPreferredWidth(100);
    }

    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14)); // Larger font
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20)); // More padding for easier clicks
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}

class StyledButtonRenderer extends JButton implements TableCellRenderer {
    public StyledButtonRenderer(String text, Color bgColor) {
        setText(text);
        setBackground(bgColor);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setFocusPainted(false);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        setText(value != null ? value.toString() : "Cập nhật");
        return this;
    }
}

abstract class AbstractButtonEditor extends DefaultCellEditor {
    protected JButton button;
    protected boolean clicked;
    protected ClientGUI parent;
    protected String label;

    public AbstractButtonEditor(JCheckBox checkBox, ClientGUI parent, String buttonText, Color bgColor) {
        super(checkBox);
        this.parent = parent;
        button = new JButton(buttonText);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        label = (value == null) ? "" : value.toString();
        clicked = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (clicked)
            performAction();
        clicked = false;
        return label;
    }

    abstract void performAction();
}

class ViewClassButtonEditor extends AbstractButtonEditor {
    private String cls;

    public ViewClassButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox, parent, "Xem danh sách", ClientGUI.PRIMARY_COLOR);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        cls = (String) table.getValueAt(row, 0);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    void performAction() {
        parent.loadStudentsForClass(cls);
        ((CardLayout) parent.studentCardPanel.getLayout()).show(parent.studentCardPanel, "students");
    }
}

class ViewButtonEditor extends AbstractButtonEditor {
    private String id;

    public ViewButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox, parent, "Xem chi tiết", ClientGUI.PRIMARY_COLOR);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    void performAction() {
        parent.showDetailDialog(id);
    }
}

class EditButtonEditor extends AbstractButtonEditor {
    private String id;

    public EditButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox, parent, "Sửa", ClientGUI.SECONDARY_COLOR);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    void performAction() {
        parent.showEditStudentDialog(id);
    }
}

class DeleteButtonEditor extends AbstractButtonEditor {
    private String id, name;

    public DeleteButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox, parent, "Xóa", ClientGUI.ACCENT_COLOR);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        name = (String) table.getValueAt(row, 1);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    void performAction() {
        parent.deleteStudent(id, name);
    }
}

class ModuleButtonEditor extends AbstractButtonEditor {
    private String id;

    public ModuleButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox, parent, "Cập nhật", ClientGUI.SECONDARY_COLOR);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        id = (String) table.getValueAt(row, 0);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    void performAction() {
        parent.showEditScoreDialog(id);
    }
}

class EditModuleButtonEditor extends AbstractButtonEditor {
    private String code;

    public EditModuleButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox, parent, "Sửa", ClientGUI.SECONDARY_COLOR);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        code = (String) table.getValueAt(row, 0);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    void performAction() {
        parent.showEditModuleDialog(code);
    }
}

class DeleteModuleButtonEditor extends AbstractButtonEditor {
    private String code, name;

    public DeleteModuleButtonEditor(JCheckBox checkBox, ClientGUI parent) {
        super(checkBox, parent, "Xóa", ClientGUI.ACCENT_COLOR);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        code = (String) table.getValueAt(row, 0);
        name = (String) table.getValueAt(row, 1);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    void performAction() {
        parent.deleteModule(code, name);
    }
}