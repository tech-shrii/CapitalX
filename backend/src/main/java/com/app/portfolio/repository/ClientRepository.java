package com.app.portfolio.repository;

import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<Client> findByUser(User user);
}
