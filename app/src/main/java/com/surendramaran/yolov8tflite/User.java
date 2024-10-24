package com.surendramaran.yolov8tflite;
import com.google.firebase.database.IgnoreExtraProperties;

import java.sql.Blob;
import java.sql.Timestamp;

@IgnoreExtraProperties
public class User {
    private int userId;
    private String userUname;
    private String userLname;
    private String userFname;
    private String userMname;
    private String userAddress;
    private String userPass;
    private String userBranch;
    private String userDesignation;
    private String userRole;
    private Blob userPic;
    private Timestamp userCreationDate;
    private Integer userCreator;

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserUname() { return userUname; }
    public void setUserUname(String userUname) { this.userUname = userUname; }

    public String getUserLname() { return userLname; }
    public void setUserLname(String userLname) { this.userLname = userLname; }

    public String getUserFname() { return userFname; }
    public void setUserFname(String userFname) { this.userFname = userFname; }

    public String getUserMname() { return userMname; }
    public void setUserMname(String userMname) { this.userMname = userMname; }

    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }

    public String getUserPass() { return userPass; }
    public void setUserPass(String userPass) { this.userPass = userPass; }

    public String getUserBranch() { return userBranch; }
    public void setUserBranch(String userBranch) { this.userBranch = userBranch; }

    public String getUserDesignation() { return userDesignation; }
    public void setUserDesignation(String userDesignation) { this.userDesignation = userDesignation; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public Blob getUserPic() { return userPic; }
    public void setUserPic(Blob userPic) { this.userPic = userPic; }

    public Timestamp getUserCreationDate() { return userCreationDate; }
    public void setUserCreationDate(Timestamp userCreationDate) { this.userCreationDate = userCreationDate; }

    public Integer getUserCreator() { return userCreator; }
    public void setUserCreator(Integer userCreator) { this.userCreator = userCreator; }
}