ALTER TABLE email_templates
    DROP CONSTRAINT email_templates_type_check;

ALTER TABLE email_templates ADD CONSTRAINT email_templates_type_check
    CHECK (
        type IN (
            'ACTIVATION_EMAIL',
            'USER_RESERVATION_CREATED',
            'CHANGED_PASSWORD',
            'REMIND_PASSWORD',
            'USER_BANNED',
            'RESERVATION_CREATED_PRIVATE',
            'RESERVATION_CREATED_ORGANIZATION',
            'RESERVATION_REMINDER'
        )
    );

-- Private reservation
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Created Private pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Utworzono prywatną rezerwacje w ${roomName} sali</p><p>Rozpoczęcie: <strong>${startAt}</strong></p><p>Zakończenie: <strong>${endAt}</strong></p><p>Życzymy udanego grania!</p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Created Private en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>A private reservation has been created in the ${roomName} room.</p><p>Start time: <strong>${startAt}</strong></p><p>End time: <strong>${endAt}</strong></p><p>We wish you a great jam session!</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Created Private pl', 'Reservation Created Private pl', 'Footer pl', 'header', 'pl', 'VipSound nowa rezerwacja', 'RESERVATION_CREATED_PRIVATE');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Created Private en', 'Reservation Created Private en', 'Footer en', 'header', 'en', 'VipSound new reservation', 'RESERVATION_CREATED_PRIVATE');

-- Organization reservation
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Created Organization pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Utworzono rezerwacje w ${roomName} sali dla zespołu ${organizationName}</p><p>Rozpoczęcie: <strong>${startAt}</strong></p><p>Zakończenie: <strong>${endAt}</strong></p><p>Życzymy udanego grania!</p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Created Organization en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>A reservation has been created in the ${roomName} room for the band ${organizationName}.</p><p>Start time: <strong>${startAt}</strong></p><p>End time: <strong>${endAt}</strong></p><p>We wish you a great jam session!</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Created Organization pl', 'Reservation Created Organization pl', 'Footer pl', 'header', 'pl', 'VipSound nowa rezerwacja', 'RESERVATION_CREATED_ORGANIZATION');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Created Organization en', 'Reservation Created Organization en', 'Footer en', 'header', 'en', 'VipSound new reservation', 'RESERVATION_CREATED_ORGANIZATION');

-- Reminder
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Reminder pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Przypominamy o jutrzejszej rezerwacji w ${roomName} sali</p><p>Rozpoczęcie: <strong>${startAt}</strong></p><p>Zakończenie: <strong>${endAt}</strong></p><p>Życzymy udanego grania!</p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Reminder en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>This is a reminder about your reservation tomorrow in the ${roomName} room.</p><p>Start time: <strong>${startAt}</strong></p><p>End time: <strong>${endAt}</strong></p><p>We wish you a great jam session!</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Reminder pl', 'Reservation Reminder pl', 'Footer pl', 'header', 'pl', 'VipSound przypomnienie', 'RESERVATION_REMINDER');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Reminder en', 'Reservation Reminder en', 'Footer en', 'header', 'en', 'VipSound reminder', 'RESERVATION_REMINDER');

