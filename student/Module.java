package rmi.student;

import java.io.Serializable;

public class Module implements Serializable {
    private static final long serialVersionUID = 1L;
    private String code;  // Mã học phần
    private String name;  // Tên học phần
    private int credits;  // Số tín chỉ

    public Module() {}

    public Module(String code, String name, int credits) {
        this.code = code;
        this.name = name;
        this.credits = credits;
    }

    // Getters & Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    @Override
    public String toString() {
        return code + " - " + name + " (" + credits + " tín chỉ)";
    }
}