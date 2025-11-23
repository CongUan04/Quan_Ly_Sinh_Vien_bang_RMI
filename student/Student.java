package rmi.student;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Student implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private int year;
    private String email;
    private String className;
    private Map<String, SubjectScores> subjectScores;  // Key: Mã học phần (module code), Value: Điểm cho học phần đó

    // Inner class cho điểm học phần
    public static class SubjectScores implements Serializable {
        private static final long serialVersionUID = 1L;
        private double attendance;  // Chuyên cần
        private double test1;       // Kiểm tra 1
        private double exam;        // Điểm thi

        public SubjectScores() {
            this.attendance = 0.0;
            this.test1 = 0.0;
            this.exam = 0.0;
        }

        public SubjectScores(double attendance, double test1, double exam) {
            this.attendance = attendance;
            this.test1 = test1;
            this.exam = exam;
        }

        // Getters & Setters
        public double getAttendance() { return attendance; }
        public void setAttendance(double attendance) { this.attendance = attendance; }
        public double getTest1() { return test1; }
        public void setTest1(double test1) { this.test1 = test1; }
        public double getExam() { return exam; }
        public void setExam(double exam) { this.exam = exam; }

        @Override
        public String toString() {
            return attendance + "," + test1 + "," + exam;
        }

        public static SubjectScores fromString(String str) {
            String[] parts = str.split(",");
            if (parts.length == 3) {
                try {
                    return new SubjectScores(
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2])
                    );
                } catch (NumberFormatException e) {
                    return new SubjectScores();
                }
            }
            return new SubjectScores();
        }
    }

    public Student() {
        this.subjectScores = new HashMap<>();
    }

    public Student(String id, String name, int year, String email, String className) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.email = email;
        this.className = className;
        this.subjectScores = new HashMap<>();
    }

    public Student(String id, String name, int year, String email, String className, Map<String, SubjectScores> subjectScores) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.email = email;
        this.className = className;
        this.subjectScores = subjectScores != null ? new HashMap<>(subjectScores) : new HashMap<>();
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Map<String, SubjectScores> getSubjectScores() { return subjectScores; }
    public void setSubjectScores(Map<String, SubjectScores> subjectScores) { this.subjectScores = subjectScores; }

    // Helper: Lấy điểm cho học phần cụ thể
    public SubjectScores getScoresForModule(String moduleCode) {
        return subjectScores.getOrDefault(moduleCode, new SubjectScores());
    }

    // Helper: Cập nhật điểm cho học phần
    public void updateScoresForModule(String moduleCode, SubjectScores scores) {
        subjectScores.put(moduleCode, scores);
    }

    // Tính điểm môn dựa trên số tín chỉ
    public double getModuleScore(String moduleCode, int credits) {
        SubjectScores scores = getScoresForModule(moduleCode);
        double attWeight, test1Weight, examWeight;
        if (credits == 1) {
            attWeight = 0.2;
            test1Weight = 0.0;
            examWeight = 0.8;
        } else if (credits == 2) {
            attWeight = 0.15;
            test1Weight = 0.15;
            examWeight = 0.7;
        } else { // 3, 4 hoặc hơn
            attWeight = 0.1;
            test1Weight = 0.3;
            examWeight = 0.6;
        }
        return attWeight * scores.getAttendance() + test1Weight * scores.getTest1() + examWeight * scores.getExam();
    }

    // Tính GPA (điểm trung bình tích lũy, trọng số theo tín chỉ)
    public double getGPA(Map<String, Integer> moduleCredits) {
        double totalPoints = 0;
        int totalCredits = 0;
        for (Map.Entry<String, SubjectScores> entry : subjectScores.entrySet()) {
            String code = entry.getKey();
            if (moduleCredits.containsKey(code)) {
                int cred = moduleCredits.get(code);
                totalPoints += getModuleScore(code, cred) * cred;
                totalCredits += cred;
            }
        }
        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }

    @Override
    public String toString() {
        return id + " - " + name + " - " + year + " - " + email + " - Class: " + className + " - Scores: " + subjectScores;
    }
}