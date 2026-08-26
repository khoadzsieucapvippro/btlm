package com.elearning.repository;

import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSrsSettingRepository extends JpaRepository<UserSrsSetting, Long> {

    Optional<UserSrsSetting> findByUser(UserProfile user);

    Optional<UserSrsSetting> findByUser_UserId(Long userId);

    boolean existsByUser(UserProfile user);
}
