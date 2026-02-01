package com.app.portfolio.repository;

import com.app.portfolio.beans.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
