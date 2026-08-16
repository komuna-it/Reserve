-- SETTINGS
ALTER TABLE settings
    DROP CONSTRAINT settings_key_check;

ALTER TABLE settings ADD CONSTRAINT settings_key_check
    CHECK(
        key IN (
            'RESERVATION_CANCELLATION_WITHOUT_APPROVAL_HOURS',
            'RESERVATION_OPENING_HOUR',
            'RESERVATION_CLOSING_HOUR',
            'RESERVATION_REMINDER_HOUR',
            'MAIL_SERVER_HOST',
            'MAIL_SERVER_PORT',
            'MAIL_SERVER_USERNAME',
            'MAIL_SERVER_PASSWORD',
            'MAIL_SMTP_AUTH',
            'MAIL_SMTP_STARTTLS_ENABLE',
            'MAIL_SERVER_BETA_ADDRESS',
            'CONTACT_PHONE',
            'CONTACT_EMAIL'
        )
    );

-- EMAILS
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
            'RESERVATION_REMINDER',
            'RESERVATION_CANCEL_REQUESTED',
            'RESERVATION_CANCEL_REJECTED',
            'RESERVATION_CANCELED_PRIVATE',
            'RESERVATION_CANCELED_ORGANIZATION',
            'RESERVATION_REJECTED',
            'RESERVATION_CONFIRMED'
        )
    );

-- RESERVATION CANCEL REQUEST
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Cancel Requested pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Użytkownik <strong>${requestedBy}</strong> utworzył prośbę o anulowanie rezerwacji.</p><p>Sala: <strong>${roomName}</strong><br>Rozpoczęcie: <strong>${startAt}</strong><br>Zakończenie: <strong>${endAt}</strong><br></p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Cancel Requested en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>User <strong>${requestedBy}</strong> has submitted a reservation cancellation request.</p><p>Room: <strong>${roomName}</strong><br>Start time: <strong>${startAt}</strong><br>End time: <strong>${endAt}</strong><br></p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Cancel Requested pl', 'Reservation Cancel Requested pl', 'Footer pl', 'header', 'pl', 'VipSound prośba o anulowanie rezerwcji', 'RESERVATION_CANCEL_REQUESTED');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Cancel Requested en', 'Reservation Cancel Requested en', 'Footer en', 'header', 'en', 'VipSound cancel reservation request', 'RESERVATION_CANCEL_REQUESTED');

-- PRIVATE RESERVATION CANCELLED
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Canceled Private pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Twoja prywatna rezerwacja w ${roomName} sali od <strong>${startAt}</strong> do <strong>${endAt}</strong> została anulowana </p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Canceled Private en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>Your private reservation in room <strong>${roomName}</strong> from <strong>${startAt}</strong> to <strong>${endAt}</strong> has been cancelled.</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Canceled Private pl', 'Reservation Canceled Private pl', 'Footer pl', 'header', 'pl', 'VipSound anulowano rezwacje', 'RESERVATION_CANCELED_PRIVATE');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Canceled Private en', 'Reservation Canceled Private en', 'Footer en', 'header', 'en', 'VipSound reservation cancelled', 'RESERVATION_CANCELED_PRIVATE');

-- ORGANIZATION RESERVATION CANCELLED
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Canceled Organization pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Rezerwacja dla Twojego zespołu ${organizationName} w ${roomName} sali od <strong>${startAt}</strong> do <strong>${endAt}</strong> została anulowana </p></td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Canceled Organization en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>The reservation for your band <strong>${organizationName}</strong> in room<strong>${roomName}</strong> from <strong>${startAt}</strong> to<strong>${endAt}</strong> has been cancelled.</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Canceled Organization pl', 'Reservation Canceled Organization pl', 'Footer pl', 'header', 'pl', 'VipSound anulowano rezwacje', 'RESERVATION_CANCELED_ORGANIZATION');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Canceled Organization en', 'Reservation Canceled Organization en', 'Footer en', 'header', 'en', 'VipSound reservation cancelled', 'RESERVATION_CANCELED_ORGANIZATION');

-- RESERVATION REJECTED
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Rejected pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Niestety, rezerwacja sali <strong>${roomName}</strong> w terminie od <strong>${startAt}</strong> do <strong>${endAt}</strong> została odrzucona.</p><p>W razie pytań skontaktuj się z administracją.</p> </td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Rejected en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>Unfortunately, your reservation for <strong>${roomName}</strong> from <strong>${startAt}</strong> to <strong>${endAt}</strong> has been declined.</p><p>If you have any questions, please contact the administration.</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Rejected pl', 'Reservation Rejected pl', 'Footer pl', 'header', 'pl', 'VipSound rezerwacja odrzucona', 'RESERVATION_REJECTED');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Rejected en', 'Reservation Rejected en', 'Footer en', 'header', 'en', 'VipSound reservation rejected', 'RESERVATION_REJECTED');

-- RESERVATION REJECTED
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Confirmed pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Rezerwacja sali <strong>${roomName}</strong> została zaakceptowana zapraszamy od <strong>${startAt}</strong> do <strong>${endAt}</strong> <p>Życzymy udanego grania!</p> </td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Confirmed en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>Your reservation for <strong>${roomName}</strong> has been approved. We look forward to welcoming you from <strong>${startAt}</strong> to <strong>${endAt}</strong>.</p><p>We wish you a great jam session!</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Confirmed pl', 'Reservation Confirmed pl', 'Footer pl', 'header', 'pl', 'VipSound rezerwacja potwierdzona', 'RESERVATION_CONFIRMED');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Confirmed en', 'Reservation Confirmed en', 'Footer en', 'header', 'en', 'VipSound reservation approved', 'RESERVATION_CONFIRMED');

-- RESERVATION CANCEL REJECTED
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Cancel Rejected pl', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Witaj ${nick},</p><p>Niestety, anulowanie rezerwacji sali <strong>${roomName}</strong> w terminie od <strong>${startAt}</strong> do <strong>${endAt}</strong> zostało odrzucone.</p><p>W razie pytań skontaktuj się z administracją.</p> </td> </tr>', 'BODY');
INSERT INTO public.email_fragments (name, fragment, type) VALUES ('Reservation Cancel Rejected en', e'<tr> <td style="background-color: #ffffff; padding: 40px; font-family: Arial, Helvetica, sans-serif; color: #333333;"> <p>Hello ${nick},</p><p>Unfortunately, your cancle reservation for <strong>${roomName}</strong> from <strong>${startAt}</strong> to <strong>${endAt}</strong> has been declined.</p><p>If you have any questions, please contact the administration.</p></td> </tr>', 'BODY');

INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Cancel Rejected pl', 'Reservation Cancel Rejected pl', 'Footer pl', 'header', 'pl', 'VipSound anulowanie rezerwacji odrzucone', 'RESERVATION_CANCEL_REJECTED');
INSERT INTO public.email_templates (name, body_id, footer_id, header_id, language, subject, type) VALUES ('Reservation Cancel Rejected en', 'Reservation Cancel Rejected en', 'Footer en', 'header', 'en', 'VipSound cancel reservation rejected', 'RESERVATION_CANCEL_REJECTED');