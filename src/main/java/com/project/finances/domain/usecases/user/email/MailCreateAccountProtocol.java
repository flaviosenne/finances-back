package com.project.finances.domain.usecases.user.email;

import com.project.finances.domain.entity.User;

public interface MailCreateAccountProtocol {

    void sendEmailActiveAccount(User user, String code);

}
