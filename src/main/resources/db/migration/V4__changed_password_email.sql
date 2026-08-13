ALTER TABLE email_templates
    DROP CONSTRAINT email_templates_type_check;

ALTER TABLE email_templates
    ADD CONSTRAINT email_templates_type_check
        CHECK (
            type IN (
                     'ACTIVATION_EMAIL',
                     'USER_RESERVATION_CREATED',
                     'CHANGED_PASSWORD'
                )
            );

INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Password Changed pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p> ${nick} Twoje hasło zostało pomyślnie zmienione. </p> </td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Password Changed en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p> ${nick} Your password has been changed successfully. </p> </td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Password Changed en', 'Password Changed en', 'Footer en', 'header', 'en', 'VipSound password changed', 'CHANGED_PASSWORD');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Password Changed pl', 'Password Changed pl', 'Footer pl', 'header', 'pl', 'VipSound zmieniono hasło', 'CHANGED_PASSWORD');