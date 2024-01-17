package com.example.securenoteapp.model.repository;

import com.example.securenoteapp.model.data.UserAgentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgentInfoRepository extends JpaRepository<UserAgentInfo, Long> {
}
