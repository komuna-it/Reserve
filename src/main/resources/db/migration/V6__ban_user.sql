ALTER TABLE email_templates
    DROP CONSTRAINT email_templates_type_check;

ALTER TABLE email_templates
    ADD CONSTRAINT email_templates_type_check
        CHECK (
            type IN (
                'ACTIVATION_EMAIL',
                'USER_RESERVATION_CREATED',
                'CHANGED_PASSWORD',
                'REMIND_PASSWORD',
                'USER_BANNED'
            )
        );

INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Banned User pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Twoje konto zostało zablokowane.</p><p>Powód blokady:</p><p><strong>${reason}</strong></p><p>Blokada obowiązuje do:</p><p><strong>${expires}</strong></p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Banned User en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>Your account has been suspended.</p><p>Reason for suspension:</p><p><strong>${reason}</strong></p><p>Suspension expires on:</p><p><strong>${expires}</strong></p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Banned User pl', 'Banned User pl', 'Footer pl', 'header', 'pl', 'VipSound Twoje konto zostało zbanowane', 'USER_BANNED');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Banned User en', 'Banned User en', 'Footer en', 'header', 'en', 'VipSound your account has been banned', 'USER_BANNED');
