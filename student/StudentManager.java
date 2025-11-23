package rmi.student;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface StudentManager extends Remote {
    boolean addStudent(Student s) throws RemoteException;
    boolean updateStudent(Student s) throws RemoteException;
    boolean deleteStudent(String id) throws RemoteException;
    Student getStudentById(String id) throws RemoteException;
    List<Student> getAllStudents() throws RemoteException;
    List<Student> getStudentsByClass(String className) throws RemoteException;
    
    // Methods cho học phần
    boolean addModule(Module m) throws RemoteException;
    boolean updateModule(Module m) throws RemoteException;
    boolean deleteModule(String code) throws RemoteException;
    Module getModuleByCode(String code) throws RemoteException;
    List<Module> getAllModules() throws RemoteException;
    List<Student> getStudentsWithScoresForModule(String moduleCode) throws RemoteException;
    List<Student> getStudentsWithScoresForModuleByClass(String moduleCode, String className) throws RemoteException; // Mới: Theo lớp
    boolean updateScoresForModule(String moduleName, Map<String, Student.SubjectScores> updates) throws RemoteException;
    
    // Methods cho GPA
    double getGPAForStudent(String id) throws RemoteException;
    Map<String, Double> getGPAForClass(String className) throws RemoteException; // Key: studentId, Value: GPA
}