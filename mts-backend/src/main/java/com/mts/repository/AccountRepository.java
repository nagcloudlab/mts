package com.mts.repository;

import com.mts.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(Long userId);

    @Query("SELECT a FROM Account a WHERE a.user.username = :username AND a.isActive = true")
    List<Account> findActiveAccountsByUsername(@Param("username") String username);

    boolean existsByAccountNumber(String accountNumber);
}
