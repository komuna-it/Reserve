ALTER TABLE email_templates
    DROP CONSTRAINT email_templates_type_check;

ALTER TABLE email_templates
    ADD CONSTRAINT email_templates_type_check
        CHECK (
            type IN (
                'ACTIVATION_EMAIL',
                'USER_RESERVATION_CREATED',
                'CHANGED_PASSWORD',
                'REMIND_PASSWORD'
            )
        );

INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Password Remind pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Twoje hasło zostało pomyślnie zresetowane.</p><p>Możesz teraz zalogować się, używając poniższego tymczasowego hasła:</p><p><strong>${newPassword}</strong></p><p>Ze względów bezpieczeństwa zalecamy zmianę hasła po zalogowaniu.</p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Password Remind en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>Your password has been reset.</p><p>Your new password is:</p><p><strong>${newPassword}</strong></p><p>Please sign in and change this password as soon as possible.</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Remind Password en', 'Password Remind en', 'Footer en', 'header', 'en', 'VipSound your new password', 'REMIND_PASSWORD');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Remind Password pl', 'Password Remind pl', 'Footer pl', 'header', 'pl', 'VipSound Twoje nowe hasło', 'REMIND_PASSWORD');