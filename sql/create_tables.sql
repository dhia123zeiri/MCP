CREATE TABLE suppliers(
   supplier_id SERIAL PRIMARY KEY,
   supplier_name VARCHAR(40) NOT NULL
);

INSERT INTO suppliers (supplier_name) VALUES ('SMTUC');
INSERT INTO suppliers (supplier_name) VALUES ('Metro Mondego');
INSERT INTO suppliers (supplier_name) VALUES ('Coimbra Taxis');
INSERT INTO suppliers (supplier_name) VALUES ('Bolt');
INSERT INTO suppliers (supplier_name) VALUES ('Scooters Assassinas');
