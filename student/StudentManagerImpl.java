package rmi.student;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

public class StudentManagerImpl extends UnicastRemoteObject implements StudentManager {
    private static final long serialVersionUID = 1L;
    private Map<String, Student> students;
    private Map<String, Module> modules;
    private MongoCollection<Document> studentCollection;
    private MongoCollection<Document> moduleCollection;

    protected StudentManagerImpl() throws RemoteException {
        super();
        students = new HashMap<>();
        modules = new HashMap<>();
        // Kết nối đến MongoDB
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("studentdb");
        studentCollection = database.getCollection("students");
        moduleCollection = database.getCollection("modules");
        loadStudentsFromDB();
        loadModulesFromDB();
    }

    // Load sinh viên từ MongoDB
    private synchronized void loadStudentsFromDB() {
        students.clear();
        FindIterable<Document> docs = studentCollection.find();
        for (Document doc : docs) {
            Object idObj = doc.get("_id");
            if (idObj == null || !(idObj instanceof String)) continue;
            String id = (String) idObj;

            String name = doc.getString("name");
            Integer yearObj = doc.getInteger("year");
            int year = (yearObj != null) ? yearObj : 0;
            String email = doc.getString("email");
            String className = doc.getString("className");

            Map<String, Student.SubjectScores> scoresMap = new HashMap<>();
            Document scoresDoc = (Document) doc.get("subjectScores");
            if (scoresDoc != null) {
                for (String moduleCode : scoresDoc.keySet()) {
                    Document score = (Document) scoresDoc.get(moduleCode);
                    if (score != null) {
                        Double att = score.getDouble("attendance");
                        Double t1 = score.getDouble("test1");
                        Double exam = score.getDouble("exam");
                        scoresMap.put(moduleCode, new Student.SubjectScores(
                            att != null ? att : 0.0,
                            t1 != null ? t1 : 0.0,
                            exam != null ? exam : 0.0
                        ));
                    }
                }
            }
            // Đảm bảo có scores cho tất cả modules hiện có
            for (String modCode : modules.keySet()) {
                if (!scoresMap.containsKey(modCode)) {
                    scoresMap.put(modCode, new Student.SubjectScores());
                }
            }

            Student s = new Student(id, name, year, email, className, scoresMap);
            students.put(id, s);
        }
    }

    // Load học phần từ MongoDB
    private synchronized void loadModulesFromDB() {
        modules.clear();
        FindIterable<Document> docs = moduleCollection.find();
        for (Document doc : docs) {
            Object idObj = doc.get("_id");
            if (idObj == null || !(idObj instanceof String)) continue;
            String code = (String) idObj;

            String name = doc.getString("name");
            Integer creditsObj = doc.getInteger("credits");
            int credits = (creditsObj != null) ? creditsObj : 0;

            Module m = new Module(code, name, credits);
            modules.put(code, m);
        }
        // Nếu không có modules, thêm mặc định
        if (modules.isEmpty()) {
            addDefaultModules();
        }
    }

    private void addDefaultModules() {
        try {
            addModule(new Module("LTM", "Lập Trình Mạng", 3));
            addModule(new Module("KNM", "Kỹ Năng Mềm", 2));
        } catch (Exception e) {
            // Ignore
        }
    }

    // Helper: Tạo Document từ Student
    private Document createDocFromStudent(Student s) {
        Document doc = new Document("_id", s.getId())
            .append("name", s.getName())
            .append("year", s.getYear())
            .append("email", s.getEmail())
            .append("className", s.getClassName());

        Document scoresDoc = new Document();
        for (Map.Entry<String, Student.SubjectScores> entry : s.getSubjectScores().entrySet()) {
            Student.SubjectScores sc = entry.getValue();
            Document scDoc = new Document("attendance", sc.getAttendance())
                .append("test1", sc.getTest1())
                .append("exam", sc.getExam());
            scoresDoc.append(entry.getKey(), scDoc);
        }
        doc.append("subjectScores", scoresDoc);
        return doc;
    }

    // Helper: Tạo Document từ Module
    private Document createDocFromModule(Module m) {
        return new Document("_id", m.getCode())
            .append("name", m.getName())
            .append("credits", m.getCredits());
    }

