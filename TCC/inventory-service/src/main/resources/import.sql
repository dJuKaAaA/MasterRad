INSERT INTO product_entity (
    id,
    name,
    description,
    price,
    added_at,
    status,
    merchant_id
) VALUES (
             'd572df76-b527-4e31-8aa3-9aa954d17100',
             'Gaming Mouse',
             'High precision RGB gaming mouse',
             59.99,
             '2024-05-15T16:22:00Z',
             'ACTIVE',
             '76347922-6f4f-41df-8ff5-dae6bb66b69a'
         );
INSERT INTO inventory_entity (
    id,
    product_id,
    available_stock,
    created_at,
    last_updated_at,
    reserved_amount
) VALUES (
             'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c10',
             'd572df76-b527-4e31-8aa3-9aa954d17100',
             100,
             '2024-05-15T16:22:00Z',
             '2024-05-15T16:22:00Z',
             0
         );

INSERT INTO product_entity (
    id,
    name,
    description,
    price,
    added_at,
    status,
    merchant_id
) VALUES (
             'd572df76-b527-4e31-8aa3-9aa954d17101',
             'Gaming Keyboard',
             'High precision RGB gaming keyboard',
             129.99,
             '2024-05-16T07:18:00Z',
             'ACTIVE',
             '76347922-6f4f-41df-8ff5-dae6bb66b69a'
         );
INSERT INTO inventory_entity (
    id,
    product_id,
    available_stock,
    created_at,
    last_updated_at,
    reserved_amount
) VALUES (
             'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c11',
             'd572df76-b527-4e31-8aa3-9aa954d17101',
             150,
             '2024-05-16T07:18:00Z',
             '2024-05-16T07:18:00Z',
             0
         );

INSERT INTO product_entity (
    id,
    name,
    description,
    price,
    added_at,
    status,
    merchant_id
) VALUES (
             'd572df76-b527-4e31-8aa3-9aa954d17102',
             'XBOX Controller for PC',
             'XBOX Controller built for PC use',
             89.99,
             '2024-05-17T10:41:00Z',
             'ACTIVE',
             '76347922-6f4f-41df-8ff5-dae6bb66b69a'
         );
INSERT INTO inventory_entity (
    id,
    product_id,
    available_stock,
    created_at,
    last_updated_at,
    reserved_amount
) VALUES (
             'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c12',
             'd572df76-b527-4e31-8aa3-9aa954d17102',
             200,
             '2024-05-17T10:41:00Z',
             '2024-05-17T10:41:00Z',
             0
         );

INSERT INTO product_entity (
    id,
    name,
    description,
    price,
    added_at,
    status,
    merchant_id
) VALUES (
             'd572df76-b527-4e31-8aa3-9aa954d17103',
             'Gaming Monitor',
             'Gaming Monitor with 300hz refresh rate with 4k resolution',
             599.99,
             '2024-05-20T17:27:00Z',
             'ACTIVE',
             '76347922-6f4f-41df-8ff5-dae6bb66b69a'
         );
INSERT INTO inventory_entity (
    id,
    product_id,
    available_stock,
    created_at,
    last_updated_at,
    reserved_amount
) VALUES (
             'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13',
             'd572df76-b527-4e31-8aa3-9aa954d17103',
             20,
             '2024-05-20T17:27:00Z',
             '2024-05-20T17:27:00Z',
             0
         );

INSERT INTO product_entity (
    id,
    name,
    description,
    price,
    added_at,
    status,
    merchant_id
) VALUES (
             'd572df76-b527-4e31-8aa3-9aa954d17104',
             'Gaming PC',
             'PC with Ryzen 7 CPU, RTX 3060 6GB graphics card, 32GB ram and 1TB of SSD',
             2099.99,
             '2024-06-01T06:21:00Z',
             'ACTIVE',
             '76347922-6f4f-41df-8ff5-dae6bb66b69a'
         );
INSERT INTO inventory_entity (
    id,
    product_id,
    available_stock,
    created_at,
    last_updated_at,
    reserved_amount
) VALUES (
             'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c14',
             'd572df76-b527-4e31-8aa3-9aa954d17104',
             25,
             '2024-06-01T06:21:00Z',
             '2024-06-01T06:21:00Z',
             0
         );

-- INSERT INTO product_entity (
--     id,
--     name,
--     description,
--     price,
--     added_at,
--     status,
--     merchant_id
-- ) VALUES (
--              'd572df76-b527-4e31-8aa3-9aa954d17105',
--              'RAM 32gb',
--              '32gb of RAM',
--              999.99,
--              '2025-11-29T16:21:00Z',
--              'DISCONTINUED',
--              '76347922-6f4f-41df-8ff5-dae6bb66b69a'
--          );
-- INSERT INTO inventory_entity (
--     id,
--     product_id,
--     available_stock,
--     created_at,
--     last_updated_at,
--     reserved_amount,
--     locked,
--     lock_id
-- ) VALUES (
--              'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c15',
--              'd572df76-b527-4e31-8aa3-9aa954d17105',
--              25,
--              '2025-11-29T16:21:00Z',
--              '2025-11-29T16:21:00Z',
--              0,
--              false,
--              null
--          );
