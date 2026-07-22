package com.portfolio.Library.Management.Repositories;

import com.portfolio.Library.Management.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>  {
}
