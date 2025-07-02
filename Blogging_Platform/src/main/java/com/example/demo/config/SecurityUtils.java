package com.example.demo.config;

import com.example.demo.customuserdetails.CustomUserDetails;
import com.example.demo.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// This class is to get the authenticated user's details
public class SecurityUtils {

    public static Long getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.getPrincipal() instanceof CustomUserDetails){
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            return  customUserDetails.getId();
        }
        throw  new IllegalStateException("User not authenticated");
    }

    public static  boolean isAdmin(User u){
        return u.getRoles().contains("ROLE_ADMIN");
    }

    public static boolean isUser(User u){
        return u.getRoles().contains("ROLE_USER");
    }

    public  static boolean isEditor(User u){
        return u.getRoles().contains("ROLE_EDITOR");
    }
}
