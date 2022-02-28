package com.project.finances.domain.usecases.user.repository;

import com.project.finances.domain.entity.User;
import com.project.finances.domain.entity.UserCode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserCodeCommand {
    private final UserCodeRepository repository;

    public String save(User user){
        Optional<UserCode> userCodeOptional = repository.findByUserId(user.getId());

        userCodeOptional.ifPresent(this::invalidateCode);

        UserCode userCodeSaved = repository.save(UserCode.builder().user(user).isValid(true).build());

        return userCodeSaved.getId();
    }

    public UserCode invalidateCode(UserCode userCode){
        userCode.disableCode();
        return repository.save(userCode);
    }

}
