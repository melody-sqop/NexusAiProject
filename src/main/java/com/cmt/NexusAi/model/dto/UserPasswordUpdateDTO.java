package com.cmt.NexusAi.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 更改用户密码的DTO
public class UserPasswordUpdateDTO {
    private String name;
   private String phone;
   private String email;
   private String password;
   private String newpassword;


}