    // Cập nhật tất cả sinh viên khi thêm/xóa module
    private synchronized void updateAllStudentsForNewModule(String moduleCode) {
        for (Student s : students.values()) {
            s.updateScoresForModule(moduleCode, new Student.SubjectScores());
            try {
                updateStudent(s);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private synchronized void removeModuleFromAllStudents(String moduleCode) {
        for (Student s : students.values()) {
            s.getSubjectScores().remove(moduleCode);
            try {
                updateStudent(s);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Override
    public synchronized boolean addStudent(Student s) throws RemoteException {
        if (s == null || s.getId() == null) return false;
        if (students.containsKey(s.getId())) return false;
        // Khởi tạo scores cho tất cả modules
        for (String modCode : modules.keySet()) {
            if (!s.getSubjectScores().containsKey(modCode)) {
                s.updateScoresForModule(modCode, new Student.SubjectScores());
            }
        }
        students.put(s.getId(), s);
        Document doc = createDocFromStudent(s);
        studentCollection.insertOne(doc);
        return true;
    }

    @Override
    public synchronized boolean updateStudent(Student s) throws RemoteException {
        if (s == null || s.getId() == null) return false;
        if (!students.containsKey(s.getId())) return false;
        students.put(s.getId(), s);
        Document doc = createDocFromStudent(s);
        studentCollection.replaceOne(eq("_id", s.getId()), doc);
        return true;
    }

    @Override
    public synchronized boolean deleteStudent(String id) throws RemoteException {
        if (id == null) return false;
        if (students.remove(id) != null) {
            studentCollection.deleteOne(eq("_id", id));
            return true;
        }
        return false;
    }

    @Override
    public synchronized Student getStudentById(String id) throws RemoteException {
        Student s = students.get(id);
        if (s != null) {
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

    @Override
    public synchronized List<Student> getStudentsByClass(String className) throws RemoteException {
        List<Student> result = new ArrayList<>();
        for (Student s : students.values()) {
            if (s.getClassName().equalsIgnoreCase(className)) {
                result.add(new Student(s.getId(), s.getName(), s.getYear(), s.getEmail(), s.getClassName(), s.getSubjectScores()));
            }
        }
        return result;
    }

    // Methods cho học phần
    @Override
    public synchronized boolean addModule(Module m) throws RemoteException {
        if (m == null || m.getCode() == null) return false;
        if (modules.containsKey(m.getCode())) return false;
        modules.put(m.getCode(), m);
        Document doc = createDocFromModule(m);
        moduleCollection.insertOne(doc);
        // Cập nhật tất cả sinh viên
        updateAllStudentsForNewModule(m.getCode());
        return true;
    }

    @Override
    public synchronized boolean updateModule(Module m) throws RemoteException {
        if (m == null || m.getCode() == null) return false;
        if (!modules.containsKey(m.getCode())) return false;
        modules.put(m.getCode(), m);
        Document doc = createDocFromModule(m);
        moduleCollection.replaceOne(eq("_id", m.getCode()), doc);
        return true;
    }

    @Override
    public synchronized boolean deleteModule(String code) throws RemoteException {
        if (code == null) return false;
        if (modules.remove(code) != null) {
            moduleCollection.deleteOne(eq("_id", code));
            // Xóa khỏi tất cả sinh viên
            removeModuleFromAllStudents(code);
            return true;
        }
        return false;
    }

    @Override
    public synchronized Module getModuleByCode(String code) throws RemoteException {
        Module m = modules.get(code);
        if (m != null) {
            return new Module(m.getCode(), m.getName(), m.getCredits());
        }
        return null;
    }

    @Override
    public synchronized List<Module> getAllModules() throws RemoteException {
        List<Module> result = new ArrayList<>();
        for (Module m : modules.values()) {
            result.add(new Module(m.getCode(), m.getName(), m.getCredits()));
        }
        return result;
    }

    @Override
    public synchronized List<Student> getStudentsWithScoresForModule(String moduleCode) throws RemoteException {
        if (!modules.containsKey(moduleCode)) return new ArrayList<>();
        List<Student> result = new ArrayList<>();
        for (Student s : students.values()) {
            Map<String, Student.SubjectScores> copyScores = new HashMap<>(s.getSubjectScores());
            Student copy = new Student(s.getId(), s.getName(), s.getYear(), s.getEmail(), s.getClassName(), copyScores);
            result.add(copy);
        }
        return result;
    }

    @Override
    public synchronized List<Student> getStudentsWithScoresForModuleByClass(String moduleCode, String className) throws RemoteException {
        if (!modules.containsKey(moduleCode)) return new ArrayList<>();
        List<Student> result = new ArrayList<>();
        for (Student s : students.values()) {
            if (s.getClassName().equalsIgnoreCase(className)) {
                Map<String, Student.SubjectScores> copyScores = new HashMap<>(s.getSubjectScores());
                Student copy = new Student(s.getId(), s.getName(), s.getYear(), s.getEmail(), s.getClassName(), copyScores);
                result.add(copy);
            }
        }
        return result;
    }

    @Override
    public synchronized boolean updateScoresForModule(String moduleCode, Map<String, Student.SubjectScores> updates) throws RemoteException {
        if (moduleCode == null || updates == null || !modules.containsKey(moduleCode)) return false;
        boolean success = true;
        for (Map.Entry<String, Student.SubjectScores> entry : updates.entrySet()) {
            Student s = students.get(entry.getKey());
            if (s != null) {
                s.updateScoresForModule(moduleCode, entry.getValue());
                success &= updateStudent(s);
            } else {
                success = false;
            }
        }
        return success;
    }

    // Methods cho GPA
    private Map<String, Integer> getModuleCredits() {
        Map<String, Integer> credits = new HashMap<>();
        for (Module m : modules.values()) {
            credits.put(m.getCode(), m.getCredits());
        }
        return credits;
    }

    @Override
    public synchronized double getGPAForStudent(String id) throws RemoteException {
        Student s = students.get(id);
        if (s != null) {
            return s.getGPA(getModuleCredits());
        }
        return 0.0;
    }

    @Override
    public synchronized Map<String, Double> getGPAForClass(String className) throws RemoteException {
        Map<String, Double> gpas = new HashMap<>();
        Map<String, Integer> credits = getModuleCredits();
        for (Student s : students.values()) {
            if (s.getClassName().equalsIgnoreCase(className)) {
                gpas.put(s.getId(), s.getGPA(credits));
            }
        }
        return gpas;
    }
}