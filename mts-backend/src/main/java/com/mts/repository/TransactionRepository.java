package com.mts.repository;

import com.mts.entity.Transaction;
import com.mts.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.accountNumber = :accountNumber " +
           "OR t.toAccount.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);

    List<Transaction> findByStatus(TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.user.username = :username " +
           "OR t.toAccount.user.username = :username ORDER BY t.createdAt DESC")
    List<Transaction> findByUsername(@Param("username") String username);
}
