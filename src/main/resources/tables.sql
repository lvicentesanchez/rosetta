create table translation_table(
  locale VARCHAR(16) NOT NULL,
  messageKey VARCHAR(4096) NOT NULL,
  message VARCHAR(4096) NOT NULL
);

insert into translation_table values('es_ES', 'greeting.morning', 'Buenos dias!');
insert into translation_table values('en_GB', 'greeting.morning', 'Good morning!');
insert into translation_table values('de_DE', 'greeting.morning', 'Gutten tag!');
insert into translation_table values('fr_FR', 'greeting.morning', 'Bon Jour!');
insert into translation_table values('es_ES', 'greeting.sleep', 'Buenas noches!');
insert into translation_table values('en_GB', 'greeting.sleep', 'Good night!');

create table translated_request(
  handle VARCHAR(512) NOT NULL,
  locale VARCHAR(16) NOT NULL,
  message VARCHAR(4096) NOT NULL,
  created_at TIMESTAMP
);
