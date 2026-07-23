package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}

//package com.webapp.cognitodemo.repo;
//
//import com.webapp.cognitodemo.entity.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface UserRepo extends JpaRepository<User, Long> {
//}
