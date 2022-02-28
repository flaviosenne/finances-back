package com.project.finances.domain.usecases.user;

import com.project.finances.domain.entity.User;
import com.project.finances.domain.exception.BadRequestException;
import com.project.finances.domain.protocols.CryptographyProtocol;
import com.project.finances.domain.protocols.UserAccountProtocol;
import com.project.finances.domain.usecases.user.email.MailCreateAccountProtocol;
import com.project.finances.domain.usecases.user.email.MailRetrievePasswordProtocol;
import com.project.finances.domain.usecases.user.repository.UserCodeCommand;
import com.project.finances.domain.usecases.user.repository.UserCommand;
import com.project.finances.domain.usecases.user.repository.UserQuery;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static com.project.finances.domain.exception.messages.MessagesException.USER_NOT_FOUND;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class UserAccountTest {

    @Mock
    private UserQuery userQuery;
    @Mock
    private UserCommand userCommand;
    @Mock
    private CryptographyProtocol cryptographyProtocol;
    @Mock
    private MailCreateAccountProtocol mailCreateAccountProtocol;
    @Mock
    private MailRetrievePasswordProtocol mailRetrievePasswordProtocol;
    @Mock
    private UserCodeCommand userCodeCommand;

    private UserAccountProtocol userAccountProtocol;

    @BeforeEach
    void setup(){
        userAccountProtocol = new UserAccount(userQuery, userCommand, cryptographyProtocol,
                mailCreateAccountProtocol, mailRetrievePasswordProtocol, userCodeCommand);
    }

    @Test
    @DisplayName("Should throw bad request exception when email already exist in Data base")
    void notCreateAccount(){
        User userMock = new User("example@email.com", "first-name", "last-name", "hash", true);
        User userToSaveMock = new User("example@email.com", "first-name", "last-name", "password", false);

        when(userQuery.findByUsername(anyString())).thenReturn(Optional.of(userMock));

        Throwable exception = BDDAssertions.catchThrowable(()-> userAccountProtocol.createAccount(userToSaveMock));

        BDDAssertions.assertThat(exception).isInstanceOf(BadRequestException.class).hasMessage("Email já cadastrado na base de dados");

        verify(userCommand, never()).save(any(User.class));
        verify(userCodeCommand, never()).save(any(User.class));
        verify(mailCreateAccountProtocol, never()).sendEmail(any(User.class));
    }

    @Test
    @DisplayName("Should create new user when email do not exist in DB and request is successful")
    void createAccount(){
        User userMock = new User("example@email.com", "first-name", "last-name", "hash", false);
        User userToSaveMock = new User("example@email.com", "first-name", "last-name", "password", false);

        userMock.withId(userToSaveMock.getId());
        when(userQuery.findByUsername(anyString())).thenReturn(Optional.empty());
        when(cryptographyProtocol.encodePassword(anyString())).thenReturn("hash");
        when(userCommand.save(userToSaveMock)).thenReturn(userMock);

        User result = userAccountProtocol.createAccount(userToSaveMock);

        BDDAssertions.assertThat(result).isNotNull();
        BDDAssertions.assertThat(result.getPassword()).isEqualTo("hash");
        BDDAssertions.assertThat(result.getId()).isNotNull().isEqualTo(userToSaveMock.getId());

        verify(cryptographyProtocol, times(1)).encodePassword(userToSaveMock.getPassword());
        verify(userCommand, times(1)).save(userToSaveMock);
        verify(mailCreateAccountProtocol, times(1)).sendEmail(userMock);
    }


    @Test
    @DisplayName("Should throw bad request exception when code user do not exist in DB")
    void notActiveAccount(){
        when(userQuery.findById(anyString())).thenReturn(Optional.empty());

        Throwable exception = BDDAssertions.catchThrowable(() ->userAccountProtocol.activeAccount("invalid-code"));

        BDDAssertions.assertThat(exception).isInstanceOf(BadRequestException.class).hasMessage("Código do usuário inválido");

        verify(userCommand, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user active account when user exist in DB")
    void activeAccount(){
        User userMock = new User("example@email.com", "first-name", "last-name", "hash", false);
        User userActive = userMock.activeAccount();
        String id = "valid-code";
        userMock.withId(id);
        userActive.withId(id);

        when(userQuery.findById(anyString())).thenReturn(Optional.of(userMock));
        when(userCommand.update(any(User.class), anyString())).thenReturn(userActive);

        User result = userAccountProtocol.activeAccount(id);

        BDDAssertions.assertThat(result).isNotNull();
        BDDAssertions.assertThat(result.isActive()).isTrue();

        verify(userQuery, times(1)).findById(anyString());
        verify(userCommand, times(1)).update(any(User.class), eq(id));
    }

    @Test
    @DisplayName("Should send email retrieve password to user when exist in DB")
    void retrievePassword(){
        User userMock = new User("example@email.com", "first-name", "last-name", "hash", false);

        when(userQuery.findByUsername(anyString())).thenReturn(Optional.of(userMock));
        when(userCodeCommand.save(userMock)).thenReturn("code");

        userAccountProtocol.retrievePassword(userMock.getEmail());

        verify(userQuery, times(1)).findByUsername(userMock.getEmail());
        verify(userCodeCommand, times(1)).save(userMock);
        verify(mailRetrievePasswordProtocol, times(1)).sendEmail(userMock, "code");
    }

    @Test
    @DisplayName("Do not should send email retrieve password to user when do not exists in DB")
    void notRetrievePassword(){
        User userMock = new User("invalid@email.com", "first-name", "last-name", "hash", false);

        when(userQuery.findByUsername(anyString())).thenReturn(Optional.empty());

        userAccountProtocol.retrievePassword(userMock.getEmail());

        verify(userQuery, times(1)).findByUsername(userMock.getEmail());
        verify(userCodeCommand, never()).save(any(User.class));
        verify(mailRetrievePasswordProtocol, never()).sendEmail(any(User.class), anyString());
    }

    @Test
    @DisplayName("Should return user details when email exist in DB")
    void loadByUsername(){
        UserAccount service = new UserAccount(userQuery, userCommand, cryptographyProtocol,
                mailCreateAccountProtocol, mailRetrievePasswordProtocol, userCodeCommand);

        User userMock = new User("valid@email.com", "first-name", "last-name", "hash", false);
        when(userQuery.findByUsername(anyString())).thenReturn(Optional.of(userMock));

        UserDetails result = service.loadUserByUsername(userMock.getEmail());

        BDDAssertions.assertThat(result).isNotNull();
        BDDAssertions.assertThat(result.getUsername()).isEqualTo(userMock.getEmail());

        verify(userQuery, times(1)).findByUsername(userMock.getEmail());
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when email not exist in DB")
    void loadByUsernameException(){
        UserAccount service = new UserAccount(userQuery, userCommand, cryptographyProtocol,
                mailCreateAccountProtocol, mailRetrievePasswordProtocol, userCodeCommand);

        User userMock = new User("invalid@email.com", "first-name", "last-name", "hash", false);
        when(userQuery.findByUsername(anyString())).thenReturn(Optional.empty());

        Throwable exception = BDDAssertions.catchThrowable(() ->service.loadUserByUsername(userMock.getEmail()));

        BDDAssertions.assertThat(exception).isInstanceOf(UsernameNotFoundException.class).hasMessage(USER_NOT_FOUND);

        verify(userQuery, times(1)).findByUsername(userMock.getEmail());
    }

}