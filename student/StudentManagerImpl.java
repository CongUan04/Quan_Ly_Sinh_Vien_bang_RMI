package rmi.student;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;

public class StudentManagerImpl extends UnicastRemoteObject implements StudentManager {
    private static final long serialVersionUID = 1L;
    private Map<String, Student> students;
    private final File storageFile = new File("students.csv");
    private final List<String> defaultModules = Arrays.asList("Lập Trình Mạng", "Kỹ Năng Mềm");

    protected StudentManagerImpl() throws RemoteException {
        super();
        students = new HashMap<>();
        loadFromFile();
    }

    // Load từ CSV (cập nhật để hỗ trợ map học phần, backward compat với file cũ)
    private synchronized void loadFromFile() {
        students.clear();
        if (!storageFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(storageFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("ID,")) continue;

                String[] parts = line.split(",");
                String id = parts[0].trim();
                String name = parts[1].trim();
                int year = Integer.parseInt(parts[2].trim());
                String email = parts[3].trim();
                String className = parts[4].trim();
                
                Map<String, Student.SubjectScores> scoresMap = new HashMap<>();
                // Backward compat: Nếu file cũ có 8 fields (có math,lit,eng), migrate sang map với điểm 0 cho học phần mới
                if (parts.length >= 8) {
                    // Bỏ qua điểm cũ, set 0 cho default modules
                    for (String mod : defaultModules) {
                        scoresMap.put(mod, new Student.SubjectScores());
                    }
                } else if (parts.length > 5 && parts[5].startsWith("Subjects:")) {
                    // Format mới
                    String subjectsStr = parts[5].substring(9);
                    String[] modulePairs = subjectsStr.split("\\|");
                    for (String pair : modulePairs) {
                        if (pair.isEmpty()) continue;
                        String[] modParts = pair.split(":");
                        if (modParts.length == 2) {
                            String moduleName = modParts[0].trim();
                            Student.SubjectScores scores = Student.SubjectScores.fromString(modParts[1].trim());
                            scoresMap.put(moduleName, scores);
                        }
                    }
                }
                // Đảm bảo default modules
                for (String mod : defaultModules) {
                    if (!scoresMap.containsKey(mod)) {
                        scoresMap.put(mod, new Student.SubjectScores());
                    }
                }
                
                Student s = new Student(id, name, year, email, className, scoresMap);
                students.put(id, s);
            }
        } catch (Exception e) {
            System.err.println("Load CSV error: " + e.getMessage());
        }
    }

    // Lưu xuống CSV (cập nhật format)
    private synchronized void saveToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(storageFile))) {
            // Ghi header mới
            bw.write("ID,Name,Year,Email,Class,Subjects");
            bw.newLine();
            for (Student s : students.values()) {
                StringBuilder subjects = new StringBuilder("Subjects:");
                boolean first = true;
                for (Map.Entry<String, Student.SubjectScores> entry : s.getSubjectScores().entrySet()) {
                    if (!first) subjects.append("|");
                    subjects.append(entry.getKey()).append(":").append(entry.getValue().toString());
                    first = false;
                }
                bw.write(s.getId() + "," + s.getName() + "," + s.getYear() + "," + s.getEmail() + "," 
                        + s.getClassName() + "," + subjects.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Save CSV error: " + e.getMessage());
        }
    }

    @Override
    public synchronized boolean addStudent(Student s) throws RemoteException {
        if (s == null || s.getId() == null) return false;
        if (students.containsKey(s.getId())) return false;
        students.put(s.getId(), s);
        saveToFile();
        return true;
    }

    @Override
    public synchronized boolean updateStudent(Student s) throws RemoteException {
        if (s == null || s.getId() == null) return false;
        if (!students.containsKey(s.getId())) return false;
        students.put(s.getId(), s);
        saveToFile();
        return true;
    }

    @Override
    public synchronized boolean deleteStudent(String id) throws RemoteException {
        if (id == null) return false;
        if (students.remove(id) != null) {
            saveToFile();
            return true;
        }
        return false;
    }

    @Override
    public synchronized Student getStudentById(String id) throws RemoteException {
        Student s = students.get(id);
        if (s != null) {
            // Trả về copy để tránh modify trực tiếp
            return new Student(s.getId(), s.getName(), s.getYear(), s.getEmail(), s.getClassName(), s.getSubjectScores());
        }
        return null;
    }

    @Override
    public synchronized List<Student> getAllStudents() throws RemoteException {
        List<Student> result = new ArrayList<>();
        for (Student s : students.values()) {
            result.add(new Student(s.getId(), s.getName(), s.getYear(), s.getEmail(), s.getClassName(), s.getSubjectScores()));
        }
        return result;
    }

    // Methods mới cho học phần
    @Override
    public synchronized List<String> getAllModules() throws RemoteException {
        return new ArrayList<>(defaultModules);
    }

    @Override
    public synchronized List<Student> getStudentsWithScoresForModule(String moduleName) throws RemoteException {
        List<Student> result = new ArrayList<>();
        for (Student s : students.values()) {
            Map<String, Student.SubjectScores> copyScores = new HashMap<>(s.getSubjectScores());
            Student copy = new Student(s.getId(), s.getName(), s.getYear(), s.getEmail(), s.getClassName(), copyScores);
            result.add(copy);
        }
        return result;
    }

    @Override
    public synchronized boolean updateScoresForModule(String moduleName, Map<String, Student.SubjectScores> updates) throws RemoteException {
        if (moduleName == null || updates == null) return false;
        boolean success = true;
        for (Map.Entry<String, Student.SubjectScores> entry : updates.entrySet()) {
            Student s = students.get(entry.getKey());
            if (s != null) {
                s.updateScoresForModule(moduleName, entry.getValue());
                success &= updateStudent(s);
            } else {
                success = false;
            }
        }
        return success;
    }
}