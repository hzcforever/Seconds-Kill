package com.hzc.secKill.Service;

import com.hzc.secKill.DAO.UserDAO;
import com.hzc.secKill.Domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserDAO userDAO;

    public User getById(int id) {
        return userDAO.getById(id);
    }
}
