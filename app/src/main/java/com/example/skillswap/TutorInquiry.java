package com.example.skillswap;

public class TutorInquiry {
    private String studentName;
    private String studentEmail;
    private String skillTitle;
    private String lastMessage;
    private String chatId;

    public TutorInquiry() {}

    public TutorInquiry(String studentName, String studentEmail, String skillTitle, String lastMessage, String chatId) {
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.skillTitle = skillTitle;
        this.lastMessage = lastMessage;
        this.chatId = chatId;
    }

    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getSkillTitle() { return skillTitle; }
    public String getLastMessage() { return lastMessage; }
    public String getChatId() { return chatId; }
}
