INSERT INTO wallet_entity (
    id,
    user_id,
    balance
) VALUES (
             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
             '8cca7a29-5add-4197-ad56-48be327ea13c',
             100000.0
         );

INSERT INTO payment_entity (
    id,
    price,
    product_id,
    product_quantity,
    payed_at,
    status,
    payer_id,
    total_price
) VALUES (
             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d60',
             59.99,
             'd572df76-b527-4e31-8aa3-9aa954d17100',
             2,
             '2025-12-12T12:01:00Z',
             'SUCCESS',
             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
             119.98
         );

INSERT INTO payment_entity (
    id,
    price,
    product_id,
    product_quantity,
    payed_at,
    status,
    payer_id,
    total_price
) VALUES (
             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d61',
             129.99,
             'd572df76-b527-4e31-8aa3-9aa954d17101',
             3,
             '2025-10-13T14:04:00Z',
             'SUCCESS',
             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
             389.97
         );

INSERT INTO payment_entity (
    id,
    price,
    product_id,
    product_quantity,
    payed_at,
    status,
    payer_id,
    total_price
) VALUES (
             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d62',
             89.99,
             'd572df76-b527-4e31-8aa3-9aa954d17102',
             5,
             '2025-11-20T20:31:00Z',
             'SUCCESS',
             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
             449.95
         );

INSERT INTO payment_entity (
    id,
    price,
    product_id,
    product_quantity,
    payed_at,
    status,
    payer_id,
    total_price
) VALUES (
             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d63',
             599.99,
             'd572df76-b527-4e31-8aa3-9aa954d17103',
             4,
             '2025-12-01T13:14:00Z',
             'SUCCESS',
             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
             2399.96
         );

INSERT INTO payment_entity (
    id,
    price,
    product_id,
    product_quantity,
    payed_at,
    status,
    payer_id,
    total_price
) VALUES (
             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d64',
             2099.99,
             'd572df76-b527-4e31-8aa3-9aa954d17104',
             1,
             '2025-11-17T20:35:00Z',
             'SUCCESS',
             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
             2099.99
         );
